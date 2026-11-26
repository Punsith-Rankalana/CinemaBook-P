-- ============================================================
-- CinemaBook — V5 Phase 3: Watcher / Streaming additions
-- ============================================================

-- Streaming sessions: short-lived signed URL tokens per watcher+ticket
CREATE TABLE streaming_sessions (
                                    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                                    ticket_id       UUID        NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
                                    watcher_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                    film_id         UUID        NOT NULL REFERENCES films(id) ON DELETE CASCADE,
                                    session_token   VARCHAR(500) NOT NULL UNIQUE,   -- signed JWT for stream auth
                                    stream_url      VARCHAR(1000),                  -- signed CDN URL
                                    expires_at      TIMESTAMPTZ NOT NULL,
                                    revoked_at      TIMESTAMPTZ,
                                    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stream_sessions_ticket_id     ON streaming_sessions (ticket_id);
CREATE INDEX idx_stream_sessions_watcher_id    ON streaming_sessions (watcher_id);
CREATE INDEX idx_stream_sessions_session_token ON streaming_sessions (session_token);
CREATE INDEX idx_stream_sessions_expires       ON streaming_sessions (expires_at)
    WHERE revoked_at IS NULL;


-- Clip extraction jobs: tracks server-side FFmpeg clip jobs
CREATE TABLE clip_jobs (
                           id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                           ticket_id       UUID        NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
                           watcher_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           film_id         UUID        NOT NULL REFERENCES films(id) ON DELETE CASCADE,
                           start_seconds   INT         NOT NULL,
                           duration_seconds INT        NOT NULL CHECK (duration_seconds <= 300),
                           clip_storage_key VARCHAR(500),
                           clip_url        VARCHAR(1000),
                           watermark_text  VARCHAR(255),   -- e.g. "CinemaBook | userId | timestamp"
                           status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                               CHECK (status IN ('PENDING','PROCESSING','DONE','FAILED')),
                           error_message   TEXT,
                           created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                           updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_clip_jobs_ticket_id  ON clip_jobs (ticket_id);
CREATE INDEX idx_clip_jobs_watcher_id ON clip_jobs (watcher_id);
CREATE INDEX idx_clip_jobs_status     ON clip_jobs (status);

CREATE TRIGGER trg_clip_jobs_updated_at
    BEFORE UPDATE ON clip_jobs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- Watch progress: tracks per-ticket resume position (upsert-friendly)
-- Already on the tickets table as watch_position_sec, but a separate table
-- allows multiple watch sessions and better analytics later.
CREATE TABLE watch_events (
                              id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                              ticket_id       UUID        NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
                              watcher_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                              position_sec    INT         NOT NULL DEFAULT 0,
                              event_type      VARCHAR(20) NOT NULL DEFAULT 'PROGRESS'
                                  CHECK (event_type IN ('START','PROGRESS','PAUSE','COMPLETE')),
                              recorded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_watch_events_ticket_id ON watch_events (ticket_id);