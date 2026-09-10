package com.raviraju.resource_booking_api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(mock(Environment.class));
        ReflectionTestUtils.setField(jwtService, "jwtSecret",
                "SuperSecretKeyForJwtTestingPurposesOnlyAtLeast32CharsLong12345");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);
        jwtService.init();
    }

    @Test
    void generateAndValidateToken_Success() {
        String token = jwtService.generateToken("testuser");

        assertNotNull(token);
        assertEquals("testuser", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, "testuser"));
    }

    @Test
    void validateToken_WrongUsername_ReturnsFalse() {
        String token = jwtService.generateToken("testuser");
        assertFalse(jwtService.isTokenValid(token, "otheruser"));
    }

    @Test
    void validateToken_NullOrMalformedToken_ReturnsFalse() {
        assertFalse(jwtService.isTokenValid("invalid.jwt.token", "testuser"));
        assertFalse(jwtService.isTokenValid(null, "testuser"));
    }

    @Test
    void init_missingSecretOutsideDevTest_throws() {
        Environment env = mock(Environment.class);
        when(env.acceptsProfiles(any(Profiles.class))).thenReturn(false);

        JwtService service = new JwtService(env);
        ReflectionTestUtils.setField(service, "jwtSecret", "");

        assertThrows(IllegalStateException.class, service::init);
    }
}
