<!-- Meta
Versão: v0.2.0
Última atualização: 2026-06-04
Documentos relacionados:
  - [Tech Stack](./TECH_STACK.md)
  - [Media Pipeline](./MEDIA_PIPELINE.md)
  - [Multi-Tenancy](./MULTI_TENANCY.md)
  - [Data Model](./DATA_MODEL.md)
  - [API Contracts](./API_CONTRACTS.md)
  - [ENV Config](./ENV_CONFIG.md)
  - [Security LGPD](./SECURITY_LGPD.md)
-->

# Arquitetura do Sistema — Microserviços {#architecture}

## 1. Visão Geral {#visao-geral}

```
╔═══════════════════════════════════════════════════════════════════════════╗
║                          CLIENTS / EDGE                                   ║
║  Browser (Next.js)  │  Mobile (futuro)  │  Integrações API                ║
╚════════════════════════════╤══════════════════════════════════════════════╝
                             │ HTTPS/WSS (TLS 1.3 obrigatório)
              ╔══════════════▼══════════════╗
              ║       CLOUDFLARE CDN         ║  ← WAF, DDoS, TLS
              ║  (terminação TLS + CDN HLS)  ║  ← Cache de HLS segments
              ╚══════════════╤══════════════╝
                             │
              ╔══════════════▼══════════════╗
              ║        API GATEWAY           ║  ← Kong / AWS API GW
              ║  • Roteamento por serviço    ║  ← JWT validation (único ponto)
              ║  • Rate limiting por tenant  ║  ← mTLS para serviços internos
              ║  • WAF rules adicionais      ║  ← Request ID injection
              ║  • Tenant header injection   ║
              ╚══╤════════════════════════╤═╝
                 │                        │
   ┌─────────────┘                        └──────────────┐
   │  REST /api/v1/*                      │  WS /ws/*     │
   │                                      │               │
╔══▼══════════╗  ╔═══════════╗  ╔════════▼═════════╗     │
║ AUTH        ║  ║  TENANT   ║  ║  ALERT SERVICE   ║     │
║ SERVICE     ║  ║  SERVICE  ║  ║  (WebSocket hub) ║     │
╚══╤══════════╝  ╚═══╤═══════╝  ╚════════╤═════════╝     │
   │                 │                   │               │
╔══▼═════════════════▼═══════════════════▼═══════════════▼══════════╗
║                    KAFKA (Message Broker)                           ║
║  Tópicos: auth.events │ camera.events │ health.events              ║
║           alert.events │ audit.events │ recording.events           ║
╚══╤══════════════╤═══════════╤══════════════╤════════════╤══════════╝
   │              │           │              │            │
╔══▼══════╗  ╔═══▼═════╗  ╔══▼══════╗  ╔═══▼═════╗  ╔══▼══════╗
║ CAMERA  ║  ║  CHMS   ║  ║RECORDING║  ║  NOTIF. ║  ║  AUDIT  ║
║ SERVICE ║  ║ SERVICE ║  ║ SERVICE ║  ║ SERVICE ║  ║ SERVICE ║
╚══╤══════╝  ╚═══╤═════╝  ╚══╤══════╝  ╚═════════╝  ╚══╤══════╝
   │             │            │                          │
╔══▼═════════════▼════════════▼══════════════════════════▼══════════╗
║          PostgreSQL 16 (schema por serviço) + Redis Cluster        ║
╚═══════════════════════════════════════════════════════════════════╝
                             │
╔════════════════════════════▼══════════════════════════════════════╗
║                     MEDIA LAYER                                    ║
║  MediaMTX Cluster (multi-região)  │  FFmpeg Workers               ║
║  ← RTSP ingestion                 │  ← Transcodificação/segm. HLS ║
╚═══════════════════════════════════╤══════════════════════════════╝
                                    │ Upload
                        ╔═══════════▼═══════════╗
                        ║    Cloudflare R2       ║
                        ║  (gravações + thumbs)  ║
                        ╚═══════════════════════╝
                             ▲
                             │ RTSP
╔════════════════════════════╧══════════════════════════════════════╗
║                     CÂMERAS IP (clientes)                          ║
╚═══════════════════════════════════════════════════════════════════╝
```

---

## 2. Decomposição em Microserviços {#microservicos}

Cada serviço tem **responsabilidade única**, **banco de dados próprio** (schema isolado no PostgreSQL compartilhado no MVP, instâncias separadas em produção plena) e **se comunica via Kafka para eventos e REST/gRPC para queries síncronas**.

### 2.1 API Gateway {#api-gateway}

**Tecnologia:** Kong OSS ou AWS API Gateway + Lambda Authorizer

| Responsabilidade | Detalhe |
|---|---|
| **Único ponto de entrada** | Nenhum serviço interno é acessível diretamente de fora |
| **Validação JWT** | Valida assinatura e expiração UMA VEZ — serviços internos confiam no header `X-Tenant-Id` injetado pelo gateway |
| **Rate limiting** | Por tenant (plano FREE = 100 req/min, PRO = 2000 req/min) usando Redis Cluster |
| **WAF** | Bloqueia SQLi, XSS, path traversal antes de chegar nos serviços |
| **mTLS interno** | Injeta certificado de cliente para serviços internos verificarem origem |
| **Request ID** | Injeta `X-Request-Id` único em cada request para rastreamento distribuído |
| **Roteamento** | `/api/v1/auth/*` → Auth Service, `/api/v1/cameras/*` → Camera Service, etc. |

> **ADR-001 (Segurança):** JWT é validado APENAS no API Gateway. Serviços internos NÃO reimplementam validação — eles confiam no `X-Tenant-Id` e `X-User-Id` injetados pelo gateway após validação. Isso evita drift de lógica de auth entre serviços e centraliza revogação.

> **ADR-002 (Performance):** Rate limiting feito no gateway com Redis (não no banco) garante O(1) por request, sem query ao PostgreSQL. Limite por tenant é carregado em cache com TTL de 5 minutos.

---

### 2.2 Auth Service {#auth-service}

**Stack:** Spring Boot 3 / Java 21 | **Banco:** PostgreSQL schema `auth`

| Responsabilidade | Detalhe |
|---|---|
| Login com email/senha | bcrypt verification, emissão de JWT |
| 2FA TOTP | Setup de secret, verificação de código, backup codes |
| Refresh token | Armazenado no Redis com TTL de 7 dias |
| Blacklist de tokens | Tokens revogados armazenados no Redis até expirar |
| Eventos publicados | `auth.login.success`, `auth.login.failure`, `auth.logout` → Kafka → Audit Service |

**Escalonamento:** Stateless — escala horizontal livremente. O estado de sessão fica no Redis.

> **ADR-003 (Segurança):** Refresh tokens são armazenados como **hash SHA-256** no Redis, nunca em texto claro. Em caso de comprometimento do Redis, o token original não é recuperável. O token real fica apenas no HttpOnly Cookie do browser.

---

### 2.3 Tenant Service {#tenant-service}

**Stack:** Spring Boot 3 / Java 21 | **Banco:** PostgreSQL schema `tenants`

| Responsabilidade | Detalhe |
|---|---|
| CRUD de tenants e usuários | Criação, suspensão, alteração de plano |
| Enforcement de limites de plano | Câmeras máx., usuários máx., retenção |
| White-label config | Logo, CSS override, domínio customizado |
| Cache de configuração | Config do tenant em Redis com TTL de 5min — consultado por outros serviços |
| Eventos publicados | `tenant.created`, `tenant.plan_changed`, `tenant.suspended` → Kafka |

> **ADR-004 (Performance):** A configuração de cada tenant (plano, limites, CSS) é cacheada no Redis. O Camera Service e Alert Service consultam o Redis — nunca o banco do Tenant Service diretamente. Isso elimina dependência síncrona entre serviços em tempo de request.

---

### 2.4 Camera Service {#camera-service}

**Stack:** Spring Boot 3 / Java 21 | **Banco:** PostgreSQL schema `cameras`

| Responsabilidade | Detalhe |
|---|---|
| CRUD de câmeras e localizações | Criação, edição, exclusão |
| Geração de stream URL | Consulta MediaMTX API, retorna URL WHEP/HLS assinada |
| Zonas de privacidade | CRUD de polígonos de mascaramento |
| Webhook de auth para MediaMTX | Endpoint interno validado por mTLS |
| Eventos publicados | `camera.created`, `camera.deleted`, `camera.rtsp_updated` → Kafka |

> **ADR-005 (Segurança):** A URL RTSP de cada câmera é criptografada com AES-256-GCM no banco (`rtsp_url_encrypted`). A chave de criptografia (`ENCRYPTION_KEY`) é gerenciada pelo **Secrets Manager** (AWS Secrets Manager ou HashiCorp Vault) — nunca em variáveis de ambiente em produção. O Camera Service busca a chave na inicialização via SDK do Secrets Manager com cache em memória.

---

### 2.5 CHMS Service — Camera Health Monitoring {#chms-service}

**Stack:** Spring Boot 3 / Java 21 | **Banco:** PostgreSQL schema `health`

Este é o serviço mais crítico para a proposta de valor da plataforma. Ele é **completamente separado** para poder escalar e ser monitorado independentemente.

| Responsabilidade | Detalhe |
|---|---|
| Polling do MediaMTX | GET `/v3/paths/list` a cada 30s por instância |
| Particionamento de câmeras | Cada instância do CHMS é responsável por um range de câmeras (via consistent hashing) |
| Detecção de offline | Câmera sem atividade por 2 ciclos (60s) → evento gerado |
| Recording Confidence Score | Calculado diariamente, armazenado por câmera |
| Eventos publicados | `camera.went_offline`, `camera.came_online`, `camera.low_confidence` → Kafka |

**Escalonamento horizontal:** Com 1000 câmeras e polling de 30s, uma instância é suficiente. Com 10.000 câmeras, 5 instâncias dividem a carga via consistent hashing. O HPA do Kubernetes escala com base na métrica customizada `cameras_per_instance`.

> **ADR-006 (Performance):** O CHMS não consulta o banco a cada ciclo de polling para comparar status. Ele mantém um **mapa em memória** do status atual de todas as câmeras do seu range, atualizado a cada tick. A escrita no banco ocorre APENAS quando há mudança de estado (ONLINE→OFFLINE ou vice-versa). Isso reduz writes no PostgreSQL de N câmeras/30s para apenas as câmeras que mudaram de estado.

---

### 2.6 Recording Service {#recording-service}

**Stack:** Spring Boot 3 / Java 21 | **Banco:** PostgreSQL schema `recordings`

| Responsabilidade | Detalhe |
|---|---|
| Registro de metadados | Inicia/fecha registros de segmentos à medida que chegam do FFmpeg Worker |
| Timeline API | Query otimizada por `(camera_id, started_at, ended_at)` com índice composto |
| Download URL | Gera signed URL do R2 com TTL de 15 minutos |
| Cleanup de retenção | Job diário deleta registros e objetos R2 expirados por plano |
| Eventos consumidos | `camera.went_offline` → marca gap na timeline |

> **ADR-007 (Performance):** Queries de timeline são servidas por uma **read replica** do PostgreSQL. A timeline pode ser consultada para janelas de 24h a 7 dias. Índice composto `(camera_id, started_at DESC, ended_at)` garante que a query use index scan mesmo para câmeras com anos de gravação.

---

### 2.7 Alert Service {#alert-service}

**Stack:** Spring Boot 3 / Java 21 | **Banco:** PostgreSQL schema `alerts` | **Redis:** pub/sub de WebSocket

| Responsabilidade | Detalhe |
|---|---|
| Consumo de eventos de saúde | Consome `camera.went_offline`, `camera.low_confidence` do Kafka |
| Criação de alertas | INSERT em `alerts` com tipo, mensagem e timestamp |
| Dispatch em tempo real | Publica no Redis canal `tenant:{id}:alerts` → WebSocket hub |
| WebSocket hub | Mantém conexões STOMP ativas, subscritas por tenant |
| ACK de alertas | PATCH `alerts.acknowledged_at` quando operador reconhece |

**Escalonamento de WebSocket:** Múltiplas instâncias do Alert Service compartilham o Redis pub/sub. Qualquer instância pode receber o evento do Kafka e publicar no Redis — qualquer instância com conexão WebSocket do tenant entrega a mensagem.

> **ADR-008 (Segurança + Performance):** WebSocket connections são autenticadas via JWT passado como query param no handshake (`/ws?token=...`). O token é validado na conexão e o `tenant_id` é extraído e fixado na sessão — o cliente não pode mudar de tenant sem reconectar. Conexões sem tenant válido são fechadas imediatamente com código 4001.

---

### 2.8 Notification Service {#notification-service}

**Stack:** Spring Boot 3 / Java 21 | **Sem banco próprio** (stateless)

| Responsabilidade | Detalhe |
|---|---|
| Consome eventos do Kafka | `camera.went_offline`, `tenant.created`, `auth.login.failure` |
| E-mail via SendGrid | Templates HTML por tipo de evento |
| SMS (pós-MVP) | Twilio para alertas CRITICAL |
| Throttling por tenant | Não envia mais de 1 e-mail/câmera/hora (estado no Redis) |

> **ADR-009 (Performance):** Throttling de notificações evita spam em caso de câmera oscilando (offline/online ciclicamente). Estado de throttle armazenado no Redis: `notify:camera:{id}:last_email` com TTL de 1 hora. Se a chave existe, suprime o e-mail.

---

### 2.9 Audit Service {#audit-service}

**Stack:** Spring Boot 3 / Java 21 | **Banco:** PostgreSQL schema `audit` (append-only)

| Responsabilidade | Detalhe |
|---|---|
| Consome audit events do Kafka | Todas as ações de todos os serviços publicam `audit.event` |
| Escrita imutável | INSERT apenas — trigger PostgreSQL bloqueia UPDATE/DELETE |
| Query de auditoria | GET `/audit-logs` com filtros (apenas ADMIN) |
| Exportação CSV | Stream de resultado sem carregar tudo em memória (JPA Scrollable) |

> **ADR-010 (Segurança):** O Audit Service é o único que escreve na tabela `audit_logs`. Nenhum outro serviço tem acesso de escrita ao schema `audit`. Isso é enforcement por política de banco (role do PostgreSQL), não apenas por convenção. Em caso de comprometimento de outro serviço, os logs de auditoria permanecem intactos.

---

## 3. Comunicação Entre Serviços {#comunicacao}

### 3.1 Síncrona (REST / gRPC)

Usado para queries que precisam de resposta imediata no fluxo de request do usuário.

| Chamada | Origem | Destino | Protocolo |
|---|---|---|---|
| Buscar config do tenant | Camera Service | Redis cache (preenchido pelo Tenant Service) | Redis GET |
| Validar limite de câmeras | Camera Service | Redis cache do Tenant Service | Redis GET |
| Gerar stream URL | Camera Service | MediaMTX Admin API | REST HTTP |
| Auth webhook de stream | MediaMTX | Camera Service (`/internal/media/auth`) | REST HTTP + mTLS |
| Buscar status de câmera | Alert Service | Redis (preenchido pelo CHMS) | Redis GET |

### 3.2 Assíncrona (Kafka)

Usado para eventos de domínio onde o produtor não precisa esperar o consumidor.

```
Tópico                  │ Produtor         │ Consumidores
────────────────────────┼──────────────────┼──────────────────────────────
auth.events             │ Auth Service     │ Audit Service
camera.events           │ Camera Service   │ CHMS Service, Audit Service
health.events           │ CHMS Service     │ Alert Service, Recording Service
alert.events            │ Alert Service    │ Notification Service, Audit Service
recording.events        │ Recording Svc    │ Audit Service
tenant.events           │ Tenant Service   │ CHMS Service, Audit Service
```

> **ADR-011 (Resiliência):** Kafka garante entrega at-least-once. Serviços consumidores devem ser **idempotentes** — processar o mesmo evento duas vezes não deve causar efeito duplicado. Exemplo: Audit Service usa `event_id` como chave única para evitar log duplicado.

---

## 4. Estratégia de Banco de Dados {#banco-dados}

### 4.1 Isolamento por Schema (MVP → Produção)

```
PostgreSQL 16 (instância única no MVP, cluster no prod)
├── schema: auth          → Auth Service
├── schema: tenants       → Tenant Service
├── schema: cameras       → Camera Service
├── schema: health        → CHMS Service
├── schema: recordings    → Recording Service
├── schema: alerts        → Alert Service
└── schema: audit         → Audit Service (append-only, sem UPDATE/DELETE)
```

Cada serviço tem **credenciais exclusivas** que só acessam seu schema. O banco enforce isso via `GRANT`:

```sql
-- Camera Service só acessa schema cameras
GRANT ALL ON SCHEMA cameras TO camera_service_user;
REVOKE ALL ON SCHEMA audit, auth, tenants FROM camera_service_user;

-- Audit Service só pode INSERT, nunca UPDATE/DELETE
GRANT INSERT, SELECT ON audit.audit_logs TO audit_service_user;
REVOKE UPDATE, DELETE ON audit.audit_logs FROM audit_service_user;
```

### 4.2 Read Replicas para Performance

| Serviço | Primary | Read Replica |
|---|---|---|
| Recording Service | Writes de novos segmentos | Timeline queries (7 dias) |
| Audit Service | Writes de eventos | Queries de admin |
| Camera Service | CRUD | Listagem do mosaico (alta frequência) |

> **ADR-012 (Performance):** Recording Service usa read replica para timeline. Uma query de timeline de 7 dias para uma câmera com segmentos de 6s retorna ~100.800 linhas — deve ir para a replica para não afetar o primary.

---

## 5. Segurança em Profundidade {#seguranca}

### 5.1 Modelo Zero Trust Interno

```
Internet → Cloudflare (WAF + DDoS) → API Gateway (JWT + Rate limit)
         ↓ (mTLS para tráfego interno)
Serviço A → Serviço B: verifica certificado de cliente
         ↓ (Kubernetes NetworkPolicy)
Camera Service NÃO pode falar com Audit Service diretamente
Camera Service SÓ pode falar com: Redis, PostgreSQL (schema cameras), MediaMTX
```

**Implementação com Istio (Service Mesh):**
- mTLS automático entre todos os pods
- `AuthorizationPolicy` define quais serviços podem chamar quais
- Tráfego não-autorizado é bloqueado no sidecar Envoy antes de chegar ao container

### 5.2 Gestão de Secrets

| Ambiente | Mecanismo |
|---|---|
| Desenvolvimento | `.env` local (git-ignored) |
| Staging | Kubernetes Secrets + Sealed Secrets |
| Produção | AWS Secrets Manager / HashiCorp Vault com rotação automática |

Secrets críticos com rotação obrigatória a cada 90 dias:
- `JWT_SECRET` — rotação transparente: aceita token assinado com chave anterior por 15 minutos após rotação
- `ENCRYPTION_KEY` — rotação com re-encriptação progressiva (background job)
- Credenciais de banco por serviço

### 5.3 Superfície de Ataque Reduzida

| Medida | Implementação |
|---|---|
| Serviços internos sem porta pública | Kubernetes Service do tipo `ClusterIP` (sem LoadBalancer) |
| MediaMTX com auth obrigatório | Webhook valida todo acesso — paths sem token retornam 401 |
| rtsp_url nunca em logs | `@JsonIgnore` + custom Logback serializer |
| Audit log de falhas de auth | Login com senha errada → `auth.login.failure` → Audit Service |
| CORS restrito | Apenas origens explicitamente listadas em `CORS_ALLOWED_ORIGINS` |

---

## 6. Performance e Escalonamento {#performance}

### 6.1 HPA (Horizontal Pod Autoscaler) por Serviço

| Serviço | Métrica de Scale | Min pods | Max pods |
|---|---|---|---|
| API Gateway (Kong) | CPU > 60% | 2 | 10 |
| Auth Service | CPU > 70% | 2 | 6 |
| Camera Service | CPU > 60% | 2 | 8 |
| CHMS Service | `cameras_monitored / 2000` | 1 | 20 |
| Alert Service | Lag de consumer Kafka > 1000 | 2 | 8 |
| Recording Service | CPU > 60% | 2 | 6 |
| MediaMTX | Conexões WebRTC ativas > 500/instância | 2 | 50 |
| Notification Service | Lag de consumer Kafka > 500 | 1 | 4 |

### 6.2 Estratégia de Cache em Camadas

```
Browser Cache (React Query)         → staleTime: 30s para lista de câmeras
      ↓ (miss)
Cloudflare CDN                      → HLS segments e thumbnails (cache por URL)
      ↓ (miss)
Redis (API Gateway / Serviços)      → Config do tenant: TTL 5min
                                    → Status de câmeras: TTL 30s
                                    → Rate limit counters: TTL 1min
      ↓ (miss)
PostgreSQL Read Replica             → Timeline, listagem de câmeras
      ↓ (miss)
PostgreSQL Primary                  → Writes e dados críticos recentes
```

### 6.3 MediaMTX — Escalonamento da Camada de Mídia

```
                     ┌─────────────────────────────┐
                     │   Load Balancer (L4/UDP)     │
                     │   (sticky session por        │
                     │    camera_id para WebRTC)    │
                     └──────┬────────────┬──────────┘
                            │            │
                 ┌──────────▼──┐    ┌────▼──────────┐
                 │ MediaMTX #1 │    │  MediaMTX #2  │
                 │ (região SP) │    │ (região RJ)   │
                 │ câmeras 1-N │    │ câmeras N+1-M │
                 └─────────────┘    └───────────────┘
```

**Sticky session por `camera_id`:** O WebRTC não suporta failover transparente. O load balancer deve rotear sempre o mesmo `camera_id` para a mesma instância MediaMTX (consistent hashing). Se a instância cair, o frontend reconecta automaticamente (o player tenta por até 10s).

> **ADR-013 (Performance):** MediaMTX não transcodifica por padrão (`-c:v copy`). Transcodificação só ocorre quando a câmera envia codec incompatível com WebRTC (ex: H.265 e o browser não suporta). Transcodificação aumenta CPU em ~4x — instâncias com GPU (NVIDIA T4) são usadas apenas para câmeras que exigem transcodificação.

---

## 7. Observabilidade {#observabilidade}

### 7.1 Stack de Observabilidade

| Camada | Tecnologia | O que monitora |
|---|---|---|
| **Métricas** | Prometheus + Grafana | CPU/memória por serviço, lag do Kafka, conexões WebRTC ativas, câmeras offline |
| **Logs** | Loki + Grafana | Logs estruturados (JSON) de todos os serviços, correlacionados por `X-Request-Id` |
| **Tracing** | OpenTelemetry + Tempo | Trace distribuído de um request do browser até o banco |
| **Alertas** | Alertmanager + PagerDuty | CHMS: taxa de câmeras offline > 5% do tenant → PagerDuty |
| **Uptime** | Uptime Robot / Better Uptime | Endpoints públicos críticos a cada 1 minuto |

### 7.2 Métricas Críticas de Negócio (Grafana)

```
Dashboard: Saúde da Plataforma
├── Câmeras online vs offline (por tenant, por região)
├── Latência WebRTC p50/p95/p99 (via stats API do browser → backend)
├── Recording Confidence Score médio (por tenant)
├── Alertas não reconhecidos > 5 minutos
└── Erros 5xx por serviço (taxa)

Dashboard: Mídia
├── Conexões WebRTC ativas por instância MediaMTX
├── Bitrate médio por câmera
├── Taxa de fallback HLS (WebRTC → HLS)
└── Upload rate para R2 (bytes/s)
```

---

## 8. Deploy em Cloud (Kubernetes) {#deploy}

### 8.1 Estrutura de Namespaces

```
Kubernetes Cluster
├── namespace: ingress     → Nginx Ingress + Cert-Manager
├── namespace: gateway     → Kong API Gateway
├── namespace: core        → Auth, Tenant, Camera Services
├── namespace: media       → Alert, CHMS, Recording Services
├── namespace: streaming   → MediaMTX cluster
├── namespace: messaging   → Kafka + Zookeeper (ou MSK/Confluent Cloud)
├── namespace: data        → PostgreSQL (CrunchyData PGO) + Redis (Operator)
├── namespace: observ      → Prometheus, Grafana, Loki, Tempo
└── namespace: workers     → FFmpeg Workers, Notification, Audit Services
```

### 8.2 Roteamento por Domínio

| Domínio | Destino |
|---|---|
| `app.{produto}.com` | Next.js (Vercel ou pod) |
| `api.{produto}.com` | API Gateway → Serviços |
| `media.{produto}.com` | MediaMTX Cluster (WebRTC + HLS) |
| `{tenant-slug}.{produto}.com` | Next.js com config do tenant (white-label) |
| `monitor.cliente.com` | Nginx Ingress mapeia para Next.js com tenant pelo domínio |

### 8.3 CI/CD Pipeline (GitHub Actions)

```
Push para main
    │
    ├── 1. Tests (unit + integration por serviço)
    ├── 2. Security scan (Snyk + Trivy para imagens Docker)
    ├── 3. Build Docker image (tag: SHA do commit)
    ├── 4. Push para ECR / GHCR
    └── 5. Deploy Kubernetes (Helm chart por serviço)
           ├── staging: automático
           └── production: aprovação manual no GitHub
```

---

## 9. Fluxos de Dados Críticos (revisados) {#fluxos}

### 9.1 Live View — RTSP → WebRTC (< 500ms) {#fluxo-live}

```
Browser
  │ GET /api/v1/cameras/{id}/stream/live (com JWT)
  ▼
API Gateway (valida JWT, injeta X-Tenant-Id, X-User-Id)
  │ routa para Camera Service
  ▼
Camera Service
  │ decripta rtsp_url (AES-256 + chave do Secrets Manager)
  │ gera token de stream assinado (HMAC, TTL 1h)
  │ retorna: { url: "https://media.x.com/tenant_T/camera_C/main/whep?token=..." }
  ▼
Browser inicializa RTCPeerConnection com URL WHEP
  │ POST para MediaMTX com SDP Offer + token
  ▼
MediaMTX recebe o POST
  │ chama webhook: POST /internal/media/auth (mTLS → Camera Service)
  │ Camera Service valida token HMAC + tenant_id + camera_id
  │ responde 200 (autorizado)
  ▼
MediaMTX negocia ICE e retorna SDP Answer
  ▼
Stream RTP flui: Câmera → MediaMTX → Browser (< 500ms)
```

### 9.2 Alerta de Câmera Offline {#fluxo-alerta}

```
CHMS Service (polling 30s)
  │ detecta câmera C do tenant T sem atividade por 2 ciclos
  │ atualiza mapa em memória: status[C] = OFFLINE
  │ Kafka PRODUCE: health.events { type: CAMERA_OFFLINE, camera_id, tenant_id, timestamp }
  ▼
Alert Service (consumer Kafka, grupo: alert-service)
  │ INSERT INTO alerts (camera_id, tenant_id, type=CAMERA_OFFLINE, ...)
  │ Redis PUBLISH tenant:{T}:alerts { alert_id, camera_name, triggered_at }
  ▼
Alert Service (WebSocket hub, subscriber Redis)
  │ todos os clients conectados no tenant T recebem via STOMP
  ▼
Browser exibe toast + marca célula do mosaico com overlay vermelho
  ▼ (em paralelo, mesmo evento Kafka)
Notification Service (consumer Kafka, grupo: notification-service)
  │ verifica throttle: Redis GET notify:camera:{C}:last_email
  │ (se não existe) envia e-mail via SendGrid
  │ Redis SET notify:camera:{C}:last_email TTL=3600
  ▼ (em paralelo, mesmo evento Kafka)
Audit Service (consumer Kafka, grupo: audit-service)
  │ INSERT INTO audit.audit_logs (action=CAMERA_WENT_OFFLINE, ...)
```

### 9.3 Gravação Contínua → R2 {#fluxo-gravacao}

```
Câmera IP → MediaMTX (RTSP ingestion)
              │ pipe para FFmpeg Worker (pod separado)
              ▼
FFmpeg Worker
  │ segmentação HLS: segmentos de 6s em /tmp
  │ a cada segmento completo:
  │   ├── upload async para R2: key = {T}/{C}/YYYY/MM/DD/HH/seg_{N}.ts
  │   ├── Kafka PRODUCE: recording.events { started_at, ended_at, r2_key, size_bytes }
  │   └── deleta /tmp local
  ▼
Recording Service (consumer Kafka, grupo: recording-service)
  │ INSERT INTO recordings (camera_id, tenant_id, started_at, ended_at, r2_key, ...)
  ▼
Retention Cleanup Job (CronJob Kubernetes, diário às 2h UTC)
  │ SELECT recordings WHERE ended_at < NOW() - plan.retention_days
  │ DELETE objetos do R2
  │ DELETE registros do banco
```

---

## 10. ADRs Consolidados {#adrs}

| # | Decisão | Alternativa Descartada | Motivo |
|---|---|---|---|
| ADR-001 | JWT validado APENAS no API Gateway | Validação em cada serviço | Evita drift de lógica; centraliza revogação |
| ADR-002 | Rate limit no gateway com Redis | Rate limit por serviço | O(1) por request; sem query ao banco |
| ADR-003 | Refresh token como hash SHA-256 no Redis | Token em texto claro | Comprometimento do Redis não expõe tokens reais |
| ADR-004 | Config de tenant cacheada no Redis | Query síncrona ao Tenant Service | Elimina dependência síncrona entre serviços no request path |
| ADR-005 | Secrets em AWS Secrets Manager / Vault | Variáveis de ambiente | Rotação automática, auditoria de acesso, sem secrets em YAML do K8s |
| ADR-006 | CHMS mantém mapa em memória | Query ao banco a cada 30s | Reduz writes no PostgreSQL de N câmeras/30s para apenas mudanças de estado |
| ADR-007 | Timeline em read replica | Primary para tudo | Queries de 7 dias de gravação não afetam writes críticos |
| ADR-008 | WebSocket auth no handshake | Auth a cada mensagem | Token fixado na sessão elimina overhead por mensagem |
| ADR-009 | Throttle de notificações no Redis | Sem throttle | Evita spam em câmeras oscilantes; Redis O(1) sem query ao banco |
| ADR-010 | Audit Service write-only via Kafka | Chamada direta de cada serviço | Serviços comprometidos não acessam audit schema; desacoplamento |
| ADR-011 | Kafka at-least-once + consumidores idempotentes | Exactly-once (mais complexo) | Simplicidade; idempotência por `event_id` é suficiente |
| ADR-012 | MediaMTX sem transcodificação por padrão | Transcodificar sempre | CPU ~4x menor; câmeras modernas já enviam H.264/H.265 compatível |
| ADR-013 | Schema por serviço no mesmo PostgreSQL | Database por serviço | Simplicidade operacional no MVP; migração para DB separado é incremental |

---

## 11. Referências Cruzadas

- Tecnologias detalhadas: [TECH_STACK.md](./TECH_STACK.md)
- Pipeline de mídia: [MEDIA_PIPELINE.md](./MEDIA_PIPELINE.md)
- Isolamento multi-tenant e RLS: [MULTI_TENANCY.md](./MULTI_TENANCY.md)
- Modelo de dados por serviço: [DATA_MODEL.md](./DATA_MODEL.md)
- Contratos de API: [API_CONTRACTS.md](./API_CONTRACTS.md)
- Segurança e LGPD: [SECURITY_LGPD.md](./SECURITY_LGPD.md)
- Variáveis de ambiente: [ENV_CONFIG.md](./ENV_CONFIG.md)
