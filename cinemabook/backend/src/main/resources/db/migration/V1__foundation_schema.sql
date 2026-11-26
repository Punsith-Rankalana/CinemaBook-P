-- ============================================================
-- CinemaBook — V1 Foundation Schema
-- Phase 1: Users, Auth, Core Domain Tables
-- ============================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";


-- ============================================================
-- USER DOMAIN
-- ============================================================

CREATE TABLE users (
                       id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
                       email           VARCHAR(255)    NOT NULL UNIQUE,
                       username        VARCHAR(50)     NOT NULL UNIQUE,
                       password_hash   VARCHAR(255)    NOT NULL,
                       user_type       VARCHAR(20)     NOT NULL CHECK (user_type IN ('WATCHER', 'FILM_OWNER')),
                       account_status  VARCHAR(30)     NOT NULL DEFAULT 'PENDING_VERIFICATION'
                           CHECK (account_status IN ('PENDING_VERIFICATION','ACTIVE','SUSPENDED','DEACTIVATED')),
                       display_name    VARCHAR(100),
                       bio             TEXT,
                       avatar_config   JSONB,
                       avatar_url      VARCHAR(500),
                       created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                       updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                       last_login_at   TIMESTAMPTZ,
                       email_verified  BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_users_email    ON users (email);
CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_type     ON users (user_type);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- Email verification tokens
CREATE TABLE email_verification_tokens (
                                           id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                                           user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                           token       VARCHAR(255) NOT NULL UNIQUE,
                                           expires_at  TIMESTAMPTZ NOT NULL,
                                           used_at     TIMESTAMPTZ,
                                           created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_evt_token   ON email_verification_tokens (token);
CREATE INDEX idx_evt_user_id ON email_verification_tokens (user_id);


-- Refresh tokens
CREATE TABLE refresh_tokens (
                                id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                                user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                token_hash  VARCHAR(255) NOT NULL UNIQUE,
                                device_id   VARCHAR(255),
                                device_info VARCHAR(500),
                                ip_address  VARCHAR(45),
                                expires_at  TIMESTAMPTZ NOT NULL,
                                revoked_at  TIMESTAMPTZ,
                                created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rt_user_id    ON refresh_tokens (user_id);
CREATE INDEX idx_rt_token_hash ON refresh_tokens (token_hash);


-- Active device sessions (single-device enforcement)
CREATE TABLE active_sessions (
                                 id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                                 user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 device_id       VARCHAR(255) NOT NULL,
                                 refresh_token_id UUID       REFERENCES refresh_tokens(id) ON DELETE SET NULL,
                                 started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 last_active_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                 UNIQUE (user_id)
);

CREATE INDEX idx_as_user_id ON active_sessions (user_id);


-- ============================================================
-- WATCHER PROFILE
-- ============================================================

CREATE TABLE watcher_profiles (
                                  user_id             UUID    PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                                  clip_seconds_used   INT     NOT NULL DEFAULT 0,
                                  preferred_genres    TEXT[],
                                  created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_watcher_profiles_updated_at
    BEFORE UPDATE ON watcher_profiles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- FILM OWNER PROFILE
-- ============================================================

CREATE TABLE film_owner_profiles (
                                     user_id             UUID        PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                                     company_name        VARCHAR(200),
                                     stripe_account_id   VARCHAR(255),
                                     bank_account_linked BOOLEAN     NOT NULL DEFAULT FALSE,
                                     payout_enabled      BOOLEAN     NOT NULL DEFAULT FALSE,
                                     total_earnings      NUMERIC(12,2) NOT NULL DEFAULT 0,
                                     created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_film_owner_profiles_updated_at
    BEFORE UPDATE ON film_owner_profiles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- ============================================================
-- FILM DOMAIN
-- ============================================================

CREATE TABLE films (
                       id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
                       owner_id        UUID            NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                       title           VARCHAR(255)    NOT NULL,
                       description     TEXT,
                       genre           VARCHAR(100),
                       tags            TEXT[],
                       duration_seconds INT,
                       ticket_price    NUMERIC(10,2)   NOT NULL,
                       currency        VARCHAR(3)      NOT NULL DEFAULT 'USD',
                       status          VARCHAR(20)     NOT NULL DEFAULT 'PROCESSING'
                           CHECK (status IN ('PROCESSING','READY','PUBLISHED','UNPUBLISHED','REJECTED')),
                       storage_key     VARCHAR(500),
                       thumbnail_url   VARCHAR(500),
                       trailer_url     VARCHAR(500),
                       release_year    INT,
                       language        VARCHAR(50),
                       age_rating      VARCHAR(10),
                       total_tickets_sold BIGINT       NOT NULL DEFAULT 0,
                       total_revenue   NUMERIC(12,2)   NOT NULL DEFAULT 0,
                       created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                       updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_films_owner_id ON films (owner_id);
CREATE INDEX idx_films_status   ON films (status);
CREATE INDEX idx_films_genre    ON films (genre);
CREATE INDEX idx_films_title    ON films USING GIN (title gin_trgm_ops);

CREATE TRIGGER trg_films_updated_at
    BEFORE UPDATE ON films
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- Tickets
CREATE TABLE tickets (
                         id                  UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
                         watcher_id          UUID            NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                         film_id             UUID            NOT NULL REFERENCES films(id) ON DELETE RESTRICT,
                         status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE'
                             CHECK (status IN ('ACTIVE','USED','EXPIRED','REFUNDED')),
                         price_paid          NUMERIC(10,2)   NOT NULL,
                         currency            VARCHAR(3)      NOT NULL DEFAULT 'USD',
                         clip_seconds_used   INT             NOT NULL DEFAULT 0,
                         clip_seconds_limit  INT             NOT NULL DEFAULT 300,
                         purchased_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                         expires_at          TIMESTAMPTZ,
                         last_watched_at     TIMESTAMPTZ,
                         watch_position_sec  INT             NOT NULL DEFAULT 0,
                         UNIQUE (watcher_id, film_id)
);

CREATE INDEX idx_tickets_watcher_id ON tickets (watcher_id);
CREATE INDEX idx_tickets_film_id    ON tickets (film_id);
CREATE INDEX idx_tickets_status     ON tickets (status);


-- ============================================================
-- SOCIAL DOMAIN
-- ============================================================

CREATE TABLE posts (
                       id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                       author_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                       post_type       VARCHAR(20) NOT NULL DEFAULT 'TEXT'
                           CHECK (post_type IN ('TEXT','IMAGE','CLIP','AVATAR_CARD')),
                       content         TEXT,
                       media_url       VARCHAR(500),
                       film_id         UUID        REFERENCES films(id) ON DELETE SET NULL,
                       likes_count     BIGINT      NOT NULL DEFAULT 0,
                       comments_count  BIGINT      NOT NULL DEFAULT 0,
                       auto_delete_at  TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '5 days'),
                       created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_posts_author_id   ON posts (author_id);
CREATE INDEX idx_posts_auto_delete ON posts (auto_delete_at);
CREATE INDEX idx_posts_film_id     ON posts (film_id);
CREATE INDEX idx_posts_created_at  ON posts (created_at DESC);

CREATE TRIGGER trg_posts_updated_at
    BEFORE UPDATE ON posts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- Comments
CREATE TABLE comments (
                          id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                          post_id     UUID        NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                          author_id   UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          parent_id   UUID        REFERENCES comments(id) ON DELETE CASCADE,
                          content     TEXT        NOT NULL,
                          likes_count BIGINT      NOT NULL DEFAULT 0,
                          created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                          updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_post_id   ON comments (post_id);
CREATE INDEX idx_comments_author_id ON comments (author_id);

CREATE TRIGGER trg_comments_updated_at
    BEFORE UPDATE ON comments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- Likes
CREATE TABLE likes (
                       id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                       user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                       post_id     UUID        REFERENCES posts(id) ON DELETE CASCADE,
                       comment_id  UUID        REFERENCES comments(id) ON DELETE CASCADE,
                       created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       CONSTRAINT chk_like_target CHECK (
                           (post_id IS NOT NULL AND comment_id IS NULL) OR
                           (post_id IS NULL AND comment_id IS NOT NULL)
                           ),
                       UNIQUE (user_id, post_id),
                       UNIQUE (user_id, comment_id)
);


-- Follows
CREATE TABLE follows (
                         follower_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         followee_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                         PRIMARY KEY (follower_id, followee_id),
                         CONSTRAINT chk_no_self_follow CHECK (follower_id != followee_id)
    );

CREATE INDEX idx_follows_follower ON follows (follower_id);
CREATE INDEX idx_follows_followee ON follows (followee_id);


-- ============================================================
-- MESSAGING DOMAIN
-- ============================================================

CREATE TABLE conversations (
                               id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                               name        VARCHAR(200),
                               is_group    BOOLEAN     NOT NULL DEFAULT FALSE,
                               created_by  UUID        REFERENCES users(id) ON DELETE SET NULL,
                               created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_conversations_updated_at
    BEFORE UPDATE ON conversations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE conversation_members (
                                      conversation_id UUID    NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
                                      user_id         UUID    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                      joined_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                      last_read_at    TIMESTAMPTZ,
                                      is_admin        BOOLEAN NOT NULL DEFAULT FALSE,
                                      PRIMARY KEY (conversation_id, user_id)
);

CREATE INDEX idx_cm_user_id ON conversation_members (user_id);


CREATE TABLE messages (
                          id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
                          conversation_id UUID            NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
                          sender_id       UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          message_type    VARCHAR(20)     NOT NULL DEFAULT 'TEXT'
                              CHECK (message_type IN ('TEXT','IMAGE','CLIP','SYSTEM')),
                          content         TEXT,
                          media_url       VARCHAR(500),
                          media_expires_at TIMESTAMPTZ,
                          reply_to_id     UUID            REFERENCES messages(id) ON DELETE SET NULL,
                          is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
                          created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_conversation_id ON messages (conversation_id, created_at DESC);
CREATE INDEX idx_messages_sender_id       ON messages (sender_id);
CREATE INDEX idx_messages_media_expires   ON messages (media_expires_at)
    WHERE media_expires_at IS NOT NULL;


-- ============================================================
-- PAYMENT DOMAIN
-- ============================================================

CREATE TABLE payment_transactions (
                                      id                  UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
                                      watcher_id          UUID            NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                                      film_id             UUID            NOT NULL REFERENCES films(id) ON DELETE RESTRICT,
                                      ticket_id           UUID            REFERENCES tickets(id) ON DELETE SET NULL,
                                      stripe_payment_id   VARCHAR(255),
                                      amount              NUMERIC(10,2)   NOT NULL,
                                      currency            VARCHAR(3)      NOT NULL DEFAULT 'USD',
                                      status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                                          CHECK (status IN ('PENDING','COMPLETED','FAILED','REFUNDED')),
                                      failure_reason      TEXT,
                                      created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
                                      updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pt_watcher_id ON payment_transactions (watcher_id);
CREATE INDEX idx_pt_film_id    ON payment_transactions (film_id);
CREATE INDEX idx_pt_status     ON payment_transactions (status);

CREATE TRIGGER trg_pt_updated_at
    BEFORE UPDATE ON payment_transactions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
