package com.cinemabook.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Enforces the "one device at a time" rule using Redis.
 *
 * Pattern:
 *   Key:   cinemabook:session:{userId}
 *   Value: {deviceId}   (the currently authorised device)
 *
 * On every login:
 *   1. Write the new deviceId → overwrites the old one.
 *   2. Any request that arrives with a token containing the OLD deviceId
 *      will be rejected by JwtAuthFilter (device mismatch).
 *
 * On logout:
 *   Delete the key → no device is authorised.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private static final String SESSION_KEY_PREFIX = "cinemabook:session:";

    private final StringRedisTemplate redis;

    /**
     * Registers a device as the ONLY active device for this user.
     * Any previously registered device is implicitly invalidated.
     */
    public void registerDevice(UUID userId, String deviceId, Duration ttl) {
        String key = sessionKey(userId);
        redis.opsForValue().set(key, deviceId, ttl);
        log.debug("Session registered: userId={} deviceId={}", userId, deviceId);
    }

    /**
     * Returns true when the given deviceId is the currently authorised
     * device for this user.
     */
    public boolean isDeviceAuthorised(UUID userId, String deviceId) {
        String stored = redis.opsForValue().get(sessionKey(userId));
        return deviceId != null && deviceId.equals(stored);
    }

    /**
     * Called on logout — removes the session so no device is active.
     */
    public void revokeSession(UUID userId) {
        redis.delete(sessionKey(userId));
        log.debug("Session revoked for userId={}", userId);
    }

    /**
     * Returns the currently active deviceId for a user, or null if
     * no session exists.
     */
    public String getActiveDeviceId(UUID userId) {
        return redis.opsForValue().get(sessionKey(userId));
    }

    private String sessionKey(UUID userId) {
        return SESSION_KEY_PREFIX + userId.toString();
    }
}
