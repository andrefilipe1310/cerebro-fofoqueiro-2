-- @path services/auth-service/src/main/resources/db/migration/V3__global_email_unique.sql
-- @owner auth-service
-- @responsibility Email globalmente único — novo fluxo de login sem tenant slug
-- @see docs/ARCHITECTURE.md#auth-service

-- Remove a constraint composta (tenant_id, email) e substitui por email único globalmente.
-- Email é globalmente único no sistema: um usuário = um email em todos os tenants.
ALTER TABLE auth.users DROP CONSTRAINT IF EXISTS auth_users_tenant_id_email_key;

-- Garante idempotência: só adiciona se não existir
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'auth_users_email_key'
          AND conrelid = 'auth.users'::regclass
    ) THEN
        ALTER TABLE auth.users ADD CONSTRAINT auth_users_email_key UNIQUE (email);
    END IF;
END $$;
