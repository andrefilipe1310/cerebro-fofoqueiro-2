-- V2: adiciona colunas logo_url e css_override à tabela organizations
-- (init script legado omitia essas colunas; IF NOT EXISTS garante idempotência)
ALTER TABLE organizations.organizations ADD COLUMN IF NOT EXISTS logo_url     TEXT;
ALTER TABLE organizations.organizations ADD COLUMN IF NOT EXISTS css_override TEXT;
