-- @path services/recording-service/src/main/resources/db/migration/V3__rename_tenant_to_org.sql
-- @owner recording-service
-- @responsibility Renomeia tenant_id → org_id e atualiza políticas RLS

ALTER TABLE recordings.recordings RENAME COLUMN tenant_id TO org_id;

ALTER INDEX IF EXISTS idx_recordings_tenant_id RENAME TO idx_recordings_org_id;

DROP POLICY IF EXISTS tenant_isolation_recordings ON recordings.recordings;

CREATE POLICY org_isolation_recordings ON recordings.recordings
    AS PERMISSIVE FOR ALL
    USING (org_id = current_setting('app.current_org_id', TRUE)::UUID)
    WITH CHECK (org_id = current_setting('app.current_org_id', TRUE)::UUID);
