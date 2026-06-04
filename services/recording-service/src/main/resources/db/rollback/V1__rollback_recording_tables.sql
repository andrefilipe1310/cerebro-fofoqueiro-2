-- @path services/recording-service/src/main/resources/db/rollback/V1__rollback_recording_tables.sql
-- @owner recording-service
-- @responsibility Rollback manual da V1

DROP TABLE IF EXISTS recordings.outbox_events;
DROP TABLE IF EXISTS recordings.recordings;
