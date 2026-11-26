package com.cinemabook.api.controller;

import com.cinemabook.api.dto.WatcherDtos.*;
import com.cinemabook.application.service.StreamingService;
import com.cinemabook.application.service.TicketService;
import com.cinemabook.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * All watcher-side endpoints: tickets, streaming, clips.
 * Single Responsibility: HTTP mapping only.
 */
@RestController
@RequiredArgsConstructor
public class WatcherController {

    private final TicketService    ticketService;
    private final StreamingService streamingService;

    // ═══════════════════════════════════════════════════════════════
    // TICKET ENDPOINTS  (/watcher/tickets)
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /watcher/tickets/purchase
     * Buy a ticket for a film using a Stripe payment token.
     */
    @PostMapping("/watcher/tickets/purchase")
    @PreAuthorize("hasRole('WATCHER')")
    public ResponseEntity<ApiResponse<PaymentConfirmResponse>> purchaseTicket(
            @AuthenticationPrincipal UUID watcherId,
            @Valid @RequestBody PurchaseTicketRequest req) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(ticketService.purchaseTicket(watcherId, req)));
    }

    /**
     * GET /watcher/tickets
     * List all tickets owned by this watcher.
     */
    @GetMapping("/watcher/tickets")
    @PreAuthorize("hasRole('WATCHER')")
    public ResponseEntity<ApiResponse<TicketPageResponse>> getMyTickets(
            @AuthenticationPrincipal UUID watcherId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                ticketService.getMyTickets(watcherId, page, size)));
    }

    /**
     * GET /watcher/tickets/{ticketId}
     * Get a single ticket by ID.
     */
    @GetMapping("/watcher/tickets/{ticketId}")
    @PreAuthorize("hasRole('WATCHER')")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(
            @AuthenticationPrincipal UUID watcherId,
            @PathVariable UUID ticketId) {

        return ResponseEntity.ok(ApiResponse.ok(
                ticketService.getTicket(watcherId, ticketId)));
    }

    /**
     * PATCH /watcher/tickets/{ticketId}/progress
     * Update watch position and event type (START, PROGRESS, PAUSE, COMPLETE).
     */
    @PatchMapping("/watcher/tickets/{ticketId}/progress")
    @PreAuthorize("hasRole('WATCHER')")
    public ResponseEntity<ApiResponse<Void>> updateProgress(
            @AuthenticationPrincipal UUID watcherId,
            @PathVariable UUID ticketId,
            @Valid @RequestBody UpdateProgressRequest req) {

        ticketService.updateProgress(watcherId, ticketId, req);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    // ═══════════════════════════════════════════════════════════════
    // STREAMING ENDPOINTS  (/watcher/stream)
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /watcher/stream/start
     * Creates a short-lived signed streaming session for a film.
     * Watcher must own an active ticket.
     *
     * Returns a signed stream URL — the client plays from this URL directly.
     * The URL is token-gated and expires after 4 hours.
     */
    @PostMapping("/watcher/stream/start")
    @PreAuthorize("hasRole('WATCHER')")
    public ResponseEntity<ApiResponse<StreamSessionResponse>> startStream(
            @AuthenticationPrincipal UUID watcherId,
            @Valid @RequestBody StartStreamRequest req) {

        UUID filmId = UUID.fromString(req.filmId());
        return ResponseEntity.ok(ApiResponse.ok(
                streamingService.startSession(watcherId, filmId)));
    }

    // ═══════════════════════════════════════════════════════════════
    // CLIP ENDPOINTS  (/watcher/clips)
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /watcher/clips/request
     * Request a server-side watermarked clip of up to 5 minutes.
     *
     * The clip is extracted by FFmpeg on the server with the watcher's
     * user ID embedded as a watermark. This is the ONLY way to get a clip —
     * no screen capture can replicate the server-side watermark.
     *
     * Each ticket allows a maximum of 300 seconds (5 minutes) of clips total.
     */
    @PostMapping("/watcher/clips/request")
    @PreAuthorize("hasRole('WATCHER')")
    public ResponseEntity<ApiResponse<ClipJobResponse>> requestClip(
            @AuthenticationPrincipal UUID watcherId,
            @RequestParam String filmId,
            @Valid @RequestBody RequestClipRequest req) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        streamingService.requestClip(
                                watcherId, UUID.fromString(filmId), req)));
    }

    /**
     * GET /watcher/clips/history?filmId={filmId}
     * Get all clip jobs for a specific film ticket.
     */
    @GetMapping("/watcher/clips/history")
    @PreAuthorize("hasRole('WATCHER')")
    public ResponseEntity<ApiResponse<List<ClipJobResponse>>> getClipHistory(
            @AuthenticationPrincipal UUID watcherId,
            @RequestParam String filmId) {

        return ResponseEntity.ok(ApiResponse.ok(
                streamingService.getClipHistory(
                        watcherId, UUID.fromString(filmId))));
    }
}