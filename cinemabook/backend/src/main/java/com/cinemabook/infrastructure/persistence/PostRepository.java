package com.cinemabook.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, UUID> {

    /** Feed: posts from users the given user follows, not yet expired */
    @Query("""
        SELECT p FROM PostEntity p
        WHERE p.author.id IN (
            SELECT f.followee.id FROM FollowEntity f WHERE f.follower.id = :userId
        )
        AND p.autoDeleteAt > :now
        ORDER BY p.createdAt DESC
    """)
    Page<PostEntity> findFeedForUser(@Param("userId") UUID userId,
                                     @Param("now") Instant now,
                                     Pageable pageable);

    /** Posts by a specific author, not yet expired */
    @Query("""
        SELECT p FROM PostEntity p
        WHERE p.author.id = :authorId
        AND p.autoDeleteAt > :now
        ORDER BY p.createdAt DESC
    """)
    Page<PostEntity> findByAuthorNotExpired(@Param("authorId") UUID authorId,
                                            @Param("now") Instant now,
                                            Pageable pageable);

    /** Posts tagged with a specific film */
    @Query("""
        SELECT p FROM PostEntity p
        WHERE p.film.id = :filmId
        AND p.autoDeleteAt > :now
        ORDER BY p.createdAt DESC
    """)
    Page<PostEntity> findByFilm(@Param("filmId") UUID filmId,
                                @Param("now") Instant now,
                                Pageable pageable);

    /** Posts tagged with a specific hashtag */
    @Query("""
        SELECT p FROM PostEntity p
        JOIN p.hashtags h
        WHERE h.tag = :tag
        AND p.autoDeleteAt > :now
        ORDER BY p.createdAt DESC
    """)
    Page<PostEntity> findByHashtag(@Param("tag") String tag,
                                   @Param("now") Instant now,
                                   Pageable pageable);

    /** Cleanup: delete all expired posts */
    @Modifying
    @Query("DELETE FROM PostEntity p WHERE p.autoDeleteAt < :now")
    int deleteExpiredPosts(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE PostEntity p SET p.likesCount = p.likesCount + :delta WHERE p.id = :id")
    void adjustLikesCount(@Param("id") UUID id, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE PostEntity p SET p.commentsCount = p.commentsCount + :delta WHERE p.id = :id")
    void adjustCommentsCount(@Param("id") UUID id, @Param("delta") int delta);
}