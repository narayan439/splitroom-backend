package com.splitroom.backend.exception;

import com.splitroom.backend.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            ConflictException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(
            ForbiddenException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationFailure(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequestBody(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": "
                        + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            HandlerMethodValidationException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "The request contains invalid or missing values",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.CONFLICT,
                "The request conflicts with existing data",
                request
        );
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDatabaseFailure(
            DataAccessException exception,
            HttpServletRequest request
    ) {
        logger.error(
                "Database operation failed for {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "A database operation could not be completed",
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
