-- @path services/alert-service/src/main/resources/db/migration/V2__rename_tenant_to_org.sql
-- @owner alert-service
-- @responsibility Renomeia tenant_id → org_id e atualiza políticas RLS

ALTER TABLE alerts.alerts RENAME COLUMN tenant_id TO org_id;

ALTER INDEX IF EXISTS idx_alerts_tenant_id RENAME TO idx_alerts_org_id;

DROP POLICY IF EXISTS tenant_isolation_alerts ON alerts.alerts;

CREATE POLICY org_isolation_alerts ON alerts.alerts
    AS PERMISSIVE FOR ALL
    USING (org_id = current_setting('app.current_org_id', TRUE)::UUID)
    WITH CHECK (org_id = current_setting('app.current_org_id', TRUE)::UUID);
