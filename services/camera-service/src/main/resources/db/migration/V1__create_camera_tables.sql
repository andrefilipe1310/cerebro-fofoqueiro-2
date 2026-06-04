-- @path services/camera-service/src/main/resources/db/migration/V1__create_camera_tables.sql
-- @owner camera-service
-- @responsibility Cria tabelas do schema cameras: locations, cameras, privacy_zones, outbox_events
-- @see docs/DATA_MODEL.md#cameras | docs/MULTI_TENANCY.md#rls-exemplo | docs/SDD.md#outbox

-- Função para atualizar updated_at automaticamente
CREATE OR REPLACE FUNCTION cameras.trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ─── LOCATIONS ──────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS cameras.locations (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID          NOT NULL,
    name        VARCHAR(255)  NOT NULL,
    address     TEXT,
    lat         DECIMAL(10,8),
    lng         DECIMAL(11,8),
    map_bounds  JSONB,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_locations_tenant_id ON cameras.locations (tenant_id);

CREATE TRIGGER locations_set_updated_at
    BEFORE UPDATE ON cameras.locations
    FOR EACH ROW EXECUTE FUNCTION cameras.trigger_set_updated_at();

ALTER TABLE cameras.locations ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_locations ON cameras.locations
    AS PERMISSIVE FOR ALL
    TO camera_service_user
    USING (tenant_id = current_setting('app.current_tenant_id', TRUE)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', TRUE)::UUID);

-- ─── CAMERAS ────────────────────────────────────────────────────────────────
-- rtsp_url_encrypted — AES-256-GCM conforme ADR-005
-- @see docs/SECURITY_LGPD.md#criptografia

CREATE TABLE IF NOT EXISTS cameras.cameras (
    id                          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                   UUID          NOT NULL,
    location_id                 UUID          REFERENCES cameras.locations(id) ON DELETE SET NULL,
    name                        VARCHAR(255)  NOT NULL,
    rtsp_url_encrypted          TEXT          NOT NULL,
    sub_stream_url_encrypted    TEXT,
    lat                         DECIMAL(10,8),
    lng                         DECIMAL(11,8),
    status                      VARCHAR(50)   NOT NULL DEFAULT 'UNKNOWN'
                                              CHECK (status IN ('ONLINE', 'OFFLINE', 'UNKNOWN', 'DELETED')),
    ptz_enabled                 BOOLEAN       NOT NULL DEFAULT FALSE,
    stream_token                TEXT,
    stream_token_expires_at     TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cameras_tenant_status   ON cameras.cameras (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_cameras_location_id     ON cameras.cameras (location_id);
CREATE INDEX IF NOT EXISTS idx_cameras_tenant_created  ON cameras.cameras (tenant_id, created_at DESC);

CREATE TRIGGER cameras_set_updated_at
    BEFORE UPDATE ON cameras.cameras
    FOR EACH ROW EXECUTE FUNCTION cameras.trigger_set_updated_at();

ALTER TABLE cameras.cameras ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_cameras ON cameras.cameras
    AS PERMISSIVE FOR ALL
    TO camera_service_user
    USING (tenant_id = current_setting('app.current_tenant_id', TRUE)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', TRUE)::UUID);

-- ─── PRIVACY_ZONES ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS cameras.privacy_zones (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    camera_id   UUID          NOT NULL REFERENCES cameras.cameras(id) ON DELETE CASCADE,
    tenant_id   UUID          NOT NULL,
    name        VARCHAR(255)  NOT NULL,
    coordinates JSONB         NOT NULL,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_privacy_zones_camera_id ON cameras.privacy_zones (camera_id);
CREATE INDEX IF NOT EXISTS idx_privacy_zones_active    ON cameras.privacy_zones (camera_id) WHERE active = TRUE;

CREATE TRIGGER privacy_zones_set_updated_at
    BEFORE UPDATE ON cameras.privacy_zones
    FOR EACH ROW EXECUTE FUNCTION cameras.trigger_set_updated_at();

ALTER TABLE cameras.privacy_zones ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation_privacy_zones ON cameras.privacy_zones
    AS PERMISSIVE FOR ALL
    TO camera_service_user
    USING (tenant_id = current_setting('app.current_tenant_id', TRUE)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', TRUE)::UUID);

-- ─── OUTBOX_EVENTS ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS cameras.outbox_events (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    topic       VARCHAR(100)  NOT NULL,
    event_type  VARCHAR(100)  NOT NULL,
    payload     JSONB         NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    sent_at     TIMESTAMPTZ,
    attempts    INTEGER       NOT NULL DEFAULT 0,
    last_error  TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_unsent ON cameras.outbox_events (created_at)
    WHERE sent_at IS NULL;
