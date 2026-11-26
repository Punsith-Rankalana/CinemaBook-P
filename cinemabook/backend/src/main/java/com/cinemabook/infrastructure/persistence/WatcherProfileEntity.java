package com.cinemabook.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "watcher_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatcherProfileEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "clip_seconds_used", nullable = false)
    @Builder.Default
    private int clipSecondsUsed = 0;

    /**
     * Maps to PostgreSQL text[].
     * Hibernate 6 handles native array types via @JdbcTypeCode(SqlTypes.ARRAY).
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "preferred_genres", columnDefinition = "text[]")
    private String[] preferredGenres;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
