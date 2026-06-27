-- @path services/audit-service/src/main/resources/db/migration/V3__rename_tenant_to_org.sql
-- @owner audit-service
-- @responsibility Rename tenant_id → org_id (idempotente — audit não tem RLS)

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema='audit' AND table_name='audit_logs' AND column_name='tenant_id') THEN
        ALTER TABLE audit.audit_logs RENAME COLUMN tenant_id TO org_id;
    END IF;
END $$;
