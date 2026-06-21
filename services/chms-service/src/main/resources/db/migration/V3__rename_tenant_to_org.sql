-- @path services/chms-service/src/main/resources/db/migration/V3__rename_tenant_to_org.sql
-- @owner chms-service
-- @responsibility Renomeia tenant_id → org_id e atualiza políticas RLS

ALTER TABLE health.camera_health_state RENAME COLUMN tenant_id TO org_id;
ALTER TABLE health.health_events       RENAME COLUMN tenant_id TO org_id;

ALTER INDEX IF EXISTS idx_health_state_tenant_id RENAME TO idx_health_state_org_id;
ALTER INDEX IF EXISTS idx_health_events_tenant_id RENAME TO idx_health_events_org_id;

DROP POLICY IF EXISTS tenant_isolation_health_state  ON health.camera_health_state;
DROP POLICY IF EXISTS tenant_isolation_health_events ON health.health_events;

CREATE POLICY org_isolation_health_state ON health.camera_health_state
    AS PERMISSIVE FOR ALL
    USING (org_id = current_setting('app.current_org_id', TRUE)::UUID)
    WITH CHECK (org_id = current_setting('app.current_org_id', TRUE)::UUID);

CREATE POLICY org_isolation_health_events ON health.health_events
    AS PERMISSIVE FOR ALL
    USING (org_id = current_setting('app.current_org_id', TRUE)::UUID)
    WITH CHECK (org_id = current_setting('app.current_org_id', TRUE)::UUID);
