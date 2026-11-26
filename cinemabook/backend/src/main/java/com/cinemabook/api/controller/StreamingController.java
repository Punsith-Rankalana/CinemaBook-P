package com.cinemabook.api.controller;

import com.cinemabook.infrastructure.streaming.LocalStreamingProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Serves chunked video bytes for local/dev streaming.
 *
 * Key security features:
 *   1. Token validation on every request — expired or invalid tokens are rejected.
 *   2. No Content-Disposition: attachment — prevents "save as" downloads.
 *   3. Chunked Range responses only — browser can play but not trivially download.
 *   4. No CORS wildcard — only the app origin can request the stream.
 *   5. Cache-Control: no-store — browser cannot cache the video to disk.
 *
 * In production this controller is replaced by CloudFront signed URLs
 * with Widevine DRM — the CDN serves bytes directly, the API only issues tokens.
 *
 * Open/Closed: this controller handles local dev only (no @Profile annotation
 * so it always registers, but in prod the stream URL points to CDN not here).
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
public class StreamingController {

    private final LocalStreamingProvider streamingProvider;

    @Value("${cinemabook.storage.local-path:${user.home}/cinemabook-storage}")
    private String storagePath;

    /**
     * GET /files/stream?token={signedToken}
     *
     * Validates the signed token, then streams the video file in chunks
     * using HTTP Range requests (standard for HTML5 video players).
     *
     * The player sends Range: bytes=0-1048575 and we respond with
     * 206 Partial Content — the browser plays but cannot easily save.
     */
    @GetMapping("/stream")
    public ResponseEntity<ResourceRegion> stream(
            @RequestParam String token,
            @RequestHeader HttpHeaders headers) throws IOException {

        // Validate signed token — throws BusinessRuleException if invalid/expired
        LocalStreamingProvider.StreamTokenClaims claims =
                streamingProvider.validateStreamToken(token);

        Path filePath = Paths.get(storagePath, claims.storageKey());

        if (!Files.exists(filePath)) {
            log.warn("Stream file not found: key={}", claims.storageKey());
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(filePath);
        long contentLength = resource.contentLength();

        // Parse Range header from the player (e.g. "bytes=0-1048575")
        HttpRange range = headers.getRange().isEmpty()
                ? HttpRange.createByteRange(0, Math.min(1_048_575L, contentLength - 1))
                : headers.getRange().get(0);

        long rangeStart  = range.getRangeStart(contentLength);
        long rangeEnd    = range.getRangeEnd(contentLength);
        long rangeLength = Math.min(2_097_152L, rangeEnd - rangeStart + 1); // 2MB max chunk

        ResourceRegion region = new ResourceRegion(resource, rangeStart, rangeLength);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                // ── Anti-download headers ──────────────────────────────────
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")        // NOT attachment
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache")  // no disk cache
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header("X-Content-Type-Options", "nosniff")
                // ── Stream headers ─────────────────────────────────────────
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE,
                        "bytes " + rangeStart + "-" + (rangeStart + rangeLength - 1)
                                + "/" + contentLength)
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(region);
    }

    /**
     * GET /files/clips/{watcherId}/{filename}
     *
     * Serves a completed watermarked clip for download.
     * Unlike streaming, clips ARE downloadable — that's their purpose.
     * The watermark (userId + timestamp) is baked into the video itself.
     */
    @GetMapping("/clips/{watcherId}/{filename}")
    public ResponseEntity<Resource> downloadClip(
            @PathVariable String watcherId,
            @PathVariable String filename) throws IOException {

        Path clipPath = Paths.get(storagePath, "clips", watcherId, filename);

        if (!Files.exists(clipPath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(clipPath);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"cinemabook-clip-" + filename + "\"")
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(resource);
    }
}