-- ============================================================
-- CinemaBook — V6 Phase 4: Social Platform additions
-- ============================================================

-- Hashtags: extracted from post content for discoverability
CREATE TABLE hashtags (
                          id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                          tag         VARCHAR(100) NOT NULL UNIQUE,    -- e.g. "action", "horror"
                          post_count  BIGINT      NOT NULL DEFAULT 0,
                          created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_hashtags_tag ON hashtags (tag);


-- Post hashtags: many-to-many between posts and hashtags
CREATE TABLE post_hashtags (
                               post_id    UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                               hashtag_id UUID NOT NULL REFERENCES hashtags(id) ON DELETE CASCADE,
                               PRIMARY KEY (post_id, hashtag_id)
);

CREATE INDEX idx_post_hashtags_hashtag_id ON post_hashtags (hashtag_id);


-- Film comments: separate from post comments (tied directly to a film, not a post)
-- Flat structure for film reviews — simpler and better UX than nested
CREATE TABLE film_comments (
                               id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                               film_id     UUID        NOT NULL REFERENCES films(id) ON DELETE CASCADE,
                               author_id   UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               content     TEXT        NOT NULL CHECK (char_length(content) <= 2000),
                               likes_count BIGINT      NOT NULL DEFAULT 0,
                               created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_film_comments_film_id   ON film_comments (film_id, created_at DESC);
CREATE INDEX idx_film_comments_author_id ON film_comments (author_id);

CREATE TRIGGER trg_film_comments_updated_at
    BEFORE UPDATE ON film_comments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- Film comment likes
CREATE TABLE film_comment_likes (
                                    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                    comment_id UUID NOT NULL REFERENCES film_comments(id) ON DELETE CASCADE,
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    PRIMARY KEY (user_id, comment_id)
);


-- Post shares: tracks which posts a user shared (for share count)
CREATE TABLE post_shares (
                             id         UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                             post_id    UUID        NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                             user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             UNIQUE (post_id, user_id)
);

CREATE INDEX idx_post_shares_post_id ON post_shares (post_id);