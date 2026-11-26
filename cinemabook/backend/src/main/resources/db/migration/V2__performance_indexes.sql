-- ============================================================
-- CinemaBook — V2 Performance Indexes & Constraints
-- Phase 1 supplement: additional indexes for query performance
-- ============================================================

-- Full-text search prep for films (used in Phase 2+)
CREATE INDEX IF NOT EXISTS idx_films_tags
    ON films USING GIN (tags);

-- Fast lookup: all films by status + genre (browse page)
-- Partial index on constant value 'PUBLISHED' is allowed.
CREATE INDEX IF NOT EXISTS idx_films_status_genre
    ON films (status, genre)
    WHERE status = 'PUBLISHED';

-- Ticket lookup: watcher's active tickets (watch page)
-- Partial index on constant value 'ACTIVE' is allowed.
CREATE INDEX IF NOT EXISTS idx_tickets_watcher_active
    ON tickets (watcher_id, status)
    WHERE status = 'ACTIVE';

-- Posts feed: posts by author ordered by time (social feed)
-- IMPORTANT: PostgreSQL does NOT allow NOW() in a partial index predicate
-- because NOW() is non-immutable (changes every call) and the index
-- condition must be fixed at creation time.
-- Fix: include auto_delete_at as an index COLUMN instead.
-- Queries of the form:
--   WHERE author_id = ? AND auto_delete_at > NOW() ORDER BY created_at DESC
-- will use this composite index — the planner evaluates auto_delete_at > NOW()
-- at runtime against the stored column values.
CREATE INDEX IF NOT EXISTS idx_posts_author_created
    ON posts (author_id, auto_delete_at, created_at DESC);

-- Conversation inbox: user's conversations sorted by activity
CREATE INDEX IF NOT EXISTS idx_cm_conversation_last
    ON conversation_members (user_id, last_read_at DESC NULLS LAST);

-- Messages page load: latest N messages in a conversation
-- Partial index on constant FALSE is allowed (immutable).
CREATE INDEX IF NOT EXISTS idx_messages_conv_created
    ON messages (conversation_id, created_at DESC)
    WHERE is_deleted = FALSE;

-- Payment audit: all transactions for a film
CREATE INDEX IF NOT EXISTS idx_pt_film_created
    ON payment_transactions (film_id, created_at DESC);
