package com.cinemabook.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_ticket_watcher_film",
                columnNames = {"watcher_id", "film_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "watcher_id", nullable = false)
    private UserEntity watcher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "film_id", nullable = false)
    private FilmEntity film;

    /**
     * Stored as VARCHAR(20) in the database with a CHECK constraint.
     * @Enumerated(EnumType.STRING) maps cleanly — no native PG enum needed.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.ACTIVE;

    @Column(name = "price_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePaid;

    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "clip_seconds_used", nullable = false)
    @Builder.Default
    private int clipSecondsUsed = 0;

    @Column(name = "clip_seconds_limit", nullable = false)
    @Builder.Default
    private int clipSecondsLimit = 300;

    @Column(name = "purchased_at", updatable = false, nullable = false)
    private Instant purchasedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_watched_at")
    private Instant lastWatchedAt;

    @Column(name = "watch_position_sec", nullable = false)
    @Builder.Default
    private int watchPositionSec = 0;

    @PrePersist
    void onCreate() {
        purchasedAt = Instant.now();
    }

    public boolean hasClipSecondsRemaining() {
        return clipSecondsUsed < clipSecondsLimit;
    }

    public int clipSecondsRemaining() {
        return Math.max(0, clipSecondsLimit - clipSecondsUsed);
    }

    public enum TicketStatus {
        ACTIVE, USED, EXPIRED, REFUNDED
    }
}