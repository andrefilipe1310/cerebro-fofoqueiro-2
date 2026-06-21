-- @path services/auth-service/src/main/resources/db/migration/V4__user_org_membership.sql
-- @owner auth-service
-- @responsibility Refatora user de 1:N para N:M com organization. Cria user_memberships.
-- @see docs/ARCHITECTURE.md#auth-service

-- ─── JOIN TABLE: user ↔ organization (N:M) ──────────────────────────────────

CREATE TABLE IF NOT EXISTS auth.user_memberships (
    id         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID          NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    org_id     UUID          NOT NULL,
    role       VARCHAR(50)   NOT NULL CHECK (role IN ('ADMIN', 'OPERATOR', 'VIEWER')),
    active     BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, org_id)
);

CREATE INDEX IF NOT EXISTS idx_memberships_user_id ON auth.user_memberships (user_id);
CREATE INDEX IF NOT EXISTS idx_memberships_org_id  ON auth.user_memberships (org_id);
CREATE INDEX IF NOT EXISTS idx_memberships_active  ON auth.user_memberships (active) WHERE active = TRUE;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'user_memberships_set_updated_at') THEN
        CREATE TRIGGER user_memberships_set_updated_at
            BEFORE UPDATE ON auth.user_memberships
            FOR EACH ROW EXECUTE FUNCTION auth.trigger_set_updated_at();
    END IF;
END $$;

-- ─── MIGRAÇÃO DE DADOS: auth.users → auth.user_memberships ──────────────────
-- Preserva tenant_id e role existentes como memberships

INSERT INTO auth.user_memberships (user_id, org_id, role, active)
SELECT id, tenant_id, role, active
FROM auth.users
WHERE tenant_id IS NOT NULL
ON CONFLICT (user_id, org_id) DO NOTHING;

-- ─── REMOVE COLUNAS QUE SAEM DA ENTIDADE USER ───────────────────────────────
-- tenant_id e role agora vivem em user_memberships

ALTER TABLE auth.users DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE auth.users DROP COLUMN IF EXISTS role;

-- ─── ADICIONA org_id AO REFRESH_TOKENS ──────────────────────────────────────
-- Necessário para o refresh saber qual organização re-emitir o token scoped

ALTER TABLE auth.refresh_tokens ADD COLUMN IF NOT EXISTS org_id UUID;

-- ─── RLS EM USERS DESATIVADA ─────────────────────────────────────────────────
-- Não há mais org_id em auth.users — isolamento acontece em user_memberships
-- e nos outros schemas via app.current_org_id

DROP POLICY IF EXISTS tenant_isolation_users ON auth.users;
ALTER TABLE auth.users DISABLE ROW LEVEL SECURITY;

-- ─── RENAME CONSTRAINT de V3 (email global único) ───────────────────────────
-- Já existe de V3, apenas garantindo idempotência
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'auth_users_email_key'
          AND conrelid = 'auth.users'::regclass
    ) THEN
        ALTER TABLE auth.users ADD CONSTRAINT auth_users_email_key UNIQUE (email);
    END IF;
END $$;
