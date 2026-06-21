-- @path services/chms-service/src/main/resources/db/migration/V3__rename_tenant_to_org.sql
-- @owner chms-service
-- @responsibility Rename tenant_id → org_id (idempotente)

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema='health' AND table_name='camera_health_state' AND column_name='tenant_id') THEN
        ALTER TABLE health.camera_health_state RENAME COLUMN tenant_id TO org_id;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema='health' AND table_name='health_events' AND column_name='tenant_id') THEN
        ALTER TABLE health.health_events RENAME COLUMN tenant_id TO org_id;
    END IF;
END $$;

DROP POLICY IF EXISTS tenant_isolation_health_state  ON health.camera_health_state;
DROP POLICY IF EXISTS tenant_isolation_health_events ON health.health_events;
DROP POLICY IF EXISTS org_isolation_health_state     ON health.camera_health_state;
DROP POLICY IF EXISTS org_isolation_health_events    ON health.health_events;

CREATE POLICY org_isolation_health_state ON health.camera_health_state
    AS PERMISSIVE FOR ALL
    USING (org_id = current_setting('app.current_org_id', TRUE)::UUID)
    WITH CHECK (org_id = current_setting('app.current_org_id', TRUE)::UUID);

CREATE POLICY org_isolation_health_events ON health.health_events
    AS PERMISSIVE FOR ALL
    USING (org_id = current_setting('app.current_org_id', TRUE)::UUID)
    WITH CHECK (org_id = current_setting('app.current_org_id', TRUE)::UUID);
