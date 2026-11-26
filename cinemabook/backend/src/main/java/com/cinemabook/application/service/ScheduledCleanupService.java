package com.cinemabook.application.service;

import com.cinemabook.infrastructure.persistence.RefreshTokenCleanupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs background cleanup jobs on a schedule.
 *
 * Single Responsibility: this class only runs scheduled tasks.
 * Open/Closed: new cleanup jobs are new @Scheduled methods — never edit existing ones.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledCleanupService {

    private final RefreshTokenCleanupRepository refreshTokenCleanupRepository;

    /** Deletes expired refresh tokens. Runs every day at 03:00 UTC. */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredRefreshTokens() {
        log.info("Running: purgeExpiredRefreshTokens");
        refreshTokenCleanupRepository.deleteExpiredNow();
        log.info("Completed: purgeExpiredRefreshTokens");
    }

    /** Post auto-delete — wired in Phase 4. Runs every hour. */
    @Scheduled(cron = "0 0 * * * *")
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
