package com.cinemabook.api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class WatcherDtos {

    private WatcherDtos() {}

    // ── Requests ─────────────────────────────────────────────────────────────

    public record PurchaseTicketRequest(
            @NotNull(message = "Film ID is required")
            String filmId,

            @NotBlank(message = "Payment token is required")
            String paymentToken      // Stripe PaymentMethod ID from frontend
    ) {}

    public record StartStreamRequest(
            @NotNull(message = "Film ID is required")
            String filmId
    ) {}

    public record UpdateProgressRequest(
            @NotNull(message = "Position is required")
            @Min(value = 0, message = "Position must be 0 or greater")
            Integer positionSeconds,

            @NotBlank(message = "Event type is required")
            @Pattern(regexp = "START|PROGRESS|PAUSE|COMPLETE",
                    message = "Event type must be START, PROGRESS, PAUSE, or COMPLETE")
            String eventType
    ) {}

    public record RequestClipRequest(
            @NotNull(message = "Start time is required")
            @Min(value = 0, message = "Start time cannot be negative")
            Integer startSeconds,

            @NotNull(message = "Duration is required")
            @Min(value = 1,   message = "Duration must be at least 1 second")
            @Max(value = 300, message = "Duration cannot exceed 300 seconds (5 minutes)")
            Integer durationSeconds
    ) {}

    // ── Responses ────────────────────────────────────────────────────────────

    public record TicketResponse(
            String      id,
            String      filmId,
            String      filmTitle,
            String      filmThumbnailUrl,
            BigDecimal  pricePaid,
            String      currency,
            String      status,
            int         clipSecondsUsed,
            int         clipSecondsRemaining,
            int         watchPositionSec,
            Instant     purchasedAt,
            Instant     expiresAt,
            Instant     lastWatchedAt
    ) {}

    public record TicketPageResponse(
            List<TicketResponse> tickets,
            int    page,
            int    size,
            long   totalElements,
            int    totalPages
    ) {}

    public record StreamSessionResponse(
            String  sessionId,
            String  streamUrl,       // signed URL — client streams from here
            String  filmTitle,
            int     durationSeconds,
            int     resumePositionSec,
            int     clipSecondsRemaining,
            Instant expiresAt,
            boolean widevineEnabled   // always false in dev, true in prod with DRM
    ) {}

    public record ClipJobResponse(
            String  jobId,
            String  filmId,
            String  status,          // PENDING | PROCESSING | DONE | FAILED
            String  clipUrl,         // available when status = DONE
            int     startSeconds,
            int     durationSeconds,
            String  errorMessage,
            Instant createdAt
    ) {}

    public record PaymentConfirmResponse(
            String      ticketId,
            String      filmTitle,
            BigDecimal  pricePaid,
            String      currency,
            Instant     purchasedAt,
            String      message
    ) {}
}