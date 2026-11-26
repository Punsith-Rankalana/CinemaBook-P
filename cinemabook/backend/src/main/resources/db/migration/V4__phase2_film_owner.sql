-- ============================================================
-- CinemaBook — V4 Phase 2: Film Owner additions
-- ============================================================

-- Film upload jobs table: tracks background processing state
CREATE TABLE film_upload_jobs (
                                  id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                                  film_id         UUID        NOT NULL REFERENCES films(id) ON DELETE CASCADE,
                                  owner_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                  original_filename VARCHAR(500),
                                  file_size_bytes BIGINT,
                                  storage_key     VARCHAR(500),   -- raw uploaded file key in S3/R2
                                  job_status      VARCHAR(30)     NOT NULL DEFAULT 'PENDING'
                                      CHECK (job_status IN ('PENDING','UPLOADING','PROCESSING','DONE','FAILED')),
                                  error_message   TEXT,
                                  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_upload_jobs_film_id  ON film_upload_jobs (film_id);
CREATE INDEX idx_upload_jobs_owner_id ON film_upload_jobs (owner_id);
CREATE INDEX idx_upload_jobs_status   ON film_upload_jobs (job_status);

CREATE TRIGGER trg_upload_jobs_updated_at
    BEFORE UPDATE ON film_upload_jobs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


-- Film price history: immutable audit log of every price change (OCP — never delete)
CREATE TABLE film_price_history (
                                    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                                    film_id     UUID        NOT NULL REFERENCES films(id) ON DELETE CASCADE,
                                    old_price   NUMERIC(10,2),
                                    new_price   NUMERIC(10,2) NOT NULL,
                                    currency    VARCHAR(3)    NOT NULL DEFAULT 'USD',
                                    changed_by  UUID        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                                    changed_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_price_history_film_id ON film_price_history (film_id);


-- Stripe payout records
CREATE TABLE stripe_payouts (
                                id                  UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
                                owner_id            UUID        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                                stripe_payout_id    VARCHAR(255) UNIQUE,
                                amount              NUMERIC(12,2) NOT NULL,
                                currency            VARCHAR(3)    NOT NULL DEFAULT 'USD',
                                status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                                    CHECK (status IN ('PENDING','PAID','FAILED','CANCELLED')),
                                failure_reason      TEXT,
                                initiated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                completed_at        TIMESTAMPTZ
);

CREATE INDEX idx_payouts_owner_id ON stripe_payouts (owner_id);
CREATE INDEX idx_payouts_status   ON stripe_payouts (status);