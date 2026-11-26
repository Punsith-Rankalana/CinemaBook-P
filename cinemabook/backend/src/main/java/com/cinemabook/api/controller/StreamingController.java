package com.cinemabook.api.controller;

import com.cinemabook.infrastructure.streaming.LocalStreamingProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
public class StreamingController {

    private final LocalStreamingProvider streamingProvider;

    @Value("${cinemabook.storage.local-path:${user.home}/cinemabook-storage}")
    private String storagePath;

    // ── Upload (dev only) ─────────────────────────────────────────────────────

    @PutMapping(value = "/upload/**", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Void> uploadFile(HttpServletRequest request) throws IOException {
        String uri        = request.getRequestURI();
        String ctxPath    = request.getContextPath(); // "/api/v1"
        String storageKey = uri.replace(ctxPath + "/files/upload/", "");

        Path targetPath = Paths.get(storagePath, storageKey);
        Files.createDirectories(targetPath.getParent());

        try (InputStream in = request.getInputStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("File uploaded: key={} bytes={}", storageKey, Files.size(targetPath));
        return ResponseEntity.ok().build();
    }

    // ── Stream ────────────────────────────────────────────────────────────────

    /**
     * GET /files/stream?token={jwt}
     *
     * Serves video bytes using HTTP Range requests.
     *
     * IMPORTANT: Do NOT manually set Content-Range or Content-Length.
     * Spring's ResourceRegion message converter sets them automatically and
     * correctly. Setting them manually creates duplicate conflicting headers
     * which cause browsers to reject the response with MEDIA_ERR_SRC_NOT_SUPPORTED.
     */
    @GetMapping("/stream")
    public ResponseEntity<ResourceRegion> stream(
            @RequestParam String token,
            @RequestHeader HttpHeaders headers) throws IOException {

        // Validate signed JWT — throws if invalid or expired
        LocalStreamingProvider.StreamTokenClaims claims =
                streamingProvider.validateStreamToken(token);

        Path filePath = Paths.get(storagePath, claims.storageKey());

        if (!Files.exists(filePath)) {
            log.warn("Stream file not found: storageKey={} fullPath={}",
                    claims.storageKey(), filePath.toAbsolutePath());
            return ResponseEntity.notFound().build();
        }

        Resource resource  = new FileSystemResource(filePath);
        long contentLength = resource.contentLength();

        // Detect actual content type from file
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "video/mp4"; // fallback
        }

        // Calculate the byte range to serve
        long chunkSize = 1_048_576L; // 1 MB per chunk

        long rangeStart;
        long rangeEnd;

        if (headers.getRange().isEmpty()) {
            // No Range header — serve first chunk
            rangeStart = 0;
            rangeEnd   = Math.min(chunkSize - 1, contentLength - 1);
        } else {
            HttpRange range = headers.getRange().get(0);
            rangeStart = range.getRangeStart(contentLength);
            rangeEnd   = range.getRangeEnd(contentLength);
        }

        long rangeLength = Math.min(chunkSize, rangeEnd - rangeStart + 1);

        ResourceRegion region = new ResourceRegion(resource, rangeStart, rangeLength);

        log.debug("Streaming: key={} range={}-{}/{} chunk={}",
                claims.storageKey(), rangeStart, rangeStart + rangeLength - 1,
                contentLength, rangeLength);

        return ResponseEntity
                .status(HttpStatus.PARTIAL_CONTENT)
                // Anti-download headers
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                // DO NOT set Content-Range or Content-Length here —
                // ResourceRegion message converter handles them correctly
                .contentType(MediaType.parseMediaType(contentType))
                .body(region);
    }

    // ── Clip download ─────────────────────────────────────────────────────────

    @GetMapping("/clips/{watcherId}/{filename}")
    public ResponseEntity<Resource> downloadClip(
            @PathVariable String watcherId,
            @PathVariable String filename) throws IOException {

        Path clipPath = Paths.get(storagePath, "clips", watcherId, filename);

        if (!Files.exists(clipPath)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"cinemabook-clip-" + filename + "\"")
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(new FileSystemResource(clipPath));
    }
}