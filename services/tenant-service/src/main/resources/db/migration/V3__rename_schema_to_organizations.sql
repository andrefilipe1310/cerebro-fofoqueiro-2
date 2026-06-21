-- @path services/tenant-service/src/main/resources/db/migration/V3__rename_schema_to_organizations.sql
-- @owner tenant-service (em processo de ser renomeado para organization-service)
-- @responsibility Renomeia schema tenants → organizations e tabela tenants → organizations

-- Renomeia o schema
ALTER SCHEMA tenants RENAME TO organizations;

-- Renomeia a tabela principal (agora em organizations.tenants → organizations.organizations)
ALTER TABLE organizations.tenants RENAME TO organizations;

-- Renomeia o trigger
ALTER TRIGGER tenants_set_updated_at ON organizations.organizations
    RENAME TO organizations_set_updated_at;

-- Renomeia a tabela outbox_events (estava em tenants.outbox_events)
-- Tabela outbox_events continua com o mesmo nome mas agora em schema organizations

-- Atualiza índices que referenciam o nome antigo
ALTER INDEX IF EXISTS idx_tenants_slug   RENAME TO idx_organizations_slug;
ALTER INDEX IF EXISTS idx_tenants_domain RENAME TO idx_organizations_domain;
ALTER INDEX IF EXISTS idx_tenants_status RENAME TO idx_organizations_status;
ALTER INDEX IF EXISTS idx_tenants_plan   RENAME TO idx_organizations_plan;

-- org_service_user é owner da tabela (Flyway roda como ela) → pode conceder SELECT
-- auth-service precisa ler dados básicos de org para popular o org picker no login
GRANT SELECT (id, slug, name, logo_url, status) ON organizations.organizations TO auth_service_user;
