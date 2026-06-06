-- Colunas presentes na migration V1 mas ausentes no 03_tables.sql (init script)

ALTER TABLE cameras.locations
    ADD COLUMN IF NOT EXISTS map_bounds JSONB;

ALTER TABLE cameras.cameras
    ADD COLUMN IF NOT EXISTS sub_stream_url_encrypted TEXT,
    ADD COLUMN IF NOT EXISTS stream_token TEXT,
    ADD COLUMN IF NOT EXISTS stream_token_expires_at TIMESTAMPTZ;

-- privacy_zones pode não existir se criado apenas pelo init (03_tables não a tem)
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
