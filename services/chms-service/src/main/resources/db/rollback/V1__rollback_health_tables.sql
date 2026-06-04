-- @path services/chms-service/src/main/resources/db/rollback/V1__rollback_health_tables.sql
-- @owner chms-service
-- @responsibility Rollback manual da V1

DROP TABLE IF EXISTS health.outbox_events;
DROP TABLE IF EXISTS health.health_events;
DROP TABLE IF EXISTS health.camera_health_state;
