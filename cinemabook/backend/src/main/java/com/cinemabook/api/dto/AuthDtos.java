package com.cinemabook.api.dto;

import com.cinemabook.domain.user.AccountStatus;
import com.cinemabook.domain.user.UserType;
import jakarta.validation.constraints.*;

/**
 * Auth-related DTOs (Data Transfer Objects).
 *
 * Using Java records for immutability.
 * These are the shapes of JSON in requests and responses.
 */
public final class AuthDtos {

    private AuthDtos() {}

    // ── Requests ─────────────────────────────────────────────

    public record RegisterWatcherRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Must be a valid email address")
            String email,

            @NotBlank(message = "Username is required")
            @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
            @Pattern(regexp = "^[a-zA-Z0-9_]+$",
                    message = "Username may only contain letters, numbers, and underscores")
            String username,

            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password,

            String displayName
    ) {}

    public record RegisterFilmOwnerRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Must be a valid email address")
            String email,

            @NotBlank(message = "Username is required")
            @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
            @Pattern(regexp = "^[a-zA-Z0-9_]+$",
                    message = "Username may only contain letters, numbers, and underscores")
            String username,

            @NotBlank(message = "Password is required")
            @Size(min = 8, message = "Password must be at least 8 characters")
            String password,

            String companyName,
            String displayName
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Login is required")
            String login,           // accepts email OR username

            @NotBlank(message = "Password is required")
            String password,

            /** A stable identifier for this device/browser (e.g. browser fingerprint UUID). */
            @NotBlank(message = "Device ID is required")
            String deviceId,

            String deviceInfo       // optional user-agent description
    ) {}

    public record RefreshTokenRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}

    // ── Responses ────────────────────────────────────────────

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresInMs,
            UserDto user
    ) {}

    public record UserDto(
            String id,
            String email,
            String username,
            String displayName,
            UserType userType,
            AccountStatus accountStatus,
            boolean emailVerified,
            String avatarUrl
    ) {}

    public record RegisterResponse(
            String message,
            String userId
    ) {}
}
