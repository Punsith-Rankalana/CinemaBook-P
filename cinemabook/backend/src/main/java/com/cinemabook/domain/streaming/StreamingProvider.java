package com.cinemabook.domain.streaming;

import java.util.UUID;

/**
 * Open/Closed Principle:
 * Swap or add streaming backends (AWS CloudFront, Cloudflare Stream, etc.)
 * by creating new implementations — this contract never changes.
 *
 * Dependency Inversion:
 * StreamingService depends on this abstraction, not on any concrete CDN SDK.
 */
public interface StreamingProvider {

    /**
     * Generates a short-lived, signed streaming URL for a film.
     * The URL expires quickly to prevent sharing and leaking.
     *
     * @param storageKey  the film's object key in cloud storage
     * @param watcherId   used to embed watermark / tracking info
     * @param ticketId    validates entitlement
     * @param expirySeconds how long the URL remains valid
     * @return a signed URL the client can stream from
     */
    StreamSession createSession(String storageKey, UUID watcherId,
                                UUID ticketId, int expirySeconds);

    /**
     * Extracts a clip from a film server-side via FFmpeg.
     * The clip is watermarked with the watcher's ID.
     *
     * @param storageKey     source film object key
     * @param watcherId      embedded in watermark
     * @param startSeconds   where the clip begins
     * @param durationSeconds clip length (max 300 = 5 minutes)
     * @return ClipResult with the clip's storage key and shareable URL
     */
    ClipResult extractClip(String storageKey, UUID watcherId,
                           int startSeconds, int durationSeconds);

    /**
     * Revokes an active streaming session (e.g. on logout or ticket expiry).
     *
     * @param sessionId the session ID returned by createSession
     */
    void revokeSession(String sessionId);
}
