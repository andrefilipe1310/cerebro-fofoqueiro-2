-- @path services/tenant-service/src/main/resources/db/migration/V3__rename_schema_to_organizations.sql
-- @owner organization-service
-- @responsibility Migração de schema: tenants → organizations (idempotente)
-- Em instalações novas (V1 já cria organizations), este script é no-op.
-- Em upgrades de instalações antigas, renomeia o schema legado.

DO $$
BEGIN
    -- Só executa o rename se o schema legado ainda existir
    IF EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'tenants') THEN
        -- Garante que organizations ainda não existe (evita conflito)
        IF NOT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'organizations') THEN
            ALTER SCHEMA tenants RENAME TO organizations;
        END IF;

        -- Renomeia tabela legada (tenants.tenants → organizations.organizations)
        IF EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = 'organizations' AND table_name = 'tenants'
        ) THEN
            ALTER TABLE organizations.tenants RENAME TO organizations;
        END IF;
    END IF;
END $$;

-- Garante índices com nomes corretos (idempotente)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_tenants_slug') THEN
        ALTER INDEX idx_tenants_slug   RENAME TO idx_organizations_slug;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_tenants_domain') THEN
        ALTER INDEX idx_tenants_domain RENAME TO idx_organizations_domain;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_tenants_status') THEN
        ALTER INDEX idx_tenants_status RENAME TO idx_organizations_status;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_tenants_plan') THEN
        ALTER INDEX idx_tenants_plan   RENAME TO idx_organizations_plan;
    END IF;
END $$;

-- auth-service lê dados básicos de org para o picker no login
-- GRANT é idempotente (pode rodar mais de uma vez sem erro)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'auth_service_user') THEN
        GRANT SELECT (id, slug, name, logo_url, status)
            ON organizations.organizations TO auth_service_user;
    END IF;
END $$;
