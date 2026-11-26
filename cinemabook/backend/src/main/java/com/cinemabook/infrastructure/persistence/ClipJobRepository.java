package com.cinemabook.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClipJobRepository extends JpaRepository<ClipJobEntity, UUID> {

    List<ClipJobEntity> findByWatcherIdOrderByCreatedAtDesc(UUID watcherId);

    List<ClipJobEntity> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);

    @Modifying
    @Query("""
        UPDATE ClipJobEntity c
        SET c.status = :status,
            c.clipStorageKey = :storageKey,
            c.clipUrl = :url,
            c.errorMessage = :error
        WHERE c.id = :id
    """)
    void updateResult(@Param("id") UUID id,
                      @Param("status") ClipJobEntity.ClipStatus status,
                      @Param("storageKey") String storageKey,
                      @Param("url") String url,
                      @Param("error") String error);
}