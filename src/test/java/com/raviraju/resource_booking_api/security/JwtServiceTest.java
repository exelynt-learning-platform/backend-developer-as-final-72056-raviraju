package com.raviraju.resource_booking_api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", "SuperSecretKeyForJwtTestingPurposesOnlyAtLeast32CharsLong12345");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);
        jwtService.init();
    }

    @Test
    void generateAndValidateToken_Success() {
        String username = "testuser";
        String token = jwtService.generateToken(username);

        assertNotNull(token);
        assertEquals(username, jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, username));
    }

    @Test
    void validateToken_WrongUsername_ReturnsFalse() {
        String token = jwtService.generateToken("testuser");
        org.junit.jupiter.api.Assertions.assertFalse(jwtService.isTokenValid(token, "otheruser"));
    }

    @Test
    void validateToken_NullOrMalformedToken_ReturnsFalse() {
        org.junit.jupiter.api.Assertions.assertFalse(jwtService.isTokenValid("invalid.jwt.token", "testuser"));
        org.junit.jupiter.api.Assertions.assertFalse(jwtService.isTokenValid(null, "testuser"));
    }
}
