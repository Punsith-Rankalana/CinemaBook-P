package com.cinemabook.infrastructure.persistence;

import com.cinemabook.domain.user.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM UserEntity u WHERE u.email = :login OR u.username = :login")
    Optional<UserEntity> findByEmailOrUsername(@Param("login") String login);

    @Modifying
    @Query("UPDATE UserEntity u SET u.lastLoginAt = :time WHERE u.id = :id")
    void updateLastLogin(@Param("id") UUID id, @Param("time") Instant time);

    @Modifying
    @Query("UPDATE UserEntity u SET u.accountStatus = :status WHERE u.id = :id")
    void updateAccountStatus(@Param("id") UUID id,
                             @Param("status") AccountStatus status);
}
