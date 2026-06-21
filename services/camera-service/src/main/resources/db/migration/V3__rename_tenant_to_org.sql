-- @path services/camera-service/src/main/resources/db/migration/V3__rename_tenant_to_org.sql
-- @owner camera-service
-- @responsibility Renomeia tenant_id → org_id e atualiza políticas RLS

ALTER TABLE cameras.cameras       RENAME COLUMN tenant_id TO org_id;
ALTER TABLE cameras.locations     RENAME COLUMN tenant_id TO org_id;
ALTER TABLE cameras.privacy_zones RENAME COLUMN tenant_id TO org_id;

-- Atualiza índices
ALTER INDEX IF EXISTS idx_cameras_tenant_id    RENAME TO idx_cameras_org_id;
ALTER INDEX IF EXISTS idx_locations_tenant_id  RENAME TO idx_locations_org_id;

-- Atualiza políticas RLS: tenant_isolation → org_isolation
DROP POLICY IF EXISTS tenant_isolation_cameras       ON cameras.cameras;
DROP POLICY IF EXISTS tenant_isolation_locations     ON cameras.locations;
DROP POLICY IF EXISTS tenant_isolation_privacy_zones ON cameras.privacy_zones;

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
