-- @path services/recording-service/src/main/resources/db/migration/V3__rename_tenant_to_org.sql
-- @owner recording-service
-- @responsibility Rename tenant_id → org_id (idempotente)

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema='recordings' AND table_name='recordings' AND column_name='tenant_id') THEN
        ALTER TABLE recordings.recordings RENAME COLUMN tenant_id TO org_id;
    END IF;
END $$;

DROP POLICY IF EXISTS tenant_isolation_recordings ON recordings.recordings;
DROP POLICY IF EXISTS org_isolation_recordings    ON recordings.recordings;

CREATE POLICY org_isolation_recordings ON recordings.recordings
    AS PERMISSIVE FOR ALL
    USING (org_id = current_setting('app.current_org_id', TRUE)::UUID)
    WITH CHECK (org_id = current_setting('app.current_org_id', TRUE)::UUID);
