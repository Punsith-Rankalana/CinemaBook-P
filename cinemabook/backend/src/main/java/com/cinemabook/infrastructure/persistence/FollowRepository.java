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
public interface FollowRepository extends JpaRepository<FollowEntity, FollowId> {

    boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

    @Modifying
    @Query("DELETE FROM FollowEntity f WHERE f.follower.id = :followerId AND f.followee.id = :followeeId")
    void deleteByFollowerIdAndFolloweeId(@Param("followerId") UUID followerId,
                                         @Param("followeeId") UUID followeeId);

    @Query("SELECT f.followee FROM FollowEntity f WHERE f.follower.id = :userId ORDER BY f.createdAt DESC")
    Page<UserEntity> findFollowing(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT f.follower FROM FollowEntity f WHERE f.followee.id = :userId ORDER BY f.createdAt DESC")
    Page<UserEntity> findFollowers(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(f) FROM FollowEntity f WHERE f.follower.id = :userId")
    long countFollowing(@Param("userId") UUID userId);

    @Query("SELECT COUNT(f) FROM FollowEntity f WHERE f.followee.id = :userId")
    long countFollowers(@Param("userId") UUID userId);
}