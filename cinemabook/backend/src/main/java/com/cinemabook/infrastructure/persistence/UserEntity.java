package com.cinemabook.infrastructure.persistence;

import com.cinemabook.domain.user.AccountStatus;
import com.cinemabook.domain.user.UserAccount;
import com.cinemabook.domain.user.UserType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity for the `users` table.
 *
 * Enum columns (user_type, account_status) are stored as VARCHAR in PostgreSQL
 * with CHECK constraints — NOT as native PG enum types. This avoids the
 * Hibernate 6 schema-validation conflict between Types#VARCHAR and NAMED_ENUM.
 * @Enumerated(EnumType.STRING) alone is sufficient and correct.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity implements UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 20)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 30)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.PENDING_VERIFICATION;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "avatar_config", columnDefinition = "jsonb")
    private Map<String, Object> avatarConfig;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, optional = true)
    private WatcherProfileEntity watcherProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, optional = true)
    private FilmOwnerProfileEntity filmOwnerProfile;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
