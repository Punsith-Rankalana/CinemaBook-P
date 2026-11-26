package com.cinemabook.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, UUID> {

    Optional<TicketEntity> findByWatcherIdAndFilmId(UUID watcherId, UUID filmId);

    boolean existsByWatcherIdAndFilmId(UUID watcherId, UUID filmId);

    Page<TicketEntity> findByWatcherIdOrderByPurchasedAtDesc(UUID watcherId, Pageable pageable);

    @Query("SELECT t FROM TicketEntity t WHERE t.watcher.id = :watcherId AND t.film.id = :filmId AND t.status = 'ACTIVE'")
    Optional<TicketEntity> findActiveTicket(@Param("watcherId") UUID watcherId,
                                            @Param("filmId") UUID filmId);

    @Modifying
    @Query("UPDATE TicketEntity t SET t.watchPositionSec = :pos, t.lastWatchedAt = :now WHERE t.id = :id")
    void updateWatchPosition(@Param("id") UUID id,
                             @Param("pos") int positionSec,
                             @Param("now") Instant now);

    @Modifying
    @Query("UPDATE TicketEntity t SET t.clipSecondsUsed = t.clipSecondsUsed + :seconds WHERE t.id = :id")
    void incrementClipSecondsUsed(@Param("id") UUID id, @Param("seconds") int seconds);

    @Modifying
    @Query("UPDATE TicketEntity t SET t.status = :status WHERE t.id = :id")
    void updateStatus(@Param("id") UUID id,
                      @Param("status") TicketEntity.TicketStatus status);

    /** Expire all tickets whose expires_at has passed and status is still ACTIVE. */
    @Modifying
    @Query("UPDATE TicketEntity t SET t.status = 'EXPIRED' WHERE t.expiresAt < :now AND t.status = 'ACTIVE'")
    int expireOverdueTickets(@Param("now") Instant now);
}