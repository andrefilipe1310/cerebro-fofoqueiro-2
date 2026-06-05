-- @path services/auth-service/src/main/resources/db/migration/V1__create_auth_tables.sql
-- @owner auth-service
-- @responsibility Cria tabelas do schema auth: users, refresh_tokens, outbox_events
-- @see docs/DATA_MODEL.md#users | docs/SDD.md#outbox | docs/MULTI_TENANCY.md#rls-exemplo

-- Schema criado aqui para funcionar em Testcontainers (sem init scripts externos)
CREATE SCHEMA IF NOT EXISTS auth;

-- Função para atualizar updated_at automaticamente
CREATE OR REPLACE FUNCTION auth.trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ─── USERS ──────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS auth.users (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID          NOT NULL,
    email           VARCHAR(255)  NOT NULL,
    password_hash   VARCHAR(255)  NOT NULL,
    role            VARCHAR(50)   NOT NULL DEFAULT 'VIEWER'
                                  CHECK (role IN ('ADMIN', 'OPERATOR', 'VIEWER')),
    totp_secret     TEXT,
    totp_enabled    BOOLEAN       NOT NULL DEFAULT FALSE,
    backup_codes    TEXT[],
    last_login      TIMESTAMPTZ,
    failed_attempts INTEGER       NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, email)
);

CREATE INDEX IF NOT EXISTS idx_users_tenant_id    ON auth.users (tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_email        ON auth.users (email);
CREATE INDEX IF NOT EXISTS idx_users_active       ON auth.users (active) WHERE active = TRUE;

CREATE TRIGGER users_set_updated_at
    BEFORE UPDATE ON auth.users
    FOR EACH ROW EXECUTE FUNCTION auth.trigger_set_updated_at();

-- RLS — isolamento por tenant (ADR-001)
ALTER TABLE auth.users ENABLE ROW LEVEL SECURITY;
-- TO omitido: policy aplica a todos os roles (isolamento de schema via GRANT está no init script)
CREATE POLICY tenant_isolation_users ON auth.users
    AS PERMISSIVE FOR ALL
    USING (tenant_id = current_setting('app.current_tenant_id', TRUE)::UUID)
    WITH CHECK (tenant_id = current_setting('app.current_tenant_id', TRUE)::UUID);

-- ─── REFRESH_TOKENS ─────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS auth.refresh_tokens (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID          NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    token_hash  TEXT          NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ   NOT NULL,
    revoked     BOOLEAN       NOT NULL DEFAULT FALSE,
    ip_address  INET,
    user_agent  TEXT,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id    ON auth.refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON auth.refresh_tokens (expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_hash       ON auth.refresh_tokens (token_hash);

-- ─── OUTBOX_EVENTS ──────────────────────────────────────────────────────────
-- Implementa o Outbox Pattern para garantir consistência banco + Kafka
-- @see docs/SDD.md#outbox

CREATE TABLE IF NOT EXISTS auth.outbox_events (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    topic       VARCHAR(100)  NOT NULL,
    event_type  VARCHAR(100)  NOT NULL,
    payload     JSONB         NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    sent_at     TIMESTAMPTZ,
    attempts    INTEGER       NOT NULL DEFAULT 0,
    last_error  TEXT
);

CREATE INDEX IF NOT EXISTS idx_outbox_unsent ON auth.outbox_events (created_at)
    WHERE sent_at IS NULL;
