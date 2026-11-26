package com.cinemabook.application.service;

import com.cinemabook.api.dto.WatcherDtos.*;
import com.cinemabook.domain.streaming.ClipResult;
import com.cinemabook.domain.streaming.StreamSession;
import com.cinemabook.domain.streaming.StreamingProvider;
import com.cinemabook.infrastructure.persistence.*;
import com.cinemabook.shared.exception.BusinessRuleException;
import com.cinemabook.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Single Responsibility: streaming session creation and clip extraction.
 *
 * Open/Closed: StreamingProvider is injected — swap LocalStreamingProvider
 * for CloudFrontStreamingProvider in prod without touching this class.
 *
 * Dependency Inversion: depends on StreamingProvider (interface).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingService {

    private static final int SESSION_EXPIRY_SECONDS = 14400; // 4 hours
    private static final int MAX_CLIP_SECONDS       = 300;   // 5 minutes

    private final TicketRepository           ticketRepository;
    private final FilmRepository             filmRepository;
    private final StreamingSessionRepository sessionRepository;
    private final ClipJobRepository          clipJobRepository;
    private final UserRepository             userRepository;
    private final TicketService              ticketService;
    private final StreamingProvider          streamingProvider;

    // ── Start streaming session ───────────────────────────────────────────────

    @Transactional
    public StreamSessionResponse startSession(UUID watcherId, UUID filmId) {

        // 1. Validate entitlement — throws if no active ticket
        TicketEntity ticket = ticketService.validateEntitlement(watcherId, filmId);

        FilmEntity film = filmRepository.findById(filmId)
                .orElseThrow(() -> new ResourceNotFoundException("Film", filmId));

        if (film.getStorageKey() == null) {
            throw new BusinessRuleException("Film is not yet available for streaming.");
        }

        // 2. Revoke any existing sessions for this watcher + film
        sessionRepository.revokeAllForTicket(ticket.getId(), Instant.now());

        // 3. Create new signed session via the provider
        StreamSession session = streamingProvider.createSession(
                film.getStorageKey(), watcherId, ticket.getId(),
                SESSION_EXPIRY_SECONDS);

        // 4. Persist the session record
        UserEntity watcher = userRepository.findById(watcherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", watcherId));

        StreamingSessionEntity entity = StreamingSessionEntity.builder()
                .ticket(ticket)
                .watcher(watcher)
                .film(film)
                .sessionToken(session.sessionId())
                .streamUrl(session.streamUrl())
                .expiresAt(session.expiresAt())
                .build();
        sessionRepository.save(entity);

        // 5. Update ticket last watched timestamp
        ticketRepository.updateWatchPosition(
                ticket.getId(), ticket.getWatchPositionSec(), Instant.now());

        log.info("Stream session started: watcherId={} filmId={} sessionId={}",
                watcherId, filmId, session.sessionId());

        return new StreamSessionResponse(
                session.sessionId(),
                session.streamUrl(),
                film.getTitle(),
                film.getDurationSeconds() != null ? film.getDurationSeconds() : 0,
                ticket.getWatchPositionSec(),
                ticket.clipSecondsRemaining(),
                session.expiresAt(),
                false   // Widevine DRM: false in dev, true in prod
        );
    }

    // ── Request a clip ────────────────────────────────────────────────────────

    @Transactional
    public ClipJobResponse requestClip(UUID watcherId, UUID filmId,
                                       RequestClipRequest req) {
        // 1. Validate entitlement
        TicketEntity ticket = ticketService.validateEntitlement(watcherId, filmId);

        // 2. Check clip seconds budget
        if (!ticket.hasClipSecondsRemaining()) {
            throw new BusinessRuleException(
                    "You have used all your clip seconds for this film. " +
                            "Each ticket allows up to 5 minutes of clips total.");
        }

        int requestedDuration = req.durationSeconds();
        if (ticket.getClipSecondsUsed() + requestedDuration > MAX_CLIP_SECONDS) {
            int remaining = ticket.clipSecondsRemaining();
            throw new BusinessRuleException(
                    "Clip duration exceeds your remaining allowance. " +
                            "You have " + remaining + " seconds remaining.");
        }

        FilmEntity film = filmRepository.findById(filmId)
                .orElseThrow(() -> new ResourceNotFoundException("Film", filmId));

        if (film.getStorageKey() == null) {
            throw new BusinessRuleException("Film file not available.");
        }

        UserEntity watcher = userRepository.findById(watcherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", watcherId));

        String watermark = "CinemaBook | " + watcherId + " | " + Instant.now();

        // 3. Create the clip job record first (PROCESSING)
        ClipJobEntity job = ClipJobEntity.builder()
                .ticket(ticket)
                .watcher(watcher)
                .film(film)
                .startSeconds(req.startSeconds())
                .durationSeconds(requestedDuration)
                .watermarkText(watermark)
                .status(ClipJobEntity.ClipStatus.PROCESSING)
                .build();
        job = clipJobRepository.save(job);

        // 4. Deduct clip seconds immediately (prevents double-spending)
        ticketRepository.incrementClipSecondsUsed(ticket.getId(), requestedDuration);

        // 5. Run clip extraction synchronously for dev simplicity.
        //    In prod: push to a message queue (RabbitMQ/SQS) for async processing.
        try {
            ClipResult result = streamingProvider.extractClip(
                    film.getStorageKey(), watcherId,
                    req.startSeconds(), requestedDuration);

            clipJobRepository.updateResult(
                    job.getId(),
                    ClipJobEntity.ClipStatus.DONE,
                    result.clipStorageKey(),
                    result.clipUrl(),
                    null);

            log.info("Clip created: jobId={} watcherId={} filmId={}",
                    job.getId(), watcherId, filmId);

            return toClipJobResponse(job, ClipJobEntity.ClipStatus.DONE,
                    result.clipUrl(), null);

        } catch (Exception e) {
            log.error("Clip extraction failed: jobId={}", job.getId(), e);

            // Refund the clip seconds on failure
            ticketRepository.incrementClipSecondsUsed(ticket.getId(), -requestedDuration);

            clipJobRepository.updateResult(
                    job.getId(),
                    ClipJobEntity.ClipStatus.FAILED,
                    null, null, e.getMessage());

            throw new BusinessRuleException("Clip extraction failed: " + e.getMessage());
        }
    }

    // ── Get clip history ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ClipJobResponse> getClipHistory(UUID watcherId, UUID filmId) {
        ticketService.validateEntitlement(watcherId, filmId);

        TicketEntity ticket = ticketRepository.findActiveTicket(watcherId, filmId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", filmId));

        return clipJobRepository.findByTicketIdOrderByCreatedAtDesc(ticket.getId())
                .stream()
                .map(j -> toClipJobResponse(j, j.getStatus(), j.getClipUrl(), j.getErrorMessage()))
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ClipJobResponse toClipJobResponse(ClipJobEntity job,
                                              ClipJobEntity.ClipStatus status,
                                              String clipUrl, String error) {
        return new ClipJobResponse(
                job.getId().toString(),
                job.getFilm().getId().toString(),
                status.name(),
                clipUrl,
                job.getStartSeconds(),
                job.getDurationSeconds(),
                error,
                job.getCreatedAt()
        );
    }
}