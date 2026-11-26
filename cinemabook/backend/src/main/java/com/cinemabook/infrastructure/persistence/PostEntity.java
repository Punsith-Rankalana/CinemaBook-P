package com.cinemabook.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false, length = 20)
    @Builder.Default
    private PostType postType = PostType.TEXT;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "media_url", length = 500)
    private String mediaUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "film_id")
    private FilmEntity film;

    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    private long likesCount = 0L;

    @Column(name = "comments_count", nullable = false)
    @Builder.Default
    private long commentsCount = 0L;

    @Column(name = "auto_delete_at", nullable = false)
    private Instant autoDeleteAt;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "post_hashtags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "hashtag_id")
    )
    @Builder.Default
    private Set<HashtagEntity> hashtags = new HashSet<>();

    @PrePersist
    void onCreate() {
        createdAt    = Instant.now();
        updatedAt    = Instant.now();
        autoDeleteAt = Instant.now().plusSeconds(5 * 24 * 3600); // 5 days
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum PostType {
        TEXT, IMAGE, CLIP, AVATAR_CARD
    }
}