-- @path services/camera-service/src/main/resources/db/migration/V4__add_privacy_zones_updated_at.sql
-- @owner camera-service
-- @responsibility Adiciona updated_at em privacy_zones (necessário para validação do Hibernate)

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'cameras' AND table_name = 'privacy_zones' AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE cameras.privacy_zones ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
    END IF;
END $$;
