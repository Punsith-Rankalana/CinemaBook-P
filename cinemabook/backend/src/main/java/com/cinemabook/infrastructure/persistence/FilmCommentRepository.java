package com.cinemabook.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FilmCommentRepository extends JpaRepository<FilmCommentEntity, UUID> {

    Page<FilmCommentEntity> findByFilmIdOrderByCreatedAtDesc(UUID filmId, Pageable pageable);

    long countByFilmId(UUID filmId);

    boolean existsByIdAndAuthorId(UUID id, UUID authorId);

    @Modifying
    @Query("UPDATE FilmCommentEntity c SET c.likesCount = c.likesCount + :delta WHERE c.id = :id")
    void adjustLikesCount(@Param("id") UUID id, @Param("delta") int delta);
}