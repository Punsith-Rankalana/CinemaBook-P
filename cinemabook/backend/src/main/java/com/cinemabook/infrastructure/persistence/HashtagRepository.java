package com.cinemabook.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HashtagRepository extends JpaRepository<HashtagEntity, UUID> {

    Optional<HashtagEntity> findByTag(String tag);

    List<HashtagEntity> findByTagStartingWithOrderByPostCountDesc(String prefix, Pageable pageable);

    @Query("SELECT h FROM HashtagEntity h ORDER BY h.postCount DESC")
    List<HashtagEntity> findTrending(Pageable pageable);

    @Modifying
    @Query("UPDATE HashtagEntity h SET h.postCount = h.postCount + :delta WHERE h.id = :id")
    void adjustPostCount(@Param("id") UUID id, @Param("delta") int delta);
}