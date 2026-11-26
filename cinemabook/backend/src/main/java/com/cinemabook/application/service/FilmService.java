package com.cinemabook.application.service;

import com.cinemabook.api.dto.FilmDtos.*;
import com.cinemabook.domain.film.DefaultFilmUploadPolicy;
import com.cinemabook.domain.film.StorageProvider;
import com.cinemabook.infrastructure.persistence.*;
import com.cinemabook.shared.exception.BusinessRuleException;
import com.cinemabook.shared.exception.ResourceNotFoundException;
import com.cinemabook.shared.exception.UnauthorisedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Single Responsibility: all film management use-cases.
 * Open/Closed: StorageProvider and FilmUploadPolicy are injected abstractions.
 * Dependency Inversion: depends on StorageProvider interface, not concrete class.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FilmService {

    private final FilmRepository             filmRepository;
    private final FilmUploadJobRepository    uploadJobRepository;
    private final FilmPriceHistoryRepository priceHistoryRepository;
    private final FilmOwnerProfileRepository ownerProfileRepository;
    private final UserRepository             userRepository;
    private final StorageProvider            storageProvider;
    private final DefaultFilmUploadPolicy    uploadPolicy;

    private static final long UPLOAD_URL_EXPIRY_SECONDS = 3600;

    // ── Create film + get upload URL ─────────────────────────────────────────

    @Transactional
    public FilmUploadUrlResponse createFilmAndGetUploadUrl(UUID ownerId,
                                                           CreateFilmRequest req) {
        UserEntity owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

        ownerProfileRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessRuleException(
                        "Film owner profile not found."));

        FilmEntity film = FilmEntity.builder()
                .owner(owner)
                .title(req.title())
                .description(req.description())
                .genre(req.genre())
                .tags(req.tags() != null ? req.tags().toArray(new String[0]) : null)
                .ticketPrice(req.ticketPrice())
                .currency(req.currency() != null ? req.currency() : "USD")
                .releaseYear(req.releaseYear())
                .language(req.language())
                .ageRating(req.ageRating())
                .status(FilmEntity.FilmStatus.PROCESSING)
                .build();

        film = filmRepository.save(film);

        String storageKey = buildStorageKey(ownerId, film.getId());
        String uploadUrl  = storageProvider.generateUploadPresignedUrl(
                storageKey, Duration.ofSeconds(UPLOAD_URL_EXPIRY_SECONDS));

        FilmUploadJobEntity job = FilmUploadJobEntity.builder()
                .film(film)
                .owner(owner)
                .storageKey(storageKey)
                .jobStatus(FilmUploadJobEntity.JobStatus.PENDING)
                .build();
        uploadJobRepository.save(job);

        recordPriceHistory(film, null, req.ticketPrice(), req.currency(), owner);

        log.info("Film created: filmId={} ownerId={}", film.getId(), ownerId);

        return new FilmUploadUrlResponse(
                film.getId().toString(),
                uploadUrl,
                storageKey,
                UPLOAD_URL_EXPIRY_SECONDS
        );
    }

    // ── Confirm upload ───────────────────────────────────────────────────────

    @Transactional
    public FilmResponse confirmUpload(UUID ownerId, UUID filmId,
                                      ConfirmUploadRequest req) {
        FilmEntity film = findOwnedFilm(filmId, ownerId);

        uploadPolicy.validate(req.contentType(), req.fileSizeBytes(), ownerId);

        if (!storageProvider.exists(req.storageKey())) {
            throw new BusinessRuleException(
                    "Upload not found in storage. Please try uploading again.");
        }

        film.setStorageKey(req.storageKey());
        film.setStatus(FilmEntity.FilmStatus.READY);
        filmRepository.save(film);

        uploadJobRepository.findByFilmId(filmId).ifPresent(job -> {
            job.setJobStatus(FilmUploadJobEntity.JobStatus.DONE);
            job.setFileSizeBytes(req.fileSizeBytes());
            job.setOriginalFilename(req.storageKey());
            uploadJobRepository.save(job);
        });

        log.info("Upload confirmed: filmId={} ownerId={}", filmId, ownerId);
        return toFilmResponse(film);
    }

    // ── Publish / unpublish ──────────────────────────────────────────────────

    @Transactional
    public FilmResponse publishFilm(UUID ownerId, UUID filmId) {
        FilmEntity film = findOwnedFilm(filmId, ownerId);

        if (film.getStorageKey() == null) {
            throw new BusinessRuleException(
                    "Cannot publish film — no video has been uploaded yet.");
        }
        if (FilmEntity.FilmStatus.REJECTED.equals(film.getStatus())) {
            throw new BusinessRuleException("Rejected films cannot be published.");
        }

        film.setStatus(FilmEntity.FilmStatus.PUBLISHED);
        filmRepository.save(film);
        log.info("Film published: filmId={}", filmId);
        return toFilmResponse(film);
    }

    @Transactional
    public FilmResponse unpublishFilm(UUID ownerId, UUID filmId) {
        FilmEntity film = findOwnedFilm(filmId, ownerId);
        film.setStatus(FilmEntity.FilmStatus.UNPUBLISHED);
        filmRepository.save(film);
        log.info("Film unpublished: filmId={}", filmId);
        return toFilmResponse(film);
    }

    // ── Update metadata ──────────────────────────────────────────────────────

    @Transactional
    public FilmResponse updateFilm(UUID ownerId, UUID filmId, UpdateFilmRequest req) {
        FilmEntity film = findOwnedFilm(filmId, ownerId);

        if (req.title()       != null) film.setTitle(req.title());
        if (req.description() != null) film.setDescription(req.description());
        if (req.genre()       != null) film.setGenre(req.genre());
        if (req.tags()        != null) film.setTags(req.tags().toArray(new String[0]));
        if (req.releaseYear() != null) film.setReleaseYear(req.releaseYear());
        if (req.language()    != null) film.setLanguage(req.language());
        if (req.ageRating()   != null) film.setAgeRating(req.ageRating());

        filmRepository.save(film);
        return toFilmResponse(film);
    }

    // ── Update price ─────────────────────────────────────────────────────────

    @Transactional
    public FilmResponse updatePrice(UUID ownerId, UUID filmId, UpdatePriceRequest req) {
        FilmEntity film = findOwnedFilm(filmId, ownerId);

        BigDecimal oldPrice = film.getTicketPrice();
        film.setTicketPrice(req.newPrice());
        filmRepository.save(film);

        UserEntity owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));
        recordPriceHistory(film, oldPrice, req.newPrice(), film.getCurrency(), owner);

        log.info("Price updated: filmId={} {} -> {}", filmId, oldPrice, req.newPrice());
        return toFilmResponse(film);
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FilmPageResponse getOwnerFilms(UUID ownerId, int page, int size, String status) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<FilmEntity> result;
        if (status != null && !status.isBlank()) {
            FilmEntity.FilmStatus filmStatus =
                    FilmEntity.FilmStatus.valueOf(status.toUpperCase());
            result = filmRepository.findByOwnerIdAndStatusOrderByCreatedAtDesc(
                    ownerId, filmStatus, pageable);
        } else {
            result = filmRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId, pageable);
        }

        return toPage(result);
    }

    /**
     * Browse ALL published films — used by the public /films endpoint.
     * Fix: was incorrectly reusing getOwnerFilms() which filters by owner.
     */
    @Transactional(readOnly = true)
    public FilmPageResponse browsePublishedFilms(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FilmEntity> result = filmRepository.findByStatusOrderByCreatedAtDesc(
                FilmEntity.FilmStatus.PUBLISHED, pageable);
        return toPage(result);
    }

    @Transactional(readOnly = true)
    public FilmResponse getFilm(UUID filmId, UUID requesterId) {
        FilmEntity film = filmRepository.findById(filmId)
                .orElseThrow(() -> new ResourceNotFoundException("Film", filmId));

        boolean isOwner = film.getOwner().getId().equals(requesterId);
        if (!isOwner && !FilmEntity.FilmStatus.PUBLISHED.equals(film.getStatus())) {
            throw new ResourceNotFoundException("Film", filmId);
        }
        return toFilmResponse(film);
    }

    @Transactional(readOnly = true)
    public OwnerStatsResponse getOwnerStats(UUID ownerId) {
        FilmOwnerProfileEntity profile = ownerProfileRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("FilmOwnerProfile", ownerId));

        return new OwnerStatsResponse(
                filmRepository.countByOwnerId(ownerId),
                filmRepository.countByOwnerIdAndStatus(ownerId, FilmEntity.FilmStatus.PUBLISHED),
                filmRepository.sumTicketsSoldByOwnerId(ownerId),
                filmRepository.sumRevenueByOwnerId(ownerId),
                profile.isBankAccountLinked(),
                profile.isPayoutEnabled(),
                profile.getStripeAccountId()
        );
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryEntry> getPriceHistory(UUID ownerId, UUID filmId) {
        findOwnedFilm(filmId, ownerId); // ownership check
        return priceHistoryRepository.findByFilmIdOrderByChangedAtDesc(filmId)
                .stream()
                .map(h -> new PriceHistoryEntry(
                        h.getOldPrice(),
                        h.getNewPrice(),
                        h.getCurrency(),
                        h.getChangedAt()))
                .toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private FilmEntity findOwnedFilm(UUID filmId, UUID ownerId) {
        FilmEntity film = filmRepository.findById(filmId)
                .orElseThrow(() -> new ResourceNotFoundException("Film", filmId));
        if (!film.getOwner().getId().equals(ownerId)) {
            throw new UnauthorisedException("You do not own this film");
        }
        return film;
    }

    private String buildStorageKey(UUID ownerId, UUID filmId) {
        return "films/" + ownerId + "/" + filmId + "/video.mp4";
    }

    private void recordPriceHistory(FilmEntity film, BigDecimal oldPrice,
                                    BigDecimal newPrice, String currency,
                                    UserEntity changedBy) {
        priceHistoryRepository.save(FilmPriceHistoryEntity.builder()
                .film(film)
                .oldPrice(oldPrice)
                .newPrice(newPrice)
                .currency(currency != null ? currency : "USD")
                .changedBy(changedBy)
                .build());
    }

    private FilmPageResponse toPage(Page<FilmEntity> result) {
        return new FilmPageResponse(
                result.getContent().stream().map(this::toFilmResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private FilmResponse toFilmResponse(FilmEntity f) {
        return new FilmResponse(
                f.getId().toString(),
                f.getOwner().getId().toString(),
                f.getOwner().getUsername(),
                f.getTitle(),
                f.getDescription(),
                f.getGenre(),
                f.getTags() != null ? List.of(f.getTags()) : null,
                f.getDurationSeconds(),
                f.getTicketPrice(),
                f.getCurrency(),
                f.getStatus().name(),
                f.getThumbnailUrl(),
                f.getTrailerUrl(),
                f.getReleaseYear(),
                f.getLanguage(),
                f.getAgeRating(),
                f.getTotalTicketsSold(),
                f.getTotalRevenue(),
                f.getCreatedAt(),
                f.getUpdatedAt()
        );
    }
}