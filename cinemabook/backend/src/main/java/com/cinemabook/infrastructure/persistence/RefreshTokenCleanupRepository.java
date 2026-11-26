package com.cinemabook.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Thin repository used only by ScheduledCleanupService.
 * Kept separate so the scheduler has a focused, minimal dependency.
 */
@Repository
public interface RefreshTokenCleanupRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpiredNow();
}
