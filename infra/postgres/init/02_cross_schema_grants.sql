-- @path infra/postgres/init/02_cross_schema_grants.sql
-- @owner infra
-- @responsibility Grants cross-schema controlados — executados APÓS as tabelas serem criadas
-- @see docs/ARCHITECTURE.md#banco-dados

-- auth-service pode ler dados básicos de organizations para popular o org picker no login
-- Executa após as tabelas existirem (por isso em arquivo separado, com \gexec pattern)
-- Em docker, o Flyway de organization-service cria a tabela; o init roda antes.
-- Por isso usamos DO block para criar a grant quando a tabela existir.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'organizations' AND table_name = 'organizations'
    ) THEN
        EXECUTE 'GRANT SELECT (id, slug, name, logo_url, status) ON organizations.organizations TO auth_service_user';
    END IF;
END $$;
