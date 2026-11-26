package com.cinemabook.infrastructure.streaming;

import com.cinemabook.domain.streaming.ClipResult;
import com.cinemabook.domain.streaming.StreamSession;
import com.cinemabook.domain.streaming.StreamingProvider;
import com.cinemabook.shared.exception.BusinessRuleException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Local / dev streaming provider.
 *
 * Stream sessions: issues short-lived signed JWTs that the
 * StreamingController validates on every chunk request.
 *
 * Clips: runs FFmpeg locally (must be installed) with a text watermark.
 * Falls back gracefully if FFmpeg is not installed.
 *
 * Open/Closed: swap for CloudFrontStreamingProvider in prod by changing profile.
 */
@Service
@Profile({"default", "local", "test"})
@Slf4j
public class LocalStreamingProvider implements StreamingProvider {

    private final String     baseUrl;
    private final String     storagePath;
    private final String     clipsPath;
    private final SecretKey  streamKey;

    public LocalStreamingProvider(
            @Value("${cinemabook.storage.local-base-url:http://localhost:8082/api/v1/files}") String baseUrl,
            @Value("${cinemabook.storage.local-path:${user.home}/cinemabook-storage}") String storagePath,
            @Value("${cinemabook.jwt.secret}") String jwtSecret) {
        this.baseUrl     = baseUrl;
        this.storagePath = storagePath;
        this.clipsPath   = storagePath + "/clips";
        this.streamKey   = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        try {
            Files.createDirectories(Paths.get(this.clipsPath));
        } catch (IOException e) {
            log.warn("Could not create clips directory: {}", e.getMessage());
        }
    }

    // ── Stream session ────────────────────────────────────────────────────────

    @Override
    public StreamSession createSession(String storageKey, UUID watcherId,
                                       UUID ticketId, int expirySeconds) {
        String sessionId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(expirySeconds);

        // Short-lived signed JWT embedded in the stream URL
        String token = Jwts.builder()
                .subject(sessionId)
                .claim("watcherId", watcherId.toString())
                .claim("ticketId",  ticketId.toString())
                .claim("storageKey", storageKey)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiresAt))
                .signWith(streamKey)
                .compact();

        // Stream URL includes the token — validated on every chunk request
        String streamUrl = baseUrl + "/stream?token=" + token;

        log.debug("Stream session created: sessionId={} watcherId={} expiresIn={}s",
                sessionId, watcherId, expirySeconds);

        return new StreamSession(sessionId, streamUrl, expiresAt);
    }

    @Override
    public void revokeSession(String sessionId) {
        // In prod: add sessionId to a Redis blocklist
        // In dev: log only — token expiry handles this
        log.debug("Session revoked: sessionId={}", sessionId);
    }

    // ── Clip extraction ───────────────────────────────────────────────────────

    @Override
    public ClipResult extractClip(String storageKey, UUID watcherId,
                                  int startSeconds, int durationSeconds) {
        String sourceFile = storagePath + "/" + storageKey;
        String clipKey    = "clips/" + watcherId + "/" + UUID.randomUUID() + ".mp4";
        String clipFile   = storagePath + "/" + clipKey;

        try {
            Files.createDirectories(Paths.get(clipFile).getParent());
        } catch (IOException e) {
            throw new BusinessRuleException("Cannot prepare clip directory: " + e.getMessage());
        }

        if (!Files.exists(Paths.get(sourceFile))) {
            throw new BusinessRuleException("Source film file not found in storage.");
        }

        String watermark = "CinemaBook | " + watcherId + " | " + Instant.now();

        boolean ffmpegSuccess = runFfmpeg(sourceFile, clipFile,
                startSeconds, durationSeconds, watermark);

        if (!ffmpegSuccess) {
            // Fallback for dev when FFmpeg is not installed: copy a dummy clip
            log.warn("FFmpeg not available — returning placeholder clip for dev");
            try {
                Files.copy(Paths.get(sourceFile), Paths.get(clipFile));
            } catch (IOException e) {
                throw new BusinessRuleException("Clip extraction failed: " + e.getMessage());
            }
        }

        String clipUrl = baseUrl + "/" + clipKey;
        log.info("Clip extracted: watcherId={} key={} start={}s duration={}s",
                watcherId, clipKey, startSeconds, durationSeconds);

        return new ClipResult(clipKey, clipUrl, durationSeconds);
    }

    // ── FFmpeg runner ─────────────────────────────────────────────────────────

    private boolean runFfmpeg(String sourceFile, String outputFile,
                              int startSeconds, int durationSeconds,
                              String watermark) {
        try {
            // Sanitise watermark for shell safety
            String safeWatermark = watermark.replaceAll("[^a-zA-Z0-9 |\\-:]", "");

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-y",                                       // overwrite output
                    "-ss",  String.valueOf(startSeconds),       // start time
                    "-i",   sourceFile,                         // input
                    "-t",   String.valueOf(durationSeconds),    // duration
                    "-vf",  "drawtext=text='" + safeWatermark  // watermark overlay
                    + "':fontcolor=white:fontsize=18"
                    + ":box=1:boxcolor=black@0.5"
                    + ":x=10:y=10",
                    "-c:v", "libx264",                          // re-encode for watermark
                    "-c:a", "aac",
                    "-movflags", "+faststart",                  // web-optimised
                    outputFile
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.warn("FFmpeg exited with code {}", exitCode);
                return false;
            }
            return true;
        } catch (IOException e) {
            // FFmpeg not installed — expected in many dev environments
            log.debug("FFmpeg not found ({}): {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessRuleException("Clip extraction was interrupted");
        }
    }

    // ── Token validation (used by StreamingController) ────────────────────────

    public StreamTokenClaims validateStreamToken(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(streamKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new StreamTokenClaims(
                    claims.getSubject(),
                    UUID.fromString(claims.get("watcherId", String.class)),
                    UUID.fromString(claims.get("ticketId",  String.class)),
                    claims.get("storageKey", String.class)
            );
        } catch (Exception e) {
            throw new BusinessRuleException("Invalid or expired stream token");
        }
    }

    public record StreamTokenClaims(
            String sessionId,
            UUID   watcherId,
            UUID   ticketId,
            String storageKey
    ) {}
}
