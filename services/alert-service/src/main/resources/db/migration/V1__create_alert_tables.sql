-- @path services/alert-service/src/main/resources/db/migration/V1__create_alert_tables.sql
-- @owner alert-service
-- @responsibility Cria tabelas do schema alerts: alerts, outbox_events
-- @see docs/DATA_MODEL.md#alerts | docs/SDD.md#design-alert (ADR-011 idempotência)

-- ─── ALERTS ─────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS alerts.alerts (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    camera_id         UUID          NOT NULL,
    tenant_id         UUID          NOT NULL,
    type              VARCHAR(50)   NOT NULL
                                    CHECK (type IN ('CAMERA_OFFLINE', 'CAMERA_ONLINE', 'MOTION_DETECTED', 'LOW_CONFIDENCE')),
    message           TEXT          NOT NULL,
    severity          VARCHAR(50)   NOT NULL DEFAULT 'WARNING'
                                    CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    triggered_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    acknowledged_at   TIMESTAMPTZ,
    acknowledged_by   UUID,
    -- Chave de deduplicação por evento Kafka (ADR-011)
    kafka_event_id    TEXT          UNIQUE
);

-- Alertas não reconhecidos por tenant — query mais frequente do operador
CREATE INDEX IF NOT EXISTS idx_alerts_tenant_unack ON alerts.alerts (tenant_id, triggered_at DESC)
    WHERE acknowledged_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_alerts_camera       ON alerts.alerts (camera_id, triggered_at DESC);
CREATE INDEX IF NOT EXISTS idx_alerts_tenant_type  ON alerts.alerts (tenant_id, type, triggered_at DESC);

ALTER TABLE alerts.alerts ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_alerts ON alerts.alerts
    AS PERMISSIVE FOR ALL
    TO alert_service_user
    USING (tenant_id = current_setting('app.current_tenant_id', TRUE)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', TRUE)::UUID);

-- ─── OUTBOX_EVENTS ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS alerts.outbox_events (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    topic       VARCHAR(100)  NOT NULL,
    event_type  VARCHAR(100)  NOT NULL,
    payload     JSONB         NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    sent_at     TIMESTAMPTZ,
    attempts    INTEGER       NOT NULL DEFAULT 0,
    last_error  TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_unsent ON alerts.outbox_events (created_at)
    WHERE sent_at IS NULL;
