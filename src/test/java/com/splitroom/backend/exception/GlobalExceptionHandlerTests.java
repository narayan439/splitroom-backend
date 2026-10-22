package com.splitroom.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTests {

    @Test
    void loginFailuresUseOneGenericMessageRegardlessOfAccountExistence() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/auth/login"
        );

        var response = handler.handleAuthenticationFailure(
                new BadCredentialsException(
                        "No account exists for private@example.com"
                ),
                request
        );

        assertEquals(401, response.getStatusCode().value());
        assertEquals("Invalid email or password", response.getBody().message());
    }

    @Test
    void databaseFailuresAreLoggedButDoNotExposeDatabaseDetails() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/rooms"
        );
        var exception = new DataAccessResourceFailureException(
                "Database connection failed for private-host",
                new IllegalStateException("private database diagnostic")
        );

        var response = handler.handleDatabaseFailure(exception, request);

        assertEquals(500, response.getStatusCode().value());
        assertEquals(
                "A database operation could not be completed",
                response.getBody().message()
        );
        assertFalse(response.getBody().message().contains("private"));
    }
}
