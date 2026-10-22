package com.splitroom.backend.config;

import com.splitroom.backend.dto.response.RoomDetailsResponse;
import com.splitroom.backend.entity.User;
import com.splitroom.backend.repository.UserRepository;
import com.splitroom.backend.security.JwtAuthenticationEntryPoint;
import com.splitroom.backend.security.JwtAuthenticationFilter;
import com.splitroom.backend.security.JwtTokenService;
import com.splitroom.backend.security.SplitRoomUserDetailsService;
import com.splitroom.backend.service.RoomService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = com.splitroom.backend.controller.RoomController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtTokenService.class,
        SplitRoomUserDetailsService.class,
        PasswordConfig.class,
        CorsConfig.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "app.jwt.expiration-seconds=3600",
        "app.cors.allowed-origins=https://splitroom-frontend.onrender.com"
})
class RoomAuthenticationTests {

    private static final String EMAIL = "creator@example.com";
    private static final long USER_ID = 42L;
    private static final String SECRET =
            "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private FilterRegistrationBean<JwtAuthenticationFilter>
            jwtAuthenticationFilterRegistration;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(USER_ID);
        user.setEmail(EMAIL);
        user.setName("Room Creator");
        user.setPassword("not-used");
        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));
        when(roomService.createRoom("Maple House"))
                .thenReturn(new RoomDetailsResponse(
                        7L,
                        "Maple House",
                        new RoomDetailsResponse.CreatorDetails(
                                USER_ID,
                                "Room Creator",
                                EMAIL
                        ),
                        LocalDateTime.now(),
                        1
                ));
    }

    @Test
    void acceptsValidBearerTokenForRoomCreation() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + jwtTokenService.createToken(user)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomName\":\"Maple House\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsRoomCreationWithoutBearerToken() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomName\":\"Maple House\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidAndExpiredBearerTokens() throws Exception {
        assertUnauthorized("not-a-jwt");
        assertUnauthorized(expiredToken());
    }

    @Test
    void registersJwtFilterOnlyInSpringSecurityChain() {
        assertFalse(jwtAuthenticationFilterRegistration.isEnabled());
    }

    @Test
    void allowsErrorDispatchButKeepsDirectErrorRequestsProtected()
            throws Exception {
        mockMvc.perform(get("/error"))
                .andExpect(status().isUnauthorized());

        int errorDispatchStatus = mockMvc.perform(
                        get("/error").with(request -> {
                            request.setDispatcherType(DispatcherType.ERROR);
                            return request;
                        }))
                .andReturn()
                .getResponse()
                .getStatus();

        assertNotEquals(401, errorDispatchStatus);
    }

    private void assertUnauthorized(String token) throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomName\":\"Maple House\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String expiredToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(EMAIL)
                .claim("uid", USER_ID)
                .issuedAt(Date.from(now.minusSeconds(7200)))
                .expiration(Date.from(now.minusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(
                        Base64.getDecoder().decode(SECRET)
                ))
                .compact();
    }
}
