package com.raviraju.resource_booking_api.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        if (jwtSecret != null && !jwtSecret.trim().isEmpty()) {
            if (jwtSecret.trim().length() < 32) {
                throw new IllegalStateException("JWT_SECRET must be at least 32 characters (256 bits) long for secure HS256 signing.");
            }
            byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        } else {
            java.util.List<String> activeProfiles = activeProfile != null
                    ? java.util.Arrays.asList(activeProfile.split(","))
                    : java.util.Collections.emptyList();
            boolean isDevOrTest = activeProfiles.stream().anyMatch(p -> {
                String trimmed = p.trim();
                return trimmed.equalsIgnoreCase("dev") || trimmed.equalsIgnoreCase("test");
            });

            // In non-dev/non-test profiles, fail fast if JWT_SECRET is not provided
            if (!isDevOrTest) {
                throw new IllegalStateException("JWT_SECRET environment variable is strictly required in production profiles.");
            }
            // In development mode without an explicit JWT_SECRET, an ephemeral key is generated for convenience.
            // Note that all issued tokens will be invalidated upon application server restart.
            this.signingKey = Jwts.SIG.HS256.key().build();
            log.warn("JWT_SECRET not configured; generated ephemeral development signing key (tokens invalidate on restart).");
        }
    }

    private SecretKey getSigningKey() {
        return this.signingKey;
    }

    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public boolean isTokenValid(String token, String username) {
        try {
            final Claims claims = extractAllClaims(token);
            final String extractedUsername = claims.getSubject();
            final boolean isExpired = claims.getExpiration().before(new Date(System.currentTimeMillis() - 60_000));
            return (username != null && username.equals(extractedUsername) && !isExpired);
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .clockSkewSeconds(60)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
