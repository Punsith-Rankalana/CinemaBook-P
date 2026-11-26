package com.cinemabook.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "films")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilmEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String genre;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] tags;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "ticket_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal ticketPrice;

    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "USD";

    /**
     * Stored as VARCHAR(20) in the database with a CHECK constraint.
     * @Enumerated(EnumType.STRING) maps cleanly — no native PG enum needed.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FilmStatus status = FilmStatus.PROCESSING;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "trailer_url", length = 500)
    private String trailerUrl;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(length = 50)
    private String language;

    @Column(name = "age_rating", length = 10)
    private String ageRating;

    @Column(name = "total_tickets_sold", nullable = false)
    @Builder.Default
    private long totalTicketsSold = 0L;

    @Column(name = "total_revenue", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalRevenue = BigDecimal.ZERO;

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

    public enum FilmStatus {
        PROCESSING, READY, PUBLISHED, UNPUBLISHED, REJECTED
    }
}