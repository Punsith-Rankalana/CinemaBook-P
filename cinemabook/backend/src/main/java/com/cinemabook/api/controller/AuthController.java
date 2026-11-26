package com.cinemabook.api.controller;

import com.cinemabook.api.dto.AuthDtos.*;
import com.cinemabook.application.service.AuthService;
import com.cinemabook.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Single Responsibility: HTTP mapping only.
 * All logic lives in AuthService.
 *
 * Base path: /api/v1/auth  (configured in application.properties)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /auth/register/watcher
     * Creates a new watcher account.
     */
    @PostMapping("/register/watcher")
    public ResponseEntity<ApiResponse<RegisterResponse>> registerWatcher(
            @Valid @RequestBody RegisterWatcherRequest req) {

        RegisterResponse result = authService.registerWatcher(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    /**
     * POST /auth/register/film-owner
     * Creates a new film owner account.
     */
    @PostMapping("/register/film-owner")
    public ResponseEntity<ApiResponse<RegisterResponse>> registerFilmOwner(
            @Valid @RequestBody RegisterFilmOwnerRequest req) {

        RegisterResponse result = authService.registerFilmOwner(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    /**
     * POST /auth/login
     * Authenticates a user and returns JWT tokens.
     * Enforces single-device: logging in here kicks out any other device.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest req) {

        AuthResponse result = authService.login(req);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * POST /auth/refresh
     * Exchanges a valid refresh token for a new access token.
     * Refresh tokens rotate on each use (old one is invalidated).
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest req,
            @RequestHeader("X-Device-Id") String deviceId) {

        AuthResponse result = authService.refresh(req, deviceId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * POST /auth/logout
     * Revokes all tokens and clears the device session.
     * Requires authentication.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UUID userId) {

        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /**
     * GET /auth/verify-email?token=...
     * Verifies the user's email address (Phase 7 — placeholder here).
     */
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(
            @RequestParam String token) {
        // TODO (Phase 7): implement email verification
        return ResponseEntity.ok(ApiResponse.ok("Email verification coming soon"));
    }
}
