package com.splitroom.backend.security;

import com.splitroom.backend.entity.User;
import com.splitroom.backend.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null
                || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            if (isProtectedApiRequest(request)) {
                logger.warn(
                        "Missing bearer token for {} {}",
                        request.getMethod(),
                        request.getRequestURI()
                );
            }
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new org.springframework.security.authentication.BadCredentialsException(
                            "Invalid bearer token"
                    )
            );
            return;
        }

        try {
            Claims claims = jwtTokenService.parseToken(token);
            String email = claims.getSubject();
            Number userIdClaim = claims.get("uid", Number.class);
            if (email == null || userIdClaim == null) {
                throw new JwtException("Required token claims are missing");
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new JwtException(
                            "Token user no longer exists"
                    ));
            if (user.getUserId().longValue() != userIdClaim.longValue()) {
                throw new JwtException("Token identity does not match");
            }

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(email);
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );
            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException
                 | AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            logger.warn(
                    "Rejected bearer token for {} {} ({})",
                    request.getMethod(),
                    request.getRequestURI(),
                    exception.getClass().getSimpleName()
            );
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new org.springframework.security.authentication.BadCredentialsException(
                            "Invalid bearer token"
                    )
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isProtectedApiRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/")
                && !path.equals("/api/auth/login")
                && !path.equals("/api/auth/register");
    }
}
