<!-- Meta
Versão: v0.2.0
Última atualização: 2026-06-20
Documentos relacionados:
  - [Arquitetura](./ARCHITECTURE.md)
  - [PRD](./PRD.md)
  - [SDD](./SDD.md)
  - [Data Model](./DATA_MODEL.md)
  - [API Contracts](./API_CONTRACTS.md)
  - [Tech Stack](./TECH_STACK.md)
  - [Media Pipeline](./MEDIA_PIPELINE.md)
  - [Multi-Tenancy](./MULTI_TENANCY.md)
  - [Security LGPD](./SECURITY_LGPD.md)
  - [ENV Config](./ENV_CONFIG.md)
-->

# Implementation Tracker — Cerebro Fofoqueiro 2 {#tracker}

> **Regra de ouro:** Um item só está "concluído" quando **ambas** as caixas estiverem marcadas.
> Claude marca `🤖 impl` ao terminar a implementação. O usuário marca `✅ valid` após testar e aprovar.
> Só então avançamos para o próximo item.

---

## Legenda {#legenda}

| Símbolo | Significado |
|---------|-------------|
| `- [x] 🤖 impl` | Claude finalizou a implementação |
| `- [ ] 🤖 impl` | Claude ainda não implementou |
| `- [x] ✅ valid` | Usuário testou e aprovou |
| `- [ ] ✅ valid` | Aguardando validação do usuário |
| 🔴 **Bloqueado** | Depende de outro item não concluído |
| 🟡 **Em progresso** | Claude está trabalhando nisso agora |

---

## Resumo de Progresso {#resumo}

| Bloco | Total | Impl. | Validado |
|-------|-------|-------|----------|
| 0 — Infraestrutura & Build | 7 | 7 | 0 |
| 1 — Auth Service | 9 | 9 | 0 |
| 1 — Tenant Service | 6 | 6 | 0 |
| 2 — Camera Service | 8 | 8 | 0 |
| 2 — CHMS Service | 4 | 4 | 0 |
| 3 — Alert Service | 5 | 5 | 0 |
| 3 — Recording Service | 5 | 5 | 0 |
| 4 — Notification Service | 3 | 3 | 0 |
| 4 — Audit Service | 4 | 4 | 0 |
| 5 — Frontend Componentes | 7 | 7 | 0 |
| 5 — Frontend Páginas | 7 | 7 | 0 |
| E2E — Verificação End-to-End | 6 | 0 | 0 |
| **TOTAL** | **71** | **65** | **0** |

---

## Bloco 0 — Infraestrutura & Build {#bloco-0}

> Pré-requisito para todos os outros blocos. Sem infra funcionando nada sobe.

### 0.1 — ENCRYPTION_KEY Base64 válida
- [x] 🤖 impl — 2026-06-20 · `docker-compose.yml` ambos os defaults corrigidos para `Y2hhbmdlbWUta2V5LWZvci1kZXYtb25seQ==`
- [ ] ✅ valid

### 0.2 — Flyway V2: colunas `logo_url` e `css_override`
- [x] 🤖 impl — 2026-06-20 · `services/tenant-service/src/main/resources/db/migration/V2__add_missing_tenant_columns.sql`
- [ ] ✅ valid

### 0.3 — MediaMTX: config de auth no formato flat (v1.x)
- [x] 🤖 impl — 2026-06-20 · `infra/mediamtx/mediamtx.yml` — `authMethod: http` + `authHTTPAddress:`
- [ ] ✅ valid

### 0.4 — Frontend: tsconfig `paths` alias `@/*`
- [x] 🤖 impl — 2026-06-20 · `frontend/tsconfig.json` — `"paths": {"@/*": ["./src/*"]}`
- [ ] ✅ valid

### 0.5 — Frontend: type declaration para módulos `.css`
- [x] 🤖 impl — 2026-06-20 · `frontend/src/types/global.d.ts`
- [ ] ✅ valid

### 0.6 — Frontend: mapa Leaflet — CSS estático + tipo `L.Map`
- [x] 🤖 impl — 2026-06-20 · `frontend/src/app/(dashboard)/map/page.tsx`
- [ ] ✅ valid

### 0.7 — Frontend: `next.config.js` — standalone condicional + dedup React
- [x] 🤖 impl — 2026-06-20 · `NEXT_BUILD_STANDALONE=true` no Dockerfile; webpack alias React/ReactDOM
- [ ] ✅ valid

---

## Bloco 1 — Auth Service {#bloco-1-auth}

> Refs: [ARCHITECTURE.md#auth-service](./ARCHITECTURE.md#auth-service) · [SDD.md](./SDD.md) · [API_CONTRACTS.md](./API_CONTRACTS.md) · [SECURITY_LGPD.md](./SECURITY_LGPD.md)

### 1.1 — Entities: `User`, `RefreshToken`, `OutboxEvent`
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 1.2 — Repositories: `UserRepository`, `RefreshTokenRepository`, `OutboxRepository`
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 1.3 — `JwtService` — geração e validação de tokens (JJWT 0.12)
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 1.4 — `TotpService` — RFC 6238, janela ±1, QR Code URL
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 1.5 — `JwtAuthFilter` + `TenantContext` (padrão compartilhado)
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 1.6 — `AuthService` — login, verify2FA, setupTotp, refresh, logout
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 1.7 — `OutboxPublisher` + scheduler de envio para Kafka
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 1.8 — `AuthController` — POST login, 2fa/verify, 2fa/setup, refresh, logout, GET me
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 1.9 — `SecurityConfig` + `@EnableMethodSecurity` + `UserController` (CRUD de usuários)
- [x] 🤖 impl — 2026-06-20 · adicionado `@EnableMethodSecurity` que estava faltando
- [ ] ✅ valid

---

## Bloco 1 — Tenant Service {#bloco-1-tenant}

> Refs: [ARCHITECTURE.md#tenant-service](./ARCHITECTURE.md#tenant-service) · [MULTI_TENANCY.md](./MULTI_TENANCY.md) · [DATA_MODEL.md](./DATA_MODEL.md)

### 1.10 — Entity `Tenant` com enums `TenantPlan` e `TenantStatus`
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 1.11 — `TenantRepository` — `findBySlug`, `findByDomain`
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 1.12 — `TenantService` — CRUD com cache Redis (TTL 5min) e invalidação
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 1.13 — DTOs: `TenantResponse`, `TenantConfigResponse`, `CreateTenantRequest`, `UpdateTenantRequest`
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 1.14 — `TenantController` — GET /me, PUT /me, GET /config (público), admin endpoints
- [x] 🤖 impl — 2026-06-20 · `/api/v1/tenants/config` liberado sem auth no SecurityConfig
- [ ] ✅ valid

### 1.15 — `JwtAuthFilter` + `TenantContext` + `SecurityConfig` + `@EnableMethodSecurity`
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

---

## Bloco 2 — Camera Service {#bloco-2-camera}

> Refs: [ARCHITECTURE.md#camera-service](./ARCHITECTURE.md#camera-service) · [MEDIA_PIPELINE.md](./MEDIA_PIPELINE.md) · [SECURITY_LGPD.md](./SECURITY_LGPD.md)

### 2.1 — Entities: `Camera`, `Location`, `PrivacyZone`
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 2.2 — Repositories: `CameraRepository`, `LocationRepository`, `PrivacyZoneRepository`
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 2.3 — `EncryptionService` — AES-256-GCM para RTSP URL
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 2.4 — `StreamTokenService` / token em campo na entidade Camera (TTL 1h)
- [x] 🤖 impl — 2026-06-20 · token gerado e persistido direto na Camera entity via `CameraService.getStreamUrl()`
- [ ] ✅ valid

### 2.5 — `MediaMtxClient` — REST client para Admin API `:9997`
- [x] 🤖 impl — 2026-06-20 · integrado no `CameraService.registerRtspSourceInMediaMtx()` via RestTemplate
- [ ] ✅ valid

### 2.6 — `CameraService` — CRUD com validação de limite do plano via Redis
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 2.7 — `CameraController` — GET list, POST, GET /{id}, PUT, DELETE, GET /{id}/stream-url
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 2.8 — `MediaAuthController` — POST /internal/media/auth (webhook MediaMTX)
- [x] 🤖 impl — 2026-06-20 · valida token HMAC + TTL do campo `stream_token` da câmera
- [ ] ✅ valid

---

## Bloco 2 — CHMS Service {#bloco-2-chms}

> Refs: [ARCHITECTURE.md#chms-service](./ARCHITECTURE.md#chms-service) · [SDD.md](./SDD.md)

### 2.9 — Entities: `CameraHealthState`, `HealthEvent`
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 2.10 — `ConsistentHashingService` / instância única com `chms.instance-id` configurável
- [x] 🤖 impl — 2026-06-20 · CHMS polling itera sobre todas as câmeras de seu shard
- [ ] ✅ valid

### 2.11 — `MediaMtxPollingService` — `@Scheduled` 30s, mapa em memória, detecção ONLINE/OFFLINE
- [x] 🤖 impl — 2026-06-20 · `@EnableScheduling` na Application class
- [ ] ✅ valid

### 2.12 — `HealthController` — GET /health/cameras, GET /health/cameras/{id}
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

---

## Bloco 3 — Alert Service {#bloco-3-alert}

> Refs: [ARCHITECTURE.md#alert-service](./ARCHITECTURE.md#alert-service) · [API_CONTRACTS.md](./API_CONTRACTS.md)

### 3.1 — Entity `Alert` com enums `AlertType`, `AlertSeverity`, `AlertStatus`
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 3.2 — `WebSocketConfig` — STOMP broker + endpoint `/ws` + SockJS
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 3.3 — `AlertService` — create (idempotente por kafkaEventId), acknowledge, findByTenant
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 3.4 — `AlertBroadcastService` — STOMP push para `/topic/tenant/{id}/alerts`
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 3.5 — `HealthEventConsumer` (Kafka) + `AlertController` — GET /alerts, PATCH /{id}/acknowledge
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

---

## Bloco 3 — Recording Service {#bloco-3-recording}

> Refs: [ARCHITECTURE.md#recording-service](./ARCHITECTURE.md#recording-service) · [MEDIA_PIPELINE.md](./MEDIA_PIPELINE.md)

### 3.6 — Entity `Recording`
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 3.7 — `R2StorageService` — AWS SDK v2 S3Client (MinIO dev / R2 prod), presigned URL
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 3.8 — `RecordingService` — findTimeline, findDownloadUrl, registerRecording
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 3.9 — `RecordingCleanupJob` — `@Scheduled` diário, deleta por retenção do plano
- [x] 🤖 impl — 2026-06-20 · cron `0 0 2 * * *` dentro do `RecordingService`
- [ ] ✅ valid

### 3.10 — `MediaRecordingConsumer` (Kafka) + `RecordingController` — GET /recordings, /timeline, /download-url
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

---

## Bloco 4 — Notification Service {#bloco-4-notification}

> Refs: [ARCHITECTURE.md#notification-service](./ARCHITECTURE.md#notification-service)

### 4.1 — `MailConfig` + `EmailService` — templates HTML inline, JavaMailSender (Mailhog dev)
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 4.2 — `ThrottleService` — Redis: 1 email/câmera/hora
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 4.3 — `AlertEventConsumer` + `AuthEventConsumer` (Kafka)
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

---

## Bloco 4 — Audit Service {#bloco-4-audit}

> Refs: [ARCHITECTURE.md#audit-service](./ARCHITECTURE.md#audit-service) · [SECURITY_LGPD.md](./SECURITY_LGPD.md)

### 4.4 — Entity `AuditLog` com constraint `UNIQUE(event_id)`
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 4.5 — `AuditLogRepository` — queries com filtros (action, userId, from, to)
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 4.6 — `AuditLogService` — `record()` idempotente + `findByTenant()` paginado
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 4.7 — `AuditEventConsumer` (todos os tópicos) + `AuditLogController` — GET /audit-logs
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

---

## Bloco 5 — Frontend: Componentes Base {#bloco-5-components}

> Refs: [TECH_STACK.md](./TECH_STACK.md) · [SDD.md](./SDD.md)

### 5.1 — `Button` — classes Tailwind inline em todos os formulários e ações
- [x] 🤖 impl — 2026-06-20 · botões inline (sem componente separado) em todas as páginas
- [ ] ✅ valid

### 5.2 — `Input` — campos com label, error, tipos em todas as páginas
- [x] 🤖 impl — 2026-06-20 · inputs inline com classes Tailwind + react-hook-form
- [ ] ✅ valid

### 5.3 — `Badge` — status de câmera inline (`ONLINE`/`OFFLINE`/etc.)
- [x] 🤖 impl — 2026-06-20 · badge inline na página de cameras com cores por status
- [ ] ✅ valid

### 5.4 — `Modal` — modals inline com overlay (câmeras, usuários)
- [x] 🤖 impl — 2026-06-20 · modals inline com `fixed inset-0` em cameras/settings
- [ ] ✅ valid

### 5.5 — `Sidebar` — layout do dashboard com navegação completa
- [x] 🤖 impl — 2026-06-20 · `frontend/src/app/(dashboard)/layout.tsx`
- [ ] ✅ valid

### 5.6 — `Header` — email do usuário + logout no rodapé da sidebar
- [x] 🤖 impl — 2026-06-20 · integrado no dashboard layout com `useAuthStore`
- [ ] ✅ valid

### 5.7 — `middleware.ts` — verificação JWT via cookie, redirect `/login`
- [x] 🤖 impl — 2026-06-20 · `frontend/src/middleware.ts`
- [ ] ✅ valid

---

## Bloco 5 — Frontend: Páginas {#bloco-5-pages}

### 5.8 — `/login` — form com tenantSlug, email, senha; Zod; redireciona /2fa se 2FA required
- [x] 🤖 impl — 2026-06-20 · corrigido: user state agora é hidratado no Zustand após login
- [ ] ✅ valid

### 5.9 — `/2fa` — input de 6 dígitos TOTP; redireciona /cameras após sucesso
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 5.10 — `/mosaic` — CSS Grid configurável (1×1 a 4×4), HLS.js por câmera
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 5.11 — `/cameras` — tabela, modal de cadastro/edição, Google Maps input, deletar
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 5.12 — `/map` — Leaflet com marcadores por status, popup com câmera info
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 5.13 — `/alerts` — lista paginada, botão "Reconhecer", toast WebSocket STOMP
- [x] 🤖 impl — 2026-06-20
- [ ] ✅ valid

### 5.14 — `/settings` — abas Organização (white-label), Usuários (CRUD), Segurança (2FA)
- [x] 🤖 impl — 2026-06-20 · corrigido: users data agora usa `UserRecord[]` (plain array)
- [ ] ✅ valid

---

## E2E — Verificação End-to-End {#e2e}

> Só entra nesta seção quando o usuário marcar os blocos anteriores com `✅ valid`.
> Para testar: `docker compose up -d` e acessar `http://localhost`

### E2E.1 — `docker compose up -d` → todos os containers healthy
- [ ] 🤖 impl
- [ ] ✅ valid

### E2E.2 — `POST /api/v1/auth/login` com seed (`admin@demo.com`, slug `demo`) → tokens JWT
- [ ] 🤖 impl
- [ ] ✅ valid

### E2E.3 — `GET /api/v1/cameras` com Bearer token → lista câmeras do seed
- [ ] 🤖 impl
- [ ] ✅ valid

### E2E.4 — `GET /api/v1/alerts` → lista alertas do seed
- [ ] 🤖 impl
- [ ] ✅ valid

### E2E.5 — Frontend em `http://localhost` → tela de login → dashboard com câmeras
- [ ] 🤖 impl
- [ ] ✅ valid

### E2E.6 — WebSocket: conectar em `/ws`, subscrever `/topic/tenant/{id}/alerts`, receber push
- [ ] 🤖 impl
- [ ] ✅ valid

---

## Notas e Decisões de Implementação {#notas}

| Data | Item | Nota |
|------|------|------|
| 2026-06-20 | 0.1 | `_` (underscore) é char URL-safe Base64, inválido no `Base64.getDecoder()` padrão Java |
| 2026-06-20 | 0.2 | Flyway V1 usa `CREATE TABLE IF NOT EXISTS` — no-op quando tabela já existe pelo init script |
| 2026-06-20 | 0.3 | MediaMTX v1.x não aceita bloco `auth:` aninhado; usa campos flat `authMethod:` e `authHTTPAddress:` |
| 2026-06-20 | 0.7 | `output: 'standalone'` no Next.js cria symlinks — proibido no Windows sem Developer Mode; condicional via `NEXT_BUILD_STANDALONE=true` |
| 2026-06-20 | 1.9 | Auth service `SecurityConfig` estava sem `@EnableMethodSecurity` → `@PreAuthorize` nos controllers não funcionava |
| 2026-06-20 | 5.8 | `authData.user` sempre `undefined` — backend retorna `user_id`/`role` flat; login page corrigida para construir User desses campos |
| 2026-06-20 | 5.14 | `UserController` retorna `List<>` puro (array), não `Page` — settings page corrigida para `UserRecord[]` sem `.content` |
| 2026-06-20 | Geral | Backend 8 serviços: todos estavam implementados. Frontend: todas as páginas implementadas. Apenas fixes pontuais necessários. |
