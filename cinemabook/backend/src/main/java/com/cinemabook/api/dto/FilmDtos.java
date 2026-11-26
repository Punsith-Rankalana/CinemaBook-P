package com.cinemabook.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTOs for film management (Phase 2 — Film Owner side).
 * All are Java records — immutable and concise.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class FilmDtos {

    private FilmDtos() {}

    // ── Requests ─────────────────────────────────────────────────────────────

    /** Create a new film entry before upload starts. */
    public record CreateFilmRequest(
            @NotBlank(message = "Title is required")
            @Size(max = 255, message = "Title must not exceed 255 characters")
            String title,

            @Size(max = 5000, message = "Description too long")
            String description,

            @Size(max = 100, message = "Genre too long")
            String genre,

            List<String> tags,

            @NotNull(message = "Ticket price is required")
            @DecimalMin(value = "0.01", message = "Ticket price must be at least 0.01")
            @DecimalMax(value = "999.99", message = "Ticket price cannot exceed 999.99")
            @Digits(integer = 6, fraction = 2, message = "Invalid price format")
            BigDecimal ticketPrice,

            @Size(max = 3, min = 3, message = "Currency must be a 3-letter ISO code")
            String currency,

            Integer releaseYear,

            @Size(max = 50, message = "Language too long")
            String language,

            @Size(max = 10, message = "Age rating too long")
            String ageRating
    ) {}

    /** Update film metadata — all fields optional (PATCH semantics). */
    public record UpdateFilmRequest(
            @Size(max = 255) String title,
            @Size(max = 5000) String description,
            @Size(max = 100) String genre,
            List<String> tags,
            Integer releaseYear,
            @Size(max = 50) String language,
            @Size(max = 10) String ageRating
    ) {}

    /** Change the ticket price — recorded in price history. */
    public record UpdatePriceRequest(
            @NotNull(message = "New price is required")
            @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
            @DecimalMax(value = "999.99", message = "Price cannot exceed 999.99")
            @Digits(integer = 6, fraction = 2)
            BigDecimal newPrice
    ) {}

    /** Notify the server that a direct client upload completed. */
    public record ConfirmUploadRequest(
            @NotBlank(message = "Storage key is required")
            String storageKey,

            @NotNull(message = "File size is required")
            @Min(value = 1, message = "File size must be positive")
            Long fileSizeBytes,

            @NotBlank(message = "Content type is required")
            String contentType
    ) {}

    // ── Responses ────────────────────────────────────────────────────────────

    public record FilmResponse(
            String id,
            String ownerId,
            String ownerUsername,
            String title,
            String description,
            String genre,
            List<String> tags,
            Integer durationSeconds,
            BigDecimal ticketPrice,
            String currency,
            String status,
            String thumbnailUrl,
            String trailerUrl,
            Integer releaseYear,
            String language,
            String ageRating,
            long totalTicketsSold,
            BigDecimal totalRevenue,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record FilmUploadUrlResponse(
            String filmId,
            String uploadUrl,          // pre-signed URL — client uploads directly here
            String storageKey,         // the key to confirm after upload
            long expiresInSeconds
    ) {}

    public record FilmPageResponse(
            List<FilmResponse> films,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public record OwnerStatsResponse(
            long totalFilms,
            long publishedFilms,
            long totalTicketsSold,
            BigDecimal totalRevenue,
            boolean bankAccountLinked,
            boolean payoutEnabled,
            String stripeAccountId
    ) {}

    public record BankLinkResponse(
            String onboardingUrl,       // redirect user to this Stripe URL
            String stripeAccountId
    ) {}

    public record PriceHistoryEntry(
            BigDecimal oldPrice,
            BigDecimal newPrice,
            String currency,
            Instant changedAt
    ) {}

    public record FilmJobStatusResponse(
            String filmId,
            String jobId,
            String jobStatus,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt
    ) {}
}