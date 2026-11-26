package com.cinemabook.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StreamingSessionRepository extends JpaRepository<StreamingSessionEntity, UUID> {

    Optional<StreamingSessionEntity> findBySessionToken(String sessionToken);

    @Query("SELECT s FROM StreamingSessionEntity s WHERE s.watcher.id = :watcherId AND s.film.id = :filmId AND s.revokedAt IS NULL AND s.expiresAt > :now")
    Optional<StreamingSessionEntity> findActiveSession(
            @Param("watcherId") UUID watcherId,
            @Param("filmId") UUID filmId,
            @Param("now") Instant now);

    @Modifying
    @Query("UPDATE StreamingSessionEntity s SET s.revokedAt = :now WHERE s.watcher.id = :watcherId AND s.revokedAt IS NULL")
    void revokeAllForWatcher(@Param("watcherId") UUID watcherId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE StreamingSessionEntity s SET s.revokedAt = :now WHERE s.ticket.id = :ticketId AND s.revokedAt IS NULL")
    void revokeAllForTicket(@Param("ticketId") UUID ticketId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM StreamingSessionEntity s WHERE s.expiresAt < :cutoff")
    void deleteExpiredSessions(@Param("cutoff") Instant cutoff);
}