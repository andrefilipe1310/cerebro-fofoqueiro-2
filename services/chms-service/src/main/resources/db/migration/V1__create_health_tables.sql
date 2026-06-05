-- @path services/chms-service/src/main/resources/db/migration/V1__create_health_tables.sql
-- @owner chms-service
-- @responsibility Cria tabelas do schema health: camera_health_state, health_events, outbox_events
-- @see docs/DATA_MODEL.md#health_events | docs/SDD.md#design-chms

CREATE SCHEMA IF NOT EXISTS health;

-- ─── CAMERA_HEALTH_STATE ────────────────────────────────────────────────────
-- Mapa de status atual de cada câmera — atualizado apenas em mudanças de estado
-- @see docs/ARCHITECTURE.md#chms-service (ADR-006)

CREATE TABLE IF NOT EXISTS health.camera_health_state (
    id                          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    camera_id                   UUID          NOT NULL UNIQUE,
    tenant_id                   UUID          NOT NULL,
    last_seen_at                TIMESTAMPTZ,
    status                      VARCHAR(50)   NOT NULL DEFAULT 'UNKNOWN'
                                              CHECK (status IN ('ONLINE', 'OFFLINE', 'UNKNOWN')),
    consecutive_failures        INTEGER       NOT NULL DEFAULT 0,
    recording_confidence_score  NUMERIC(5,2)  DEFAULT 100.0,
    updated_at                  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_health_state_tenant_id  ON health.camera_health_state (tenant_id);
CREATE INDEX IF NOT EXISTS idx_health_state_camera_id  ON health.camera_health_state (camera_id);
CREATE INDEX IF NOT EXISTS idx_health_state_status     ON health.camera_health_state (tenant_id, status);

-- ─── HEALTH_EVENTS ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS health.health_events (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    camera_id   UUID          NOT NULL,
    tenant_id   UUID          NOT NULL,
    type        VARCHAR(50)   NOT NULL
                              CHECK (type IN ('WENT_OFFLINE', 'CAME_ONLINE', 'LOW_CONFIDENCE', 'HEARTBEAT_MISS')),
    severity    VARCHAR(50)   NOT NULL DEFAULT 'WARNING'
                              CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    detected_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    metadata    JSONB
);

CREATE INDEX IF NOT EXISTS idx_health_events_camera      ON health.health_events (camera_id, detected_at DESC);
CREATE INDEX IF NOT EXISTS idx_health_events_tenant_type ON health.health_events (tenant_id, type, detected_at DESC);
CREATE INDEX IF NOT EXISTS idx_health_events_active      ON health.health_events (tenant_id, camera_id)
    WHERE resolved_at IS NULL;

-- ─── OUTBOX_EVENTS ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS health.outbox_events (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    topic       VARCHAR(100)  NOT NULL,
    event_type  VARCHAR(100)  NOT NULL,
    payload     JSONB         NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    sent_at     TIMESTAMPTZ,
    attempts    INTEGER       NOT NULL DEFAULT 0,
    last_error  TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_unsent ON health.outbox_events (created_at)
    WHERE sent_at IS NULL;
