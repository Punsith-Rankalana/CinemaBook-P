package com.cinemabook.application.service;

import com.cinemabook.api.dto.AuthDtos.*;
import com.cinemabook.domain.user.AccountStatus;
import com.cinemabook.domain.user.UserType;
import com.cinemabook.infrastructure.persistence.*;
import com.cinemabook.infrastructure.security.JwtService;
import com.cinemabook.infrastructure.security.SessionService;
import com.cinemabook.shared.exception.ConflictException;
import com.cinemabook.shared.exception.UnauthorisedException;
import com.cinemabook.shared.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Single Responsibility: handles all authentication use-cases.
 * Open/Closed: new registration types (OAuth etc.) are new methods — this class is not modified.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final WatcherProfileRepository watcherProfileRepository;
    private final FilmOwnerProfileRepository filmOwnerProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;

    // ── Register watcher ─────────────────────────────────────────────────────

    @Transactional
    public RegisterResponse registerWatcher(RegisterWatcherRequest req) {
        validateNewUser(req.email(), req.username());

        UserEntity user = UserEntity.builder()
                .email(req.email().toLowerCase().trim())
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .userType(UserType.WATCHER)
                // TODO Phase 7: change back to PENDING_VERIFICATION once email is wired
                .accountStatus(AccountStatus.ACTIVE)
                .displayName(req.displayName() != null ? req.displayName() : req.username())
                .emailVerified(false)
                .build();

        user = userRepository.save(user);

        WatcherProfileEntity profile = WatcherProfileEntity.builder()
                .user(user)
                .build();
        watcherProfileRepository.save(profile);

        log.info("Watcher registered: userId={} email={}", user.getId(), user.getEmail());

        return new RegisterResponse(
                "Registration successful. You can now log in.",
                user.getId().toString()
        );
    }

    // ── Register film owner ───────────────────────────────────────────────────

    @Transactional
    public RegisterResponse registerFilmOwner(RegisterFilmOwnerRequest req) {
        validateNewUser(req.email(), req.username());

        UserEntity user = UserEntity.builder()
                .email(req.email().toLowerCase().trim())
                .username(req.username())
                .passwordHash(passwordEncoder.encode(req.password()))
                .userType(UserType.FILM_OWNER)
                // TODO Phase 7: change back to PENDING_VERIFICATION once email is wired
                .accountStatus(AccountStatus.ACTIVE)
                .displayName(req.displayName() != null ? req.displayName() : req.username())
                .emailVerified(false)
                .build();

        user = userRepository.save(user);

        FilmOwnerProfileEntity profile = FilmOwnerProfileEntity.builder()
                .user(user)
                .companyName(req.companyName())
                .build();
        filmOwnerProfileRepository.save(profile);

        log.info("FilmOwner registered: userId={} email={}", user.getId(), user.getEmail());

        return new RegisterResponse(
                "Registration successful. You can now log in.",
                user.getId().toString()
        );
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest req) {
        UserEntity user = userRepository.findByEmailOrUsername(req.login().trim())
                .orElseThrow(() -> new UnauthorisedException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorisedException("Invalid credentials");
        }

        if (!AccountStatus.ACTIVE.equals(user.getAccountStatus())) {
            throw new UnauthorisedException("Account is not active. Status: "
                    + user.getAccountStatus());
        }

        // Revoke all previous refresh tokens for this user
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());

        // Register new device — overwrites any previous device in Redis
        long ttlMs = jwtService.getRefreshTokenExpiryMs();
        sessionService.registerDevice(user.getId(), req.deviceId(),
                Duration.ofMillis(ttlMs));

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(),
                user.getUserType().name(), req.deviceId());

        String rawRefreshToken = jwtService.generateRefreshToken();

        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .user(user)
                .tokenHash(HashUtil.sha256(rawRefreshToken))
                .deviceId(req.deviceId())
                .deviceInfo(req.deviceInfo())
                .expiresAt(Instant.now().plusMillis(ttlMs))
                .build();
        refreshTokenRepository.save(refreshToken);

        userRepository.updateLastLogin(user.getId(), Instant.now());

        log.info("User logged in: userId={} deviceId={}", user.getId(), req.deviceId());

        return new AuthResponse(accessToken, rawRefreshToken,
                jwtService.getAccessTokenExpiryMs(), toUserDto(user));
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest req, String deviceId) {
        String hash = HashUtil.sha256(req.refreshToken());

        RefreshTokenEntity stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorisedException("Invalid refresh token"));

        if (!stored.isValid()) {
            throw new UnauthorisedException("Refresh token has expired or been revoked");
        }

        if (!sessionService.isDeviceAuthorised(stored.getUser().getId(), deviceId)) {
            throw new UnauthorisedException("Session invalid — please log in again");
        }

        // Rotate: revoke old, issue new
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        UserEntity user = stored.getUser();
        long ttlMs = jwtService.getRefreshTokenExpiryMs();

        String newAccess = jwtService.generateAccessToken(
                user.getId(), user.getEmail(),
                user.getUserType().name(), deviceId);

        String newRawRefresh = jwtService.generateRefreshToken();

        RefreshTokenEntity newToken = RefreshTokenEntity.builder()
                .user(user)
                .tokenHash(HashUtil.sha256(newRawRefresh))
                .deviceId(deviceId)
                .deviceInfo(stored.getDeviceInfo())
                .expiresAt(Instant.now().plusMillis(ttlMs))
                .build();
        refreshTokenRepository.save(newToken);

        sessionService.registerDevice(user.getId(), deviceId, Duration.ofMillis(ttlMs));

        return new AuthResponse(newAccess, newRawRefresh,
                jwtService.getAccessTokenExpiryMs(), toUserDto(user));
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
        sessionService.revokeSession(userId);
        log.info("User logged out: userId={}", userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateNewUser(String email, String username) {
        if (userRepository.existsByEmail(email.toLowerCase().trim())) {
            throw new ConflictException("Email is already registered");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username is already taken");
        }
    }

    private UserDto toUserDto(UserEntity u) {
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
}
