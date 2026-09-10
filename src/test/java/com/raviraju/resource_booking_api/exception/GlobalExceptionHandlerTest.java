package com.raviraju.resource_booking_api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
}
