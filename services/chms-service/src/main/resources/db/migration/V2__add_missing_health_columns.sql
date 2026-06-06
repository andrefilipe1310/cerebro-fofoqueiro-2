-- camera_health_state no 03_tables.sql usa camera_id como PK (sem id separado)
-- V1 Flyway tentou criar com id UUID PK mas tabela já existe — adicionar colunas faltantes

ALTER TABLE health.camera_health_state
    ADD COLUMN IF NOT EXISTS consecutive_failures INTEGER NOT NULL DEFAULT 0;

-- health_events foi criada pelo V1 (não existe em 03_tables.sql) — criar se não existir
CREATE TABLE IF NOT EXISTS health.health_events (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    camera_id   UUID          NOT NULL,
    tenant_id   UUID          NOT NULL,
    type        VARCHAR(50)   NOT NULL,
    severity    VARCHAR(50)   NOT NULL DEFAULT 'WARNING',
    detected_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    metadata    JSONB
);

CREATE INDEX IF NOT EXISTS idx_health_events_camera ON health.health_events (camera_id, detected_at DESC);
CREATE INDEX IF NOT EXISTS idx_health_events_tenant ON health.health_events (tenant_id, type, detected_at DESC);
