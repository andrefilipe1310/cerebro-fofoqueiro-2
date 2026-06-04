-- @path services/alert-service/src/main/resources/db/rollback/V1__rollback_alert_tables.sql
-- @owner alert-service
-- @responsibility Rollback manual da V1

DROP TABLE IF EXISTS alerts.outbox_events;
DROP TABLE IF EXISTS alerts.alerts;
