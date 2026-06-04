-- @path services/recording-service/src/main/resources/db/migration/V1__create_recording_tables.sql
-- @owner recording-service
-- @responsibility Cria tabelas do schema recordings: recordings, outbox_events
-- @see docs/DATA_MODEL.md#recordings | docs/ARCHITECTURE.md#recording-service (ADR-007)

-- ─── RECORDINGS ─────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS recordings.recordings (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    camera_id        UUID          NOT NULL,
    tenant_id        UUID          NOT NULL,
    started_at       TIMESTAMPTZ   NOT NULL,
    ended_at         TIMESTAMPTZ,
    r2_key           TEXT          NOT NULL,
    duration_seconds INTEGER,
    size_bytes       BIGINT,
    has_motion       BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Índice crítico para queries de timeline — ADR-007
-- camera_id + started_at DESC + ended_at: permite index scan mesmo para câmeras com anos de gravação
CREATE INDEX IF NOT EXISTS idx_recordings_camera_time ON recordings.recordings (camera_id, started_at DESC, ended_at);
CREATE INDEX IF NOT EXISTS idx_recordings_tenant       ON recordings.recordings (tenant_id, started_at DESC);
-- Usado pelo job de retenção para deletar gravações expiradas
CREATE INDEX IF NOT EXISTS idx_recordings_cleanup      ON recordings.recordings (tenant_id, ended_at)
    WHERE ended_at IS NOT NULL;

ALTER TABLE recordings.recordings ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_recordings ON recordings.recordings
    AS PERMISSIVE FOR ALL
    TO recording_service_user
    USING (tenant_id = current_setting('app.current_tenant_id', TRUE)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', TRUE)::UUID);

-- ─── OUTBOX_EVENTS ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS recordings.outbox_events (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    topic       VARCHAR(100)  NOT NULL,
    event_type  VARCHAR(100)  NOT NULL,
    payload     JSONB         NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    sent_at     TIMESTAMPTZ,
    attempts    INTEGER       NOT NULL DEFAULT 0,
    last_error  TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_unsent ON recordings.outbox_events (created_at)
    WHERE sent_at IS NULL;
