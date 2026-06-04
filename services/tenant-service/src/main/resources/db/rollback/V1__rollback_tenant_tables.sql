-- @path services/tenant-service/src/main/resources/db/rollback/V1__rollback_tenant_tables.sql
-- @owner tenant-service
-- @responsibility Rollback manual da V1 — executa APENAS se precisar reverter a migration

DROP TRIGGER IF EXISTS tenants_set_updated_at ON tenants.tenants;
DROP TABLE IF EXISTS tenants.outbox_events;
DROP TABLE IF EXISTS tenants.tenants;
DROP FUNCTION IF EXISTS tenants.trigger_set_updated_at();
