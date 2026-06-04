-- @path infra/postgres/init/99_dev_seed.sql
-- @owner infra
-- @responsibility Dados de desenvolvimento — executado apenas no ambiente dev
-- ATENÇÃO: Este arquivo só deve existir no perfil de desenvolvimento
-- @see docs/DATA_MODEL.md

-- Tenant demo
INSERT INTO tenants.tenants (id, slug, name, domain, plan, max_cameras, max_users, retention_days, status, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'demo', 'Cliente Demo', 'localhost',
    'PRO', 50, 20, 90, 'ACTIVE', NOW(), NOW()
) ON CONFLICT (slug) DO NOTHING;

-- Usuário admin (senha: Demo@123456 — hash bcrypt strength 12)
-- IMPORTANTE: Gere o hash real com: htpasswd -bnBC 12 "" 'Demo@123456' | tr -d ':\n' | sed 's/$2y/$2a/'
INSERT INTO auth.users (id, tenant_id, email, password_hash, role, totp_enabled, active, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000010',
    '00000000-0000-0000-0000-000000000001',
    'admin@demo.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LeK5F8FPlv8vA5Hq2',
    'ADMIN', false, true, NOW(), NOW()
) ON CONFLICT DO NOTHING;

-- Usuário operador
INSERT INTO auth.users (id, tenant_id, email, password_hash, role, totp_enabled, active, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000011',
    '00000000-0000-0000-0000-000000000001',
    'operador@demo.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LeK5F8FPlv8vA5Hq2',
    'OPERATOR', false, true, NOW(), NOW()
) ON CONFLICT DO NOTHING;

-- Location
INSERT INTO cameras.locations (id, tenant_id, name, address, lat, lng, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000020',
    '00000000-0000-0000-0000-000000000001',
    'Sede SP', 'Av. Paulista, 1000 - São Paulo/SP',
    -23.5632, -46.6544, NOW(), NOW()
) ON CONFLICT DO NOTHING;

-- 3 câmeras fictícias (rtsp_url_encrypted = placeholder — não decriptável em dev)
INSERT INTO cameras.cameras (id, tenant_id, location_id, name, rtsp_url_encrypted, status, lat, lng, ptz_enabled, created_at, updated_at)
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

-- 2 alertas não reconhecidos (câmera offline)
INSERT INTO alerts.alerts (camera_id, tenant_id, type, message, severity, triggered_at, kafka_event_id)
VALUES
    ('00000000-0000-0000-0000-000000000032', '00000000-0000-0000-0000-000000000001',
     'CAMERA_OFFLINE', 'Câmera Corredor ficou offline', 'CRITICAL',
     NOW() - INTERVAL '15 minutes', 'seed-event-001'),
    ('00000000-0000-0000-0000-000000000032', '00000000-0000-0000-0000-000000000001',
     'CAMERA_OFFLINE', 'Câmera Corredor continua offline', 'CRITICAL',
     NOW() - INTERVAL '5 minutes', 'seed-event-002')
ON CONFLICT (kafka_event_id) DO NOTHING;

-- 5 audit logs de exemplo
INSERT INTO audit.audit_logs (event_id, tenant_id, user_id, action, resource_type, resource_id, ip_address, occurred_at)
VALUES
    ('seed-audit-001', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000010',
     'USER_LOGIN', 'USER', '00000000-0000-0000-0000-000000000010', '127.0.0.1', NOW() - INTERVAL '1 hour'),
    ('seed-audit-002', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000010',
     'VIEW_CAMERA', 'CAMERA', '00000000-0000-0000-0000-000000000030', '127.0.0.1', NOW() - INTERVAL '50 minutes'),
    ('seed-audit-003', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000011',
     'USER_LOGIN', 'USER', '00000000-0000-0000-0000-000000000011', '192.168.1.10', NOW() - INTERVAL '30 minutes'),
    ('seed-audit-004', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000011',
     'PTZ_COMMAND', 'CAMERA', '00000000-0000-0000-0000-000000000032', '192.168.1.10', NOW() - INTERVAL '20 minutes'),
    ('seed-audit-005', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000010',
     'ALERT_ACKNOWLEDGED', 'ALERT', 'seed-event-001', '127.0.0.1', NOW() - INTERVAL '10 minutes')
ON CONFLICT (event_id) DO NOTHING;

-- Health state para as câmeras
INSERT INTO health.camera_health_state (camera_id, tenant_id, status, last_seen_at, recording_confidence_score, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000030', '00000000-0000-0000-0000-000000000001', 'ONLINE', NOW() - INTERVAL '25 seconds', 99.8, NOW()),
    ('00000000-0000-0000-0000-000000000031', '00000000-0000-0000-0000-000000000001', 'ONLINE', NOW() - INTERVAL '28 seconds', 97.5, NOW()),
    ('00000000-0000-0000-0000-000000000032', '00000000-0000-0000-0000-000000000001', 'OFFLINE', NOW() - INTERVAL '20 minutes', 72.3, NOW())
ON CONFLICT (camera_id) DO NOTHING;
