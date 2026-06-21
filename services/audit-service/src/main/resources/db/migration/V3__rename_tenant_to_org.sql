-- @path services/audit-service/src/main/resources/db/migration/V3__rename_tenant_to_org.sql
-- @owner audit-service
-- @responsibility Renomeia tenant_id → org_id (audit não tem RLS — imutável por design)

ALTER TABLE audit.audit_logs RENAME COLUMN tenant_id TO org_id;

ALTER INDEX IF EXISTS idx_audit_logs_tenant_id RENAME TO idx_audit_logs_org_id;
