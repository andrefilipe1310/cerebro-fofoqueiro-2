-- @path services/auth-service/src/main/resources/db/rollback/V1__rollback_auth_tables.sql
-- @owner auth-service
-- @responsibility Rollback manual da V1 — executa APENAS se precisar reverter a migration
-- ATENÇÃO: Este script NÃO é executado pelo Flyway automaticamente.
-- Para usar: psql -U auth_service_user -d platform -f V1__rollback_auth_tables.sql

DROP TRIGGER IF EXISTS users_set_updated_at ON auth.users;
DROP TABLE IF EXISTS auth.outbox_events;
DROP TABLE IF EXISTS auth.refresh_tokens;
DROP TABLE IF EXISTS auth.users;
DROP FUNCTION IF EXISTS auth.trigger_set_updated_at();
