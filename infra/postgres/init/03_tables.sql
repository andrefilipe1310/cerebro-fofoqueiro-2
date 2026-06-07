-- @path infra/postgres/init/03_tables.sql
-- @owner infra
-- @responsibility Cria todas as tabelas de domínio — deve rodar antes do 99_dev_seed.sql
-- @see docs/DATA_MODEL.md | docs/SDD.md

-- ─── TENANTS SCHEMA ─────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS tenants.tenants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug            VARCHAR(100) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    domain          VARCHAR(255),
    plan            VARCHAR(50)  NOT NULL DEFAULT 'STARTER',
    max_cameras     INTEGER      NOT NULL DEFAULT 5,
    max_users       INTEGER      NOT NULL DEFAULT 5,
    retention_days  INTEGER      NOT NULL DEFAULT 30,
    status          VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tenants.outbox_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic       VARCHAR(100) NOT NULL,
    event_type  VARCHAR(100) NOT NULL,
    payload     JSONB        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    sent_at     TIMESTAMPTZ,
    attempts    INTEGER      NOT NULL DEFAULT 0,
    last_error  TEXT
);

CREATE INDEX IF NOT EXISTS idx_tenants_outbox_unsent
    ON tenants.outbox_events (created_at) WHERE sent_at IS NULL;

-- ─── AUTH SCHEMA ────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS auth.users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID         NOT NULL REFERENCES tenants.tenants(id),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255),
    role            VARCHAR(50)  NOT NULL DEFAULT 'OPERATOR',
    totp_secret     VARCHAR(255),
    totp_enabled    BOOLEAN      NOT NULL DEFAULT false,
    active          BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, email)
);

CREATE TABLE IF NOT EXISTS auth.refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS auth.outbox_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic       VARCHAR(100) NOT NULL,
    event_type  VARCHAR(100) NOT NULL,
    payload     JSONB        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    sent_at     TIMESTAMPTZ,
    attempts    INTEGER      NOT NULL DEFAULT 0,
    last_error  TEXT
);

CREATE INDEX IF NOT EXISTS idx_auth_outbox_unsent
    ON auth.outbox_events (created_at) WHERE sent_at IS NULL;

-- ─── CAMERAS SCHEMA ─────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS cameras.locations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL REFERENCES tenants.tenants(id),
    name        VARCHAR(255) NOT NULL,
    address     TEXT,
    lat         DECIMAL(10, 7),
    lng         DECIMAL(10, 7),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cameras.cameras (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID        NOT NULL REFERENCES tenants.tenants(id),
    location_id         UUID        REFERENCES cameras.locations(id),
    name                VARCHAR(255) NOT NULL,
    rtsp_url_encrypted  TEXT,
    status              VARCHAR(50)  NOT NULL DEFAULT 'UNKNOWN',
    lat                 DECIMAL(10, 7),
    lng                 DECIMAL(10, 7),
    ptz_enabled         BOOLEAN      NOT NULL DEFAULT false,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cameras_tenant
    ON cameras.cameras (tenant_id) WHERE status != 'DELETED';

CREATE TABLE IF NOT EXISTS cameras.outbox_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic       VARCHAR(100) NOT NULL,
    event_type  VARCHAR(100) NOT NULL,
    payload     JSONB        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    sent_at     TIMESTAMPTZ,
    attempts    INTEGER      NOT NULL DEFAULT 0,
    last_error  TEXT
);

CREATE INDEX IF NOT EXISTS idx_cameras_outbox_unsent
    ON cameras.outbox_events (created_at) WHERE sent_at IS NULL;

-- ─── HEALTH SCHEMA ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS health.camera_health_state (
    camera_id                   UUID        PRIMARY KEY,
    tenant_id                   UUID        NOT NULL,
    status                      VARCHAR(50) NOT NULL DEFAULT 'UNKNOWN',
    last_seen_at                TIMESTAMPTZ,
    recording_confidence_score  DECIMAL(5, 2) NOT NULL DEFAULT 0.0,
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS health.outbox_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic       VARCHAR(100) NOT NULL,
    event_type  VARCHAR(100) NOT NULL,
    payload     JSONB        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    sent_at     TIMESTAMPTZ,
    attempts    INTEGER      NOT NULL DEFAULT 0,
    last_error  TEXT
);

CREATE INDEX IF NOT EXISTS idx_health_outbox_unsent
    ON health.outbox_events (created_at) WHERE sent_at IS NULL;

-- ─── ALERTS SCHEMA ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS alerts.alerts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    camera_id       UUID         NOT NULL,
    tenant_id       UUID         NOT NULL,
    type            VARCHAR(100) NOT NULL,
    message         TEXT,
    severity        VARCHAR(50)  NOT NULL DEFAULT 'WARNING',
    status          VARCHAR(50)  NOT NULL DEFAULT 'TRIGGERED',
    triggered_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    acknowledged_by UUID,
    acknowledged_at TIMESTAMPTZ,
    resolved_at     TIMESTAMPTZ,
    kafka_event_id  VARCHAR(255) UNIQUE
);

CREATE INDEX IF NOT EXISTS idx_alerts_tenant_status
    ON alerts.alerts (tenant_id, status, triggered_at DESC);

CREATE TABLE IF NOT EXISTS alerts.outbox_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic       VARCHAR(100) NOT NULL,
    event_type  VARCHAR(100) NOT NULL,
    payload     JSONB        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    sent_at     TIMESTAMPTZ,
    attempts    INTEGER      NOT NULL DEFAULT 0,
    last_error  TEXT
);

CREATE INDEX IF NOT EXISTS idx_alerts_outbox_unsent
    ON alerts.outbox_events (created_at) WHERE sent_at IS NULL;

-- ─── RECORDINGS SCHEMA ──────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS recordings.recordings (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID         NOT NULL,
    camera_id        UUID         NOT NULL,
    r2_key           TEXT         NOT NULL,
    filename         VARCHAR(255),
    started_at       TIMESTAMPTZ  NOT NULL,
    ended_at         TIMESTAMPTZ,
    duration_seconds INTEGER,
    file_size_bytes  BIGINT,
    status           VARCHAR(50)  NOT NULL DEFAULT 'STORED',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_recordings_camera_time
    ON recordings.recordings (camera_id, started_at);

CREATE INDEX IF NOT EXISTS idx_recordings_tenant_time
    ON recordings.recordings (tenant_id, started_at DESC);

CREATE TABLE IF NOT EXISTS recordings.outbox_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic       VARCHAR(100) NOT NULL,
    event_type  VARCHAR(100) NOT NULL,
    payload     JSONB        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    sent_at     TIMESTAMPTZ,
    attempts    INTEGER      NOT NULL DEFAULT 0,
    last_error  TEXT
);

CREATE INDEX IF NOT EXISTS idx_recordings_outbox_unsent
    ON recordings.outbox_events (created_at) WHERE sent_at IS NULL;

-- ─── AUDIT SCHEMA ───────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS audit.audit_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id      VARCHAR(255) NOT NULL UNIQUE,
    tenant_id     UUID         NOT NULL,
    user_id       UUID,
    action        VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id   VARCHAR(255),
    ip_address    VARCHAR(45),
    occurred_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    raw_payload   JSONB
);

CREATE INDEX IF NOT EXISTS idx_audit_tenant_time
    ON audit.audit_logs (tenant_id, occurred_at DESC);

-- audit_service_user precisa de CREATE no schema para o Flyway criar flyway_schema_history.
-- O princípio INSERT+SELECT apenas (ADR-010) é enforçado pela aplicação, não pelo banco,
-- porque Flyway roda como o mesmo usuário do serviço.
GRANT CREATE ON SCHEMA audit TO audit_service_user;
GRANT ALL ON ALL TABLES IN SCHEMA audit TO audit_service_user;
GRANT ALL ON ALL SEQUENCES IN SCHEMA audit TO audit_service_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT ALL ON TABLES TO audit_service_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT ALL ON SEQUENCES TO audit_service_user;

-- ─── GRANTS EXPLÍCITOS (segurança de inicialização) ─────────────────────────
-- ALTER DEFAULT PRIVILEGES já cobre tabelas futuras criadas pelo superuser,
-- mas grants explícitos garantem acesso imediato às tabelas criadas acima.

GRANT ALL ON ALL TABLES IN SCHEMA tenants    TO tenant_service_user;
GRANT ALL ON ALL TABLES IN SCHEMA auth       TO auth_service_user;
GRANT ALL ON ALL TABLES IN SCHEMA cameras    TO camera_service_user;
GRANT ALL ON ALL TABLES IN SCHEMA health     TO chms_service_user;
GRANT ALL ON ALL TABLES IN SCHEMA alerts     TO alert_service_user;
GRANT ALL ON ALL TABLES IN SCHEMA recordings TO recording_service_user;

GRANT ALL ON ALL SEQUENCES IN SCHEMA tenants    TO tenant_service_user;
GRANT ALL ON ALL SEQUENCES IN SCHEMA auth       TO auth_service_user;
GRANT ALL ON ALL SEQUENCES IN SCHEMA cameras    TO camera_service_user;
GRANT ALL ON ALL SEQUENCES IN SCHEMA health     TO chms_service_user;
GRANT ALL ON ALL SEQUENCES IN SCHEMA alerts     TO alert_service_user;
GRANT ALL ON ALL SEQUENCES IN SCHEMA recordings TO recording_service_user;

-- ─── TRANSFER DE OWNERSHIP PARA SERVICE USERS ────────────────────────────────
-- GRANT ALL cobre DML (SELECT/INSERT/UPDATE/DELETE), mas ALTER TABLE é DDL e
-- exige que o executante seja OWNER da tabela.
-- O Flyway roda como o service user e precisará de DDL nas migrações V2+.
-- Todas as tabelas criadas acima pelo superuser (postgres) devem ser
-- transferidas para o respectivo service user de cada schema.

DO $$
DECLARE
    r RECORD;
    schema_role JSONB := '{
        "tenants":    "tenant_service_user",
        "auth":       "auth_service_user",
        "cameras":    "camera_service_user",
        "health":     "chms_service_user",
        "recordings": "recording_service_user",
        "alerts":     "alert_service_user",
        "audit":      "audit_service_user"
    }';
BEGIN
    FOR r IN
        SELECT schemaname, tablename
        FROM   pg_tables
        WHERE  schemaname IN ('tenants','auth','cameras','health','recordings','alerts','audit')
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I OWNER TO %I',
            r.schemaname, r.tablename,
            schema_role ->> r.schemaname
        );
    END LOOP;

    FOR r IN
        SELECT sequence_schema AS s, sequence_name AS n
        FROM   information_schema.sequences
        WHERE  sequence_schema IN ('tenants','auth','cameras','health','recordings','alerts','audit')
    LOOP
        EXECUTE format(
            'ALTER SEQUENCE %I.%I OWNER TO %I',
            r.s, r.n,
            schema_role ->> r.s
        );
    END LOOP;
END $$;
