-- @path services/alert-service/src/main/resources/db/migration/V2__rename_tenant_to_org.sql
-- @owner alert-service
-- @responsibility Rename tenant_id → org_id (idempotente)

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema='alerts' AND table_name='alerts' AND column_name='tenant_id') THEN
        ALTER TABLE alerts.alerts RENAME COLUMN tenant_id TO org_id;
    END IF;
END $$;

DROP POLICY IF EXISTS tenant_isolation_alerts ON alerts.alerts;
DROP POLICY IF EXISTS org_isolation_alerts    ON alerts.alerts;

CREATE POLICY org_isolation_alerts ON alerts.alerts
    AS PERMISSIVE FOR ALL
    USING (org_id = current_setting('app.current_org_id', TRUE)::UUID)
    WITH CHECK (org_id = current_setting('app.current_org_id', TRUE)::UUID);
