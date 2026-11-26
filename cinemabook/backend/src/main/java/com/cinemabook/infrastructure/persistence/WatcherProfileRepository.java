package com.cinemabook.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WatcherProfileRepository extends JpaRepository<WatcherProfileEntity, UUID> {

    @Modifying
    @Query("UPDATE WatcherProfileEntity w SET w.clipSecondsUsed = w.clipSecondsUsed + :seconds WHERE w.userId = :id")
    void incrementClipSecondsUsed(@Param("id") UUID userId,
                                  @Param("seconds") int seconds);
}
