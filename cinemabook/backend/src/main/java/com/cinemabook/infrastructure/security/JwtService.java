package com.cinemabook.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Single Responsibility: JWT creation and parsing only.
 * Session management (Redis) lives in SessionService.
 */
@Service
@Slf4j
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtService(
            @Value("${cinemabook.jwt.secret}") String secret,
            @Value("${cinemabook.jwt.access-token-expiry-ms}") long accessTokenExpiryMs,
            @Value("${cinemabook.jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs) {

        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    // ── Generate access token ────────────────────────────────────────────────

    public String generateAccessToken(UUID userId, String email,
                                      String userType, String deviceId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("userType", userType)
                .claim("deviceId", deviceId)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusMillis(accessTokenExpiryMs)))
                .signWith(signingKey)
                .compact();
    }

    // ── Generate refresh token (opaque random UUID, stored hashed) ───────────

    public String generateRefreshToken() {
        return UUID.randomUUID() + "-" + UUID.randomUUID();
    }

    // ── Expiry getters ───────────────────────────────────────────────────────

    public long getAccessTokenExpiryMs() {
        return accessTokenExpiryMs;
    }

    public long getRefreshTokenExpiryMs() {
        return refreshTokenExpiryMs;
    }

    // ── Parse and validate ───────────────────────────────────────────────────

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessTokenValid(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseAccessToken(token).getSubject());
    }

    public String extractDeviceId(String token) {
        return parseAccessToken(token).get("deviceId", String.class);
    }

    public String extractUserType(String token) {
        return parseAccessToken(token).get("userType", String.class);
    }
}
