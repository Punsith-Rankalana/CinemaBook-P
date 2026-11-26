package com.cinemabook.api.controller;

import com.cinemabook.api.dto.AuthDtos.UserDto;
import com.cinemabook.api.dto.SocialDtos.UserSummaryResponse;
import com.cinemabook.application.service.SocialService;
import com.cinemabook.domain.user.AccountStatus;
import com.cinemabook.infrastructure.persistence.UserEntity;
import com.cinemabook.infrastructure.persistence.UserRepository;
import com.cinemabook.shared.exception.ResourceNotFoundException;
import com.cinemabook.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * User profile reads and search.
 * Single Responsibility: HTTP mapping only.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final SocialService  socialService;

    /** GET /users/me — current authenticated user's profile */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getMe(
            @AuthenticationPrincipal UUID userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return ResponseEntity.ok(ApiResponse.ok(toDto(user)));
    }

    /** GET /users/{username} — public profile by username */
    @GetMapping("/{username}")
    public ResponseEntity<ApiResponse<PublicProfileDto>> getPublicProfile(
            @PathVariable String username) {

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        if (!AccountStatus.ACTIVE.equals(user.getAccountStatus())) {
            throw new ResourceNotFoundException("User", username);
        }
        return ResponseEntity.ok(ApiResponse.ok(toPublicDto(user)));
    }

    /**
     * GET /users/search?q={query}&page=0&size=20
     * Search active users by username or display name.
     * Returns UserSummaryResponse with follow counts + followedByMe.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<UserSearchPageResponse>> searchUsers(
            @AuthenticationPrincipal UUID requesterId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok(
                    new UserSearchPageResponse(List.of(), 0, size, 0, 0)));
        }

        Page<UserEntity> result = userRepository.searchUsers(
                q.trim(), PageRequest.of(page, size));

        List<UserSummaryResponse> users = result.getContent().stream()
                .map(u -> socialService.toUserSummary(u, requesterId))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(
                new UserSearchPageResponse(users,
                        result.getNumber(), result.getSize(),
                        result.getTotalElements(), result.getTotalPages())));
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record PublicProfileDto(
            String id,
            String username,
            String displayName,
            String avatarUrl,
            String bio,
            String userType
    ) {}

    public record UserSearchPageResponse(
            List<UserSummaryResponse> users,
            int  page,
            int  size,
            long totalElements,
            int  totalPages
    ) {}

    private UserDto toDto(UserEntity u) {
        return new UserDto(
                u.getId().toString(), u.getEmail(), u.getUsername(),
                u.getDisplayName(), u.getUserType(), u.getAccountStatus(),
                u.isEmailVerified(), u.getAvatarUrl());
    }

    private PublicProfileDto toPublicDto(UserEntity u) {
        return new PublicProfileDto(
                u.getId().toString(), u.getUsername(),
                u.getDisplayName(), u.getAvatarUrl(),
                u.getBio(), u.getUserType().name());
    }
}