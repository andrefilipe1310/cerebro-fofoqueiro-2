-- V2: adiciona colunas logo_url e css_override que existem na entity Tenant
-- mas foram omitidas do init script 03_tables.sql (a V1 usa CREATE TABLE IF NOT EXISTS
-- e é no-op quando a tabela já existe, então as colunas nunca foram criadas).
ALTER TABLE tenants.tenants ADD COLUMN IF NOT EXISTS logo_url     TEXT;
ALTER TABLE tenants.tenants ADD COLUMN IF NOT EXISTS css_override TEXT;
