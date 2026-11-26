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
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Query("SELECT rt FROM RefreshTokenEntity rt WHERE rt.user.id = :userId AND rt.revokedAt IS NULL")
    List<RefreshTokenEntity> findActiveByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE RefreshTokenEntity rt SET rt.revokedAt = :now WHERE rt.user.id = :userId AND rt.revokedAt IS NULL")
    void revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.expiresAt < :cutoff")
    void deleteExpiredTokens(@Param("cutoff") Instant cutoff);
}
