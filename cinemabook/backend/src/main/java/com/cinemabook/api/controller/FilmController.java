package com.cinemabook.api.controller;

import com.cinemabook.api.dto.FilmDtos.*;
import com.cinemabook.application.service.BankLinkService;
import com.cinemabook.application.service.FilmService;
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
 * Film management REST API.
 * Single Responsibility: HTTP mapping only. Logic in FilmService / BankLinkService.
 */
@RestController
@RequiredArgsConstructor
public class FilmController {

    private final FilmService     filmService;
    private final BankLinkService bankLinkService;

    // ═══════════════════════════════════════════════════════════════
    // FILM OWNER ENDPOINTS  (/owner/films)
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/owner/films")
    @PreAuthorize("hasRole('FILM_OWNER')")
    public ResponseEntity<ApiResponse<FilmUploadUrlResponse>> createFilm(
            @AuthenticationPrincipal UUID ownerId,
            @Valid @RequestBody CreateFilmRequest req) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(filmService.createFilmAndGetUploadUrl(ownerId, req)));
    }

    @PostMapping("/owner/films/{filmId}/confirm-upload")
    @PreAuthorize("hasRole('FILM_OWNER')")
    public ResponseEntity<ApiResponse<FilmResponse>> confirmUpload(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID filmId,
            @Valid @RequestBody ConfirmUploadRequest req) {

        return ResponseEntity.ok(ApiResponse.ok(
                filmService.confirmUpload(ownerId, filmId, req)));
    }

    @PatchMapping("/owner/films/{filmId}")
    @PreAuthorize("hasRole('FILM_OWNER')")
    public ResponseEntity<ApiResponse<FilmResponse>> updateFilm(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID filmId,
            @Valid @RequestBody UpdateFilmRequest req) {

        return ResponseEntity.ok(ApiResponse.ok(
                filmService.updateFilm(ownerId, filmId, req)));
    }

    @PatchMapping("/owner/films/{filmId}/price")
    @PreAuthorize("hasRole('FILM_OWNER')")
    public ResponseEntity<ApiResponse<FilmResponse>> updatePrice(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID filmId,
            @Valid @RequestBody UpdatePriceRequest req) {

        return ResponseEntity.ok(ApiResponse.ok(
                filmService.updatePrice(ownerId, filmId, req)));
    }

    @PostMapping("/owner/films/{filmId}/publish")
    @PreAuthorize("hasRole('FILM_OWNER')")
    public ResponseEntity<ApiResponse<FilmResponse>> publishFilm(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID filmId) {

        return ResponseEntity.ok(ApiResponse.ok(
                filmService.publishFilm(ownerId, filmId)));
    }

    @PostMapping("/owner/films/{filmId}/unpublish")
    @PreAuthorize("hasRole('FILM_OWNER')")
    public ResponseEntity<ApiResponse<FilmResponse>> unpublishFilm(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID filmId) {

        return ResponseEntity.ok(ApiResponse.ok(
                filmService.unpublishFilm(ownerId, filmId)));
    }

    @GetMapping("/owner/films")
    @PreAuthorize("hasRole('FILM_OWNER')")
    public ResponseEntity<ApiResponse<FilmPageResponse>> getOwnerFilms(
            @AuthenticationPrincipal UUID ownerId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String status) {

        return ResponseEntity.ok(ApiResponse.ok(
                filmService.getOwnerFilms(ownerId, page, size, status)));
    }

    @GetMapping("/owner/films/{filmId}")
    @PreAuthorize("hasRole('FILM_OWNER')")
    public ResponseEntity<ApiResponse<FilmResponse>> getOwnerFilm(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID filmId) {

        return ResponseEntity.ok(ApiResponse.ok(
                filmService.getFilm(filmId, ownerId)));
    }

    @GetMapping("/owner/films/{filmId}/price-history")
    @PreAuthorize("hasRole('FILM_OWNER')")
    public ResponseEntity<ApiResponse<List<PriceHistoryEntry>>> getPriceHistory(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID filmId) {

        return ResponseEntity.ok(ApiResponse.ok(
                filmService.getPriceHistory(ownerId, filmId)));
    }

    @GetMapping("/owner/stats")
    @PreAuthorize("hasRole('FILM_OWNER')")
    public ResponseEntity<ApiResponse<OwnerStatsResponse>> getOwnerStats(
            @AuthenticationPrincipal UUID ownerId) {

        return ResponseEntity.ok(ApiResponse.ok(
                filmService.getOwnerStats(ownerId)));
    }

    // ═══════════════════════════════════════════════════════════════
    // BANK LINK ENDPOINTS  (/owner/bank)
    // ═══════════════════════════════════════════════════════════════

    @PostMapping("/owner/bank/link")
    @PreAuthorize("hasRole('FILM_OWNER')")
    public ResponseEntity<ApiResponse<BankLinkResponse>> initiateBankLink(
            @AuthenticationPrincipal UUID ownerId) {

        return ResponseEntity.ok(ApiResponse.ok(
                bankLinkService.initiateBankLink(ownerId)));
    }

    @PostMapping("/owner/bank/complete")
    @PreAuthorize("hasRole('FILM_OWNER')")
    public ResponseEntity<ApiResponse<Void>> completeBankLink(
            @AuthenticationPrincipal UUID ownerId) {

        bankLinkService.completeBankLink(ownerId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @GetMapping("/owner/bank/status")
    @PreAuthorize("hasRole('FILM_OWNER')")
    public ResponseEntity<ApiResponse<BankLinkService.BankLinkStatusResponse>> getBankLinkStatus(
            @AuthenticationPrincipal UUID ownerId) {

        return ResponseEntity.ok(ApiResponse.ok(
                bankLinkService.getBankLinkStatus(ownerId)));
    }

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC FILM ENDPOINTS  (/films) — any authenticated user
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET /films — browse ALL published films across all owners.
     * Fix: was calling getOwnerFilms(userId, ...) which only returns that user's films.
     *      Now calls browsePublishedFilms() which queries across all owners.
     */
    @GetMapping("/films")
    public ResponseEntity<ApiResponse<FilmPageResponse>> browseFilms(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok(
                filmService.browsePublishedFilms(page, size)));
    }

    @GetMapping("/films/{filmId}")
    public ResponseEntity<ApiResponse<FilmResponse>> getFilm(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID filmId) {

        return ResponseEntity.ok(ApiResponse.ok(
                filmService.getFilm(filmId, userId)));
    }
}