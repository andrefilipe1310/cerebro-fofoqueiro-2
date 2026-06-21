-- @path infra/postgres/init/01_schemas_and_roles.sql
-- @owner infra
-- @responsibility Cria schemas isolados por serviço e roles com menor privilégio
-- @see docs/ARCHITECTURE.md#banco-dados

-- Extensões necessárias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─── SCHEMAS ────────────────────────────────────────────────────────────────
CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS organizations;
CREATE SCHEMA IF NOT EXISTS cameras;
CREATE SCHEMA IF NOT EXISTS health;
CREATE SCHEMA IF NOT EXISTS recordings;
CREATE SCHEMA IF NOT EXISTS alerts;
CREATE SCHEMA IF NOT EXISTS audit;

-- ─── ROLES COM MENOR PRIVILÉGIO ─────────────────────────────────────────────
-- Cada serviço só acessa seu próprio schema (enforcement pelo banco, não por convenção)

DO $$ BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'auth_service_user') THEN
        CREATE ROLE auth_service_user WITH LOGIN PASSWORD 'changeme_auth';
    END IF;
END $$;
GRANT ALL ON SCHEMA auth TO auth_service_user;
GRANT USAGE ON SCHEMA auth TO auth_service_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA auth GRANT ALL ON TABLES TO auth_service_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA auth GRANT ALL ON SEQUENCES TO auth_service_user;
-- auth-service lê dados básicos de organizations para popular o org picker no login
GRANT USAGE ON SCHEMA organizations TO auth_service_user;

DO $$ BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'org_service_user') THEN
        CREATE ROLE org_service_user WITH LOGIN PASSWORD 'changeme_org';
    END IF;
END $$;
GRANT ALL ON SCHEMA organizations TO org_service_user;
GRANT USAGE ON SCHEMA organizations TO org_service_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA organizations GRANT ALL ON TABLES TO org_service_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA organizations GRANT ALL ON SEQUENCES TO org_service_user;

DO $$ BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'camera_service_user') THEN
        CREATE ROLE camera_service_user WITH LOGIN PASSWORD 'changeme_camera';
    END IF;
END $$;
GRANT ALL ON SCHEMA cameras TO camera_service_user;
GRANT USAGE ON SCHEMA cameras TO camera_service_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA cameras GRANT ALL ON TABLES TO camera_service_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA cameras GRANT ALL ON SEQUENCES TO camera_service_user;

DO $$ BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'chms_service_user') THEN
        CREATE ROLE chms_service_user WITH LOGIN PASSWORD 'changeme_chms';
    END IF;
END $$;
GRANT ALL ON SCHEMA health TO chms_service_user;
GRANT USAGE ON SCHEMA health TO chms_service_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA health GRANT ALL ON TABLES TO chms_service_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA health GRANT ALL ON SEQUENCES TO chms_service_user;

DO $$ BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'recording_service_user') THEN
        CREATE ROLE recording_service_user WITH LOGIN PASSWORD 'changeme_recording';
    END IF;
END $$;
GRANT ALL ON SCHEMA recordings TO recording_service_user;
GRANT USAGE ON SCHEMA recordings TO recording_service_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA recordings GRANT ALL ON TABLES TO recording_service_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA recordings GRANT ALL ON SEQUENCES TO recording_service_user;

DO $$ BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'alert_service_user') THEN
        CREATE ROLE alert_service_user WITH LOGIN PASSWORD 'changeme_alert';
    END IF;
END $$;
GRANT ALL ON SCHEMA alerts TO alert_service_user;
GRANT USAGE ON SCHEMA alerts TO alert_service_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA alerts GRANT ALL ON TABLES TO alert_service_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA alerts GRANT ALL ON SEQUENCES TO alert_service_user;

-- audit_service_user: INSERT + SELECT apenas — nunca UPDATE/DELETE (ADR-010)
DO $$ BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'audit_service_user') THEN
        CREATE ROLE audit_service_user WITH LOGIN PASSWORD 'changeme_audit';
    END IF;
END $$;
GRANT USAGE ON SCHEMA audit TO audit_service_user;
-- Permissões granulares aplicadas na migration V1 do audit-service

-- ─── ISOLAMENTO CRUZADO ──────────────────────────────────────────────────────
-- Nenhum serviço acessa schema de outro serviço
-- auth_service_user tem USAGE em organizations (read-only via GRANT abaixo após criação das tabelas)
REVOKE ALL ON SCHEMA auth          FROM org_service_user, camera_service_user, chms_service_user, recording_service_user, alert_service_user, audit_service_user;
REVOKE ALL ON SCHEMA organizations FROM camera_service_user, chms_service_user, recording_service_user, alert_service_user, audit_service_user;
REVOKE ALL ON SCHEMA cameras       FROM auth_service_user, org_service_user, chms_service_user, recording_service_user, alert_service_user, audit_service_user;
REVOKE ALL ON SCHEMA health        FROM auth_service_user, org_service_user, camera_service_user, recording_service_user, alert_service_user, audit_service_user;
REVOKE ALL ON SCHEMA recordings    FROM auth_service_user, org_service_user, camera_service_user, chms_service_user, alert_service_user, audit_service_user;
REVOKE ALL ON SCHEMA alerts        FROM auth_service_user, org_service_user, camera_service_user, chms_service_user, recording_service_user, audit_service_user;
