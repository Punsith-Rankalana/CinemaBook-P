package com.cinemabook.application.service;

import com.cinemabook.infrastructure.persistence.StreamingSessionRepository;
import com.cinemabook.infrastructure.persistence.TicketRepository;
import com.cinemabook.infrastructure.persistence.RefreshTokenCleanupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Single Responsibility: background cleanup jobs only.
 * Open/Closed: new cleanup jobs are new @Scheduled methods — never edit existing ones.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledCleanupService {

    private final RefreshTokenCleanupRepository refreshTokenCleanupRepository;
    private final StreamingSessionRepository    sessionRepository;
    private final TicketRepository              ticketRepository;

    /** Delete expired refresh tokens. Runs daily at 03:00 UTC. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        log.info("Running: purgeExpiredRefreshTokens");
        refreshTokenCleanupRepository.deleteExpiredNow();
    }

    /** Delete expired streaming sessions. Runs every hour. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpiredStreamingSessions() {
        log.info("Running: purgeExpiredStreamingSessions");
        // Keep a 1-hour grace period before hard deletion
        sessionRepository.deleteExpiredSessions(
                Instant.now().minusSeconds(3600));
    }

    /** Expire tickets whose expires_at has passed. Runs every 30 minutes. */
    @Scheduled(fixedRate = 30 * 60 * 1000)
    @Transactional
    public void expireOverdueTickets() {
        int expired = ticketRepository.expireOverdueTickets(Instant.now());
        if (expired > 0) {
            log.info("Expired {} overdue tickets", expired);
        }
    }

    /** Post auto-delete — wired in Phase 4. Runs every hour. */
    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    public void purgeExpiredPosts() {
        // TODO Phase 4: postRepository.deleteExpiredPosts(Instant.now());
        log.debug("purgeExpiredPosts: wired in Phase 4");
    }

    /** Ephemeral message media cleanup — wired in Phase 5. Runs every 15 min. */
    @Scheduled(fixedRate = 15 * 60 * 1000)
    @Transactional
    public void purgeExpiredMessageMedia() {
        // TODO Phase 5: messageRepository.deleteExpiredMedia(Instant.now());
        log.debug("purgeExpiredMessageMedia: wired in Phase 5");
    }
}