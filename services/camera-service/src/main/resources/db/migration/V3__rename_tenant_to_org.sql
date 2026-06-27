-- @path services/camera-service/src/main/resources/db/migration/V3__rename_tenant_to_org.sql
-- @owner camera-service
-- @responsibility Rename tenant_id → org_id (idempotente — no-op se coluna já é org_id)

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema='cameras' AND table_name='cameras' AND column_name='tenant_id') THEN
        ALTER TABLE cameras.cameras RENAME COLUMN tenant_id TO org_id;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema='cameras' AND table_name='locations' AND column_name='tenant_id') THEN
        ALTER TABLE cameras.locations RENAME COLUMN tenant_id TO org_id;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema='cameras' AND table_name='privacy_zones' AND column_name='tenant_id') THEN
        ALTER TABLE cameras.privacy_zones RENAME COLUMN tenant_id TO org_id;
    END IF;
END $$;

-- Atualiza políticas RLS (drop+create é idempotente)
DROP POLICY IF EXISTS tenant_isolation_cameras       ON cameras.cameras;
DROP POLICY IF EXISTS tenant_isolation_locations     ON cameras.locations;
DROP POLICY IF EXISTS tenant_isolation_privacy_zones ON cameras.privacy_zones;
DROP POLICY IF EXISTS org_isolation_cameras          ON cameras.cameras;
DROP POLICY IF EXISTS org_isolation_locations        ON cameras.locations;
DROP POLICY IF EXISTS org_isolation_privacy_zones    ON cameras.privacy_zones;

CREATE POLICY org_isolation_cameras ON cameras.cameras
    AS PERMISSIVE FOR ALL
    USING (org_id = current_setting('app.current_org_id', TRUE)::UUID)
    WITH CHECK (org_id = current_setting('app.current_org_id', TRUE)::UUID);

CREATE POLICY org_isolation_locations ON cameras.locations
    AS PERMISSIVE FOR ALL
    USING (org_id = current_setting('app.current_org_id', TRUE)::UUID)
    WITH CHECK (org_id = current_setting('app.current_org_id', TRUE)::UUID);

CREATE POLICY org_isolation_privacy_zones ON cameras.privacy_zones
    AS PERMISSIVE FOR ALL
    USING (org_id = current_setting('app.current_org_id', TRUE)::UUID)
    WITH CHECK (org_id = current_setting('app.current_org_id', TRUE)::UUID);
