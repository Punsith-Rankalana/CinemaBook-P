package com.cinemabook.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, UUID> {

    Optional<LikeEntity> findByUserIdAndPostId(UUID userId, UUID postId);

    Optional<LikeEntity> findByUserIdAndCommentId(UUID userId, UUID commentId);

    boolean existsByUserIdAndPostId(UUID userId, UUID postId);

    boolean existsByUserIdAndCommentId(UUID userId, UUID commentId);

    @Modifying
    @Query("DELETE FROM LikeEntity l WHERE l.user.id = :userId AND l.post.id = :postId")
    void deleteByUserIdAndPostId(@Param("userId") UUID userId, @Param("postId") UUID postId);

    @Modifying
    @Query("DELETE FROM LikeEntity l WHERE l.user.id = :userId AND l.comment.id = :commentId")
    void deleteByUserIdAndCommentId(@Param("userId") UUID userId, @Param("commentId") UUID commentId);
}