-- V2: adiciona colunas faltantes e cria health_events se não existir

ALTER TABLE health.camera_health_state
    ADD COLUMN IF NOT EXISTS consecutive_failures INTEGER NOT NULL DEFAULT 0;

-- health_events pode já existir via 03_tables.sql (idempotente)
CREATE TABLE IF NOT EXISTS health.health_events (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    camera_id   UUID          NOT NULL,
    org_id      UUID          NOT NULL,
    type        VARCHAR(50)   NOT NULL,
    severity    VARCHAR(50)   NOT NULL DEFAULT 'WARNING',
    detected_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    metadata    JSONB
);

CREATE INDEX IF NOT EXISTS idx_health_events_camera ON health.health_events (camera_id, detected_at DESC);
