-- @path services/camera-service/src/main/resources/db/rollback/V1__rollback_camera_tables.sql
-- @owner camera-service
-- @responsibility Rollback manual da V1 — respeita ordem de FKs (privacy_zones → cameras → locations)

DROP TRIGGER IF EXISTS privacy_zones_set_updated_at ON cameras.privacy_zones;
DROP TRIGGER IF EXISTS cameras_set_updated_at ON cameras.cameras;
DROP TRIGGER IF EXISTS locations_set_updated_at ON cameras.locations;
DROP TABLE IF EXISTS cameras.outbox_events;
DROP TABLE IF EXISTS cameras.privacy_zones;
DROP TABLE IF EXISTS cameras.cameras;
DROP TABLE IF EXISTS cameras.locations;
DROP FUNCTION IF EXISTS cameras.trigger_set_updated_at();
