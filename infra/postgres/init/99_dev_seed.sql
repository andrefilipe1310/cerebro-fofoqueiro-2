-- @path infra/postgres/init/99_dev_seed.sql
-- @owner infra
-- @responsibility Dados de desenvolvimento — executado apenas no ambiente dev
-- ATENÇÃO: Este arquivo só deve existir no perfil de desenvolvimento

-- ─── ORGANIZAÇÕES ────────────────────────────────────────────────────────────

INSERT INTO organizations.organizations (id, slug, name, domain, plan, max_cameras, max_users, retention_days, status, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'demo', 'Cliente Demo', 'localhost',
    'PRO', 50, 20, 90, 'ACTIVE', NOW(), NOW()
) ON CONFLICT (slug) DO NOTHING;

-- Segunda org para testar o picker multi-org
INSERT INTO organizations.organizations (id, slug, name, domain, plan, max_cameras, max_users, retention_days, status, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'acme', 'Acme Corp', NULL,
    'STARTER', 10, 5, 30, 'ACTIVE', NOW(), NOW()
) ON CONFLICT (slug) DO NOTHING;

-- ─── USUÁRIOS ─────────────────────────────────────────────────────────────────
-- Sem tenant_id nem role — agora em user_memberships

-- Usuário admin principal (senha: senha123)
INSERT INTO auth.users (id, email, password_hash, totp_enabled, active, failed_attempts, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000012',
    'admin@email.com',
    '$2a$12$.oMH/pPapT76dysY50RLIu3JH2Hhb3x5tG5m/8OBiROVHRbizS/qG',
    false, true, 0, NOW(), NOW()
) ON CONFLICT DO NOTHING;

-- Usuário operador principal (senha: senha123)
INSERT INTO auth.users (id, email, password_hash, totp_enabled, active, failed_attempts, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000013',
    'operador@email.com',
    '$2a$12$.oMH/pPapT76dysY50RLIu3JH2Hhb3x5tG5m/8OBiROVHRbizS/qG',
    false, true, 0, NOW(), NOW()
) ON CONFLICT DO NOTHING;

-- Usuário admin legado (senha: Demo@123456)
INSERT INTO auth.users (id, email, password_hash, totp_enabled, active, failed_attempts, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000010',
    'admin@demo.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LeK5F8FPlv8vA5Hq2',
    false, true, 0, NOW(), NOW()
) ON CONFLICT DO NOTHING;

-- ─── MEMBERSHIPS (N:M user ↔ organization) ────────────────────────────────────

-- admin@email.com → Demo (ADMIN) + Acme (ADMIN) → testa picker multi-org
INSERT INTO auth.user_memberships (user_id, org_id, role, active)
VALUES
    ('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000001', 'ADMIN', true),
    ('00000000-0000-0000-0000-000000000012', '00000000-0000-0000-0000-000000000002', 'ADMIN', true)
ON CONFLICT (user_id, org_id) DO NOTHING;

-- operador@email.com → Demo (OPERATOR)
INSERT INTO auth.user_memberships (user_id, org_id, role, active)
VALUES ('00000000-0000-0000-0000-000000000013', '00000000-0000-0000-0000-000000000001', 'OPERATOR', true)
ON CONFLICT (user_id, org_id) DO NOTHING;

-- admin@demo.com → Demo (ADMIN)
INSERT INTO auth.user_memberships (user_id, org_id, role, active)
VALUES ('00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000001', 'ADMIN', true)
ON CONFLICT (user_id, org_id) DO NOTHING;

-- ─── LOCATION ──────────────────────────────────────────────────────────────────

INSERT INTO cameras.locations (id, org_id, name, address, lat, lng, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000020',
    '00000000-0000-0000-0000-000000000001',
    'Sede SP', 'Av. Paulista, 1000 - São Paulo/SP',
    -23.5632, -46.6544, NOW(), NOW()
) ON CONFLICT DO NOTHING;

-- ─── CÂMERAS ───────────────────────────────────────────────────────────────────

INSERT INTO cameras.cameras (id, org_id, location_id, name, rtsp_url_encrypted, status, lat, lng, ptz_enabled, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000030', '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000020', 'Câmera Entrada',
     'DEV_PLACEHOLDER_ENCRYPTED', 'ONLINE', -23.5630, -46.6540, false, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000031', '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000020', 'Câmera Estacionamento',
     'DEV_PLACEHOLDER_ENCRYPTED', 'ONLINE', -23.5635, -46.6548, false, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000000032', '00000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000020', 'Câmera Corredor',
     'DEV_PLACEHOLDER_ENCRYPTED', 'OFFLINE', -23.5628, -46.6550, true, NOW(), NOW())
ON CONFLICT DO NOTHING;

-- ─── ALERTAS ───────────────────────────────────────────────────────────────────

INSERT INTO alerts.alerts (camera_id, org_id, type, message, severity, triggered_at, kafka_event_id)
VALUES
    ('00000000-0000-0000-0000-000000000032', '00000000-0000-0000-0000-000000000001',
     'CAMERA_OFFLINE', 'Câmera Corredor ficou offline', 'CRITICAL',
     NOW() - INTERVAL '15 minutes', 'seed-event-001'),
    ('00000000-0000-0000-0000-000000000032', '00000000-0000-0000-0000-000000000001',
     'CAMERA_OFFLINE', 'Câmera Corredor continua offline', 'CRITICAL',
     NOW() - INTERVAL '5 minutes', 'seed-event-002')
ON CONFLICT (kafka_event_id) DO NOTHING;

-- ─── AUDIT LOGS ────────────────────────────────────────────────────────────────

INSERT INTO audit.audit_logs (event_id, org_id, user_id, action, resource_type, resource_id, ip_address, occurred_at)
VALUES
    ('seed-audit-001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000012',
     'USER_LOGIN', 'USER', '00000000-0000-0000-0000-000000000012', '127.0.0.1', NOW() - INTERVAL '1 hour'),
    ('seed-audit-002', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000012',
     'VIEW_CAMERA', 'CAMERA', '00000000-0000-0000-0000-000000000030', '127.0.0.1', NOW() - INTERVAL '50 minutes')
ON CONFLICT (event_id) DO NOTHING;

-- ─── HEALTH STATE ──────────────────────────────────────────────────────────────

INSERT INTO health.camera_health_state (camera_id, org_id, status, last_seen_at, recording_confidence_score, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000030', '00000000-0000-0000-0000-000000000001', 'ONLINE', NOW() - INTERVAL '25 seconds', 99.8, NOW()),
    ('00000000-0000-0000-0000-000000000031', '00000000-0000-0000-0000-000000000001', 'ONLINE', NOW() - INTERVAL '28 seconds', 97.5, NOW()),
    ('00000000-0000-0000-0000-000000000032', '00000000-0000-0000-0000-000000000001', 'OFFLINE', NOW() - INTERVAL '20 minutes', 72.3, NOW())
ON CONFLICT (camera_id) DO NOTHING;
