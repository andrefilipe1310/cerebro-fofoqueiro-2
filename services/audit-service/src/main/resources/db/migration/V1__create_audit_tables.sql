-- @path services/audit-service/src/main/resources/db/migration/V1__create_audit_tables.sql
-- @owner audit-service
-- @responsibility Cria tabela de audit logs — append-only com trigger de imutabilidade
-- @see docs/DATA_MODEL.md#audit_logs | docs/SECURITY_LGPD.md#imutabilidade-audit (ADR-010)

CREATE SCHEMA IF NOT EXISTS audit;

-- ─── AUDIT_LOGS ─────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS audit.audit_logs (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        TEXT          NOT NULL UNIQUE,   -- Chave de idempotência (ADR-011)
    tenant_id       UUID          NOT NULL,
    user_id         UUID,                            -- NULL para ações do sistema
    action          VARCHAR(100)  NOT NULL,
    resource_type   VARCHAR(50),
    resource_id     TEXT,
    ip_address      INET,
    user_agent      TEXT,
    metadata        JSONB,
    raw_payload     JSONB,                           -- Payload original do evento Kafka
    occurred_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_time  ON audit.audit_logs (tenant_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user         ON audit.audit_logs (user_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action       ON audit.audit_logs (tenant_id, action, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_resource     ON audit.audit_logs (tenant_id, resource_type, resource_id);

-- ─── TRIGGER DE IMUTABILIDADE ───────────────────────────────────────────────
-- ADR-010: Audit Service é append-only. UPDATE e DELETE são proibidos por design.
-- Este trigger garante imutabilidade mesmo em caso de acesso direto ao banco.

CREATE OR REPLACE FUNCTION audit.prevent_audit_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs é append-only: % é proibido. Tentativa por: %',
        TG_OP, current_user;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER audit_immutability
    BEFORE UPDATE OR DELETE ON audit.audit_logs
    FOR EACH ROW EXECUTE FUNCTION audit.prevent_audit_mutation();

-- Permissões granulares só aplicadas se o role existir (produção via init script)
DO $$ BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'audit_service_user') THEN
        GRANT INSERT, SELECT ON audit.audit_logs TO audit_service_user;
        REVOKE UPDATE, DELETE ON audit.audit_logs FROM audit_service_user;
    END IF;
END $$;
