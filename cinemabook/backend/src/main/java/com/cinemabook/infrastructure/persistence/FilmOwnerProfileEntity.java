package com.cinemabook.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "film_owner_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilmOwnerProfileEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "stripe_account_id", length = 255)
    private String stripeAccountId;

    @Column(name = "bank_account_linked", nullable = false)
    @Builder.Default
    private boolean bankAccountLinked = false;

    @Column(name = "payout_enabled", nullable = false)
    @Builder.Default
    private boolean payoutEnabled = false;

    @Column(name = "total_earnings", nullable = false,
            precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalEarnings = BigDecimal.ZERO;

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
