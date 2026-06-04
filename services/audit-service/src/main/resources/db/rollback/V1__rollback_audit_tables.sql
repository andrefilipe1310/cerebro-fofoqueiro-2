-- @path services/audit-service/src/main/resources/db/rollback/V1__rollback_audit_tables.sql
-- @owner audit-service
-- @responsibility Rollback manual da V1 — ATENÇÃO: perda permanente de logs de auditoria

DROP TRIGGER IF EXISTS audit_immutability ON audit.audit_logs;
DROP FUNCTION IF EXISTS audit.prevent_audit_mutation();
DROP TABLE IF EXISTS audit.audit_logs;
