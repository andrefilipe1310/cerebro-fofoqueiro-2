-- @path services/tenant-service/src/main/resources/db/migration/V1__create_tenant_tables.sql
-- @owner organization-service (antigo tenant-service)
-- @responsibility Cria tabelas do schema organizations: organizations, outbox_events
-- @see docs/DATA_MODEL.md#organizations | docs/SDD.md#outbox

CREATE SCHEMA IF NOT EXISTS organizations;

-- Função para atualizar updated_at automaticamente
CREATE OR REPLACE FUNCTION organizations.trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ─── ORGANIZATIONS ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS organizations.organizations (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    slug            VARCHAR(100)  NOT NULL UNIQUE,
    name            VARCHAR(255)  NOT NULL,
    domain          VARCHAR(255)  UNIQUE,
    plan            VARCHAR(50)   NOT NULL DEFAULT 'FREE'
                                  CHECK (plan IN ('FREE', 'STARTER', 'PRO', 'ENTERPRISE')),
    logo_url        TEXT,
    css_override    TEXT,
    max_cameras     INTEGER       NOT NULL DEFAULT 4,
    max_users       INTEGER       NOT NULL DEFAULT 2,
    retention_days  INTEGER       NOT NULL DEFAULT 7,
    status          VARCHAR(50)   NOT NULL DEFAULT 'ACTIVE'
                                  CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED')),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_organizations_slug   ON organizations.organizations (slug);
CREATE INDEX IF NOT EXISTS idx_organizations_domain ON organizations.organizations (domain) WHERE domain IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_organizations_status ON organizations.organizations (status);
CREATE INDEX IF NOT EXISTS idx_organizations_plan   ON organizations.organizations (plan);

CREATE TRIGGER organizations_set_updated_at
    BEFORE UPDATE ON organizations.organizations
    FOR EACH ROW EXECUTE FUNCTION organizations.trigger_set_updated_at();

-- ─── OUTBOX_EVENTS ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS organizations.outbox_events (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    topic       VARCHAR(100)  NOT NULL,
    event_type  VARCHAR(100)  NOT NULL,
    payload     JSONB         NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    sent_at     TIMESTAMPTZ,
    attempts    INTEGER       NOT NULL DEFAULT 0,
    last_error  TEXT
);

CREATE INDEX IF NOT EXISTS idx_org_outbox_unsent ON organizations.outbox_events (created_at)
    WHERE sent_at IS NULL;
