package com.raviraju.resource_booking_api.exception;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.raviraju.resource_booking_api.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void handleGeneral_ReturnsInternalServerErrorWithSanitizedMessage() {
        Exception rawException = new RuntimeException("Sensitive database connection details or stack error");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGeneral(rawException, request);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("An unexpected error occurred. Please try again later.", response.getBody().getMessage());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    void handleNotFound_ReturnsNotFoundStatus() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Resource with ID 42 not found");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Resource with ID 42 not found", response.getBody().getMessage());
    }

    @Test
    void handleConflict_ReturnsConflictStatus() {
        ResourceConflictException ex = new ResourceConflictException("Double booking conflict");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConflict(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Double booking conflict", response.getBody().getMessage());
    }

    @Test
    void handleBadRequest_ReturnsBadRequestStatus() {
        BadRequestException ex = new BadRequestException("Invalid input params");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadRequest(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid input params", response.getBody().getMessage());
    }

    @Test
    void handleBadCredentials_ReturnsUnauthorized() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadCredentials(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid username or password", response.getBody().getMessage());
    }

    @Test
    void handleAccessDenied_ReturnsForbidden() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody().getMessage().contains("Access is denied"));
    }

    @Test
    void handleValidation_ReturnsBadRequestWithFieldErrors() throws Exception {
        Method method = this.getClass().getDeclaredMethod("setUp");
        MethodParameter parameter = new MethodParameter(method, -1);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "fieldName", "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation Failed", response.getBody().getError());
        assertNotNull(response.getBody().getValidationErrors());
        assertEquals("must not be blank", response.getBody().getValidationErrors().get("fieldName"));
    }
}
