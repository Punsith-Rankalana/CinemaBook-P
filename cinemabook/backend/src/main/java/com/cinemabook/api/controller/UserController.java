package com.cinemabook.api.controller;

import com.cinemabook.api.dto.AuthDtos.UserDto;
import com.cinemabook.domain.user.AccountStatus;
import com.cinemabook.infrastructure.persistence.UserEntity;
import com.cinemabook.infrastructure.persistence.UserRepository;
import com.cinemabook.shared.exception.ResourceNotFoundException;
import com.cinemabook.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Handles user profile reads.
 * Write operations (update profile, change password) go here in Phase 6.
 *
 * Open/Closed: new profile endpoints are added here without
 * touching AuthController or AuthService.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    /**
     * GET /users/me
     * Returns the currently authenticated user's profile.
     * Used by the frontend on app startup to rehydrate state.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getMe(
            @AuthenticationPrincipal UUID userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return ResponseEntity.ok(ApiResponse.ok(toDto(user)));
    }

    /**
     * GET /users/{username}
     * Returns a public profile view of any user.
     * (Avatar, display name, follower count — no private data.)
     */
    @GetMapping("/{username}")
    public ResponseEntity<ApiResponse<PublicProfileDto>> getPublicProfile(
            @PathVariable String username) {

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        // Only show ACTIVE accounts publicly
        if (!AccountStatus.ACTIVE.equals(user.getAccountStatus())) {
            throw new ResourceNotFoundException("User", username);
        }

        return ResponseEntity.ok(ApiResponse.ok(toPublicDto(user)));
    }

    // ── DTOs ─────────────────────────────────────────────────

    public record PublicProfileDto(
            String id,
            String username,
            String displayName,
            String avatarUrl,
            String bio,
            String userType
    ) {}

    private UserDto toDto(UserEntity u) {
        return new UserDto(
                u.getId().toString(),
                u.getEmail(),
                u.getUsername(),
                u.getDisplayName(),
                u.getUserType(),
                u.getAccountStatus(),
                u.isEmailVerified(),
                u.getAvatarUrl()
        );
    }

    private PublicProfileDto toPublicDto(UserEntity u) {
        return new PublicProfileDto(
                u.getId().toString(),
                u.getUsername(),
                u.getDisplayName(),
                u.getAvatarUrl(),
                u.getBio(),
                u.getUserType().name()
        );
    }
}
