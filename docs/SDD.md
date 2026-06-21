<!-- Meta
Versão: v0.1.0
Última atualização: 2026-06-04
Documentos relacionados:
  - [Arquitetura](./ARCHITECTURE.md)
  - [Data Model](./DATA_MODEL.md)
  - [API Contracts](./API_CONTRACTS.md)
  - [Security LGPD](./SECURITY_LGPD.md)
  - [Media Pipeline](./MEDIA_PIPELINE.md)
  - [Multi-Tenancy](./MULTI_TENANCY.md)
  - [Implementation Tracker](./IMPLEMENTATION_TRACKER.md)
-->

# SDD — Software Design Document {#sdd}

> Este documento detalha o design interno de cada microserviço: padrões de implementação, diagramas de sequência, máquinas de estado, algoritmos e contratos internos entre serviços. É o guia de referência para quem implementa — não para quem define o produto.

---

## 1. Princípios de Design {#principios}

| Princípio | Aplicação concreta neste sistema |
|---|---|
| **Single Responsibility** | Cada serviço tem um único motivo para mudar (CHMS muda se mudar a lógica de health; Audit muda se mudar a lei) |
| **Dependency Inversion** | Serviços dependem de interfaces (portas), não de implementações (adaptadores) — padrão Hexagonal |
| **Idempotência** | Todo consumer Kafka pode processar o mesmo evento N vezes sem efeito colateral duplicado |
| **Fail-fast** | Erro de configuração na inicialização mata o processo — melhor falhar cedo do que em produção |
| **Outbox Pattern** | Banco e Kafka nunca são escritos na mesma transação — Outbox garante consistência eventual |
| **CQRS leve** | Recording Service e Audit Service separam path de escrita (Kafka consumer) do path de leitura (REST API) |

---

## 2. Arquitetura Hexagonal por Serviço {#hexagonal}

Todos os serviços seguem o mesmo template de camadas:

```
┌─────────────────────────────────────────────────────┐
│                   SERVICE BOUNDARY                   │
│                                                      │
│  ┌──────────────┐        ┌──────────────────────┐   │
│  │  DRIVING     │        │   DOMAIN CORE         │   │
│  │  ADAPTERS    │◄──────►│                       │   │
│  │  (entrada)   │        │  • Entities           │   │
│  │              │        │  • Value Objects       │   │
│  │  • REST Ctrl │        │  • Domain Services    │   │
│  │  • Kafka Cns │        │  • Domain Events      │   │
│  │  • WS Handler│        │  • Ports (interfaces) │   │
│  └──────────────┘        └──────────┬────────────┘   │
│                                     │                 │
│                          ┌──────────▼────────────┐   │
│                          │  DRIVEN ADAPTERS       │   │
│                          │  (saída)               │   │
│                          │                        │   │
│                          │  • JPA Repository      │   │
│                          │  • Kafka Producer      │   │
│                          │  • Redis Client        │   │
│                          │  • HTTP Client         │   │
│                          └────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

### Estrutura de pacotes padrão (por serviço)

```
com.fofoqueiro.{service}/
├── adapter/
│   ├── in/
│   │   ├── rest/          ← Controllers REST
│   │   ├── kafka/         ← Kafka Consumers
│   │   └── websocket/     ← WebSocket Handlers
│   └── out/
│       ├── persistence/   ← JPA Repositories + Mappers
│       ├── messaging/     ← Kafka Producers
│       ├── cache/         ← Redis Adapters
│       └── http/          ← HTTP Clients (MediaMTX, SendGrid...)
├── application/
│   ├── port/
│   │   ├── in/            ← Use Case interfaces (driving ports)
│   │   └── out/           ← Repository/messaging interfaces (driven ports)
│   └── service/           ← Use Case implementations (@Service)
└── domain/
    ├── model/             ← Entities, Value Objects, Aggregates
    ├── event/             ← Domain Events
    └── exception/         ← Domain Exceptions
```

---

## 3. Outbox Pattern — Consistência Kafka + Banco {#outbox}

### 3.1 Problema

```
❌ ABORDAGEM INGÊNUA (não usar):

@Transactional
public void createCamera(Camera camera) {
    cameraRepository.save(camera);       // 1. salva no banco ✅
    kafkaProducer.send("camera.created"); // 2. envia para Kafka ← pode falhar!
    // Se Kafka estiver indisponível: banco commitou, Kafka não recebeu → inconsistência
}
```

### 3.2 Solução: Outbox Pattern

```
✅ COM OUTBOX PATTERN:

Transação única:
  ├── INSERT INTO cameras (...)           ← dado principal
  └── INSERT INTO outbox_events (...)     ← evento pendente de envio

Job separado (OutboxRelayJob, a cada 1s):
  ├── SELECT * FROM outbox_events WHERE sent_at IS NULL ORDER BY created_at LIMIT 100
  ├── Para cada evento: kafkaProducer.send(topic, payload)
  └── UPDATE outbox_events SET sent_at = NOW() WHERE id = ?
```

### 3.3 Tabela Outbox (cada serviço tem a sua)

```sql
CREATE TABLE outbox_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic       VARCHAR(100) NOT NULL,        -- ex: 'camera.events'
    event_type  VARCHAR(100) NOT NULL,        -- ex: 'camera.created'
    payload     JSONB NOT NULL,               -- conteúdo do evento
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at     TIMESTAMPTZ,                  -- NULL = pendente
    attempts    INTEGER NOT NULL DEFAULT 0,
    last_error  TEXT
);

CREATE INDEX idx_outbox_unsent ON outbox_events(created_at)
    WHERE sent_at IS NULL;
```

### 3.4 Implementação do OutboxRelayJob

```java
// OutboxRelayJob.java — roda a cada 1 segundo em cada instância do serviço
@Component
public class OutboxRelayJob {

    @Scheduled(fixedDelay = 1_000)
    @Transactional
    public void relay() {
        // SELECT com FOR UPDATE SKIP LOCKED → múltiplas instâncias não processam o mesmo evento
        List<OutboxEvent> pending = outboxRepository
            .findPendingWithLock(100);  // lote de 100

        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getId().toString(), event.getPayload())
                    .get(5, TimeUnit.SECONDS);  // aguarda ack do Kafka

                event.markAsSent();
            } catch (Exception e) {
                event.incrementAttempts(e.getMessage());
                // após 5 tentativas falhas: mover para dead-letter table
                if (event.getAttempts() >= 5) {
                    deadLetterRepository.save(DeadLetterEvent.from(event));
                    outboxRepository.delete(event);
                }
            }
        }
    }
}
```

```sql
-- Query com pessimistic locking para múltiplas instâncias
SELECT * FROM outbox_events
WHERE sent_at IS NULL AND attempts < 5
ORDER BY created_at
LIMIT 100
FOR UPDATE SKIP LOCKED;
```

> **Por que `FOR UPDATE SKIP LOCKED`?** Em múltiplas instâncias do serviço rodando em paralelo, cada uma tenta pegar lotes do outbox. `SKIP LOCKED` faz cada instância pular linhas já travadas por outra instância — sem espera, sem deadlock.

---

## 4. Estrutura de Eventos Kafka {#eventos-kafka}

### 4.1 Envelope Padrão

Todo evento Kafka usa o mesmo envelope para garantir rastreabilidade:

```json
{
  "event_id":   "uuid-v4-unico",
  "event_type": "camera.went_offline",
  "version":    "1.0",
  "occurred_at":"2026-06-04T14:00:00.000Z",
  "producer":   "chms-service",
  "tenant_id":  "uuid-do-tenant",
  "correlation_id": "X-Request-Id da request original (se houver)",
  "payload": {
    // conteúdo específico do evento
  }
}
```

### 4.2 Catálogo de Eventos

| Tópico | event_type | Produtor | Payload principal |
|---|---|---|---|
| `auth.events` | `auth.login.success` | Auth Service | `{ user_id, ip, user_agent }` |
| `auth.events` | `auth.login.failure` | Auth Service | `{ email, ip, reason }` |
| `auth.events` | `auth.logout` | Auth Service | `{ user_id }` |
| `camera.events` | `camera.created` | Camera Service | `{ camera_id, name, location_id }` |
| `camera.events` | `camera.deleted` | Camera Service | `{ camera_id, name }` |
| `camera.events` | `camera.rtsp_updated` | Camera Service | `{ camera_id }` (sem a URL — segurança) |
| `health.events` | `camera.went_offline` | CHMS Service | `{ camera_id, detected_at, last_seen_at }` |
| `health.events` | `camera.came_online` | CHMS Service | `{ camera_id, downtime_seconds }` |
| `health.events` | `camera.low_confidence` | CHMS Service | `{ camera_id, score, period_hours }` |
| `alert.events` | `alert.created` | Alert Service | `{ alert_id, type, camera_id, message }` |
| `alert.events` | `alert.acknowledged` | Alert Service | `{ alert_id, acknowledged_by, acknowledged_at }` |
| `recording.events` | `recording.segment_stored` | Recording Service | `{ recording_id, r2_key, duration_seconds }` |
| `recording.events` | `recording.deleted` | Recording Service | `{ camera_id, from, to, reason }` |
| `tenant.events` | `tenant.created` | Tenant Service | `{ tenant_id, slug, plan }` |
| `tenant.events` | `tenant.plan_changed` | Tenant Service | `{ tenant_id, old_plan, new_plan }` |
| `tenant.events` | `tenant.suspended` | Tenant Service | `{ tenant_id, reason }` |
| `audit.events` | `audit.action` | Todos os serviços | `{ action, resource_type, resource_id, user_id, ip }` |

### 4.3 Idempotência por event_id

```java
// Padrão de consumer idempotente
@KafkaListener(topics = "health.events", groupId = "alert-service")
public void consume(ConsumerRecord<String, String> record) {
    DomainEvent event = deserialize(record.value());

    // Checar se já processamos este evento (Redis com TTL de 24h)
    String dedupKey = "processed:" + event.getEventId();
    if (Boolean.TRUE.equals(redis.hasKey(dedupKey))) {
        log.debug("Evento {} já processado, ignorando", event.getEventId());
        return;
    }

    // Processar o evento
    processEvent(event);

    // Marcar como processado
    redis.opsForValue().set(dedupKey, "1", Duration.ofHours(24));
}
```

---

## 5. Design do Auth Service {#design-auth}

### 5.1 Diagrama de Sequência — Login com 2FA

```
Browser          API Gateway       Auth Service        Redis         PostgreSQL
   │                  │                 │                │               │
   │─POST /auth/login─►                 │                │               │
   │                  │─valida headers─►                 │               │
   │                  │                 │─SELECT users──────────────────►│
   │                  │                 │◄──── user row ─────────────────│
   │                  │                 │─bcrypt.verify()                │
   │                  │                 │─gera JWT temporário            │
   │                  │                 │  (scope: 2fa-only, TTL 5min)   │
   │◄─200 { requires_2fa: true,────────────────────────────────────────  │
   │         access_token_temp }         │                │               │
   │                  │                 │                │               │
   │─POST /auth/2fa/verify (token_temp)►│                │               │
   │                  │─extrai scope───►│                │               │
   │                  │  (2fa-only ok)  │─TOTP.verify()  │               │
   │                  │                 │─gera JWT final (scope: full)   │
   │                  │                 │─gera refresh token             │
   │                  │                 │─SET refresh:{userId} ─────────►│
   │                  │                 │  TTL=7days                     │
   │                  │                 │─Outbox: auth.login.success     │
   │◄─200 { access_token, refresh }────────────────────────────────────  │
   │  Set-Cookie: access=... HttpOnly   │                │               │
   │  Set-Cookie: refresh=... HttpOnly  │                │               │
```

### 5.2 Máquina de Estado — Sessão de Usuário

```
                    ┌─────────────────────────────────────────┐
                    │                                          │
        login ok    │               refresh ok                │
[ANON] ──────────► [TEMP_2FA] ──2fa ok──► [AUTHENTICATED] ──────► [AUTHENTICATED]
                       │                        │
                  2fa falha 3x            logout / expirou
                       │                        │
                   [BLOQUEADO]              [ANON]
                   (15 minutos)
```

### 5.3 Estrutura de Claims do JWT

```java
// JwtService.java
public String generateAccessToken(User user, JwtScope scope) {
    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("tenant_id", user.getTenantId().toString())
        .claim("role", user.getRole().name())
        .claim("email", user.getEmail())
        .claim("scope", scope.name())          // FULL ou TWO_FA_ONLY
        .issuedAt(new Date())
        .expiration(scope == JwtScope.FULL
            ? new Date(now + JWT_EXPIRATION_MS)       // 15 min
            : new Date(now + JWT_TEMP_EXPIRATION_MS)) // 5 min
        .signWith(getSigningKey())
        .compact();
}
```

### 5.4 Rotação de JWT_SECRET sem Downtime

```
Problema: ao rotacionar JWT_SECRET, tokens válidos emitidos com a chave antiga viram inválidos.

Solução: suporte a múltiplas chaves simultâneas por 15 minutos:

JWKS (JSON Web Key Set):
  {
    "keys": [
      { "kid": "v2", "k": "nova_chave_base64", "use": "sig" },  ← assina novos tokens
      { "kid": "v1", "k": "chave_anterior_base64", "use": "sig" } ← válida por 15 min
    ]
  }

API Gateway busca o JWKS a cada 5 min via: GET /api/v1/auth/.well-known/jwks.json
Valida o token usando o kid (Key ID) presente no header do JWT.
Após 15 min, a chave v1 é removida do JWKS.
```

---

## 6. Design do Camera Service {#design-camera}

### 6.1 Diagrama de Sequência — Cadastro de Câmera

```
Browser       API Gateway    Camera Service    MediaMTX     PostgreSQL    Kafka Outbox
   │               │               │               │              │             │
   │─POST /cameras─►               │               │              │             │
   │               │─injeta────────►               │              │             │
   │               │  X-Tenant-Id  │               │              │             │
   │               │               │─verifica limite de plano (Redis)           │
   │               │               │─AES-256 encrypt(rtsp_url)    │             │
   │               │               │─valida RTSP: ─────────────►  │             │
   │               │               │  POST /v3/config/paths        │             │
   │               │               │  { source: rtsp://... }       │             │
   │               │               │◄─ 200 OK (câmera acessível) ─│             │
   │               │               │                               │             │
   │               │               │── BEGIN TRANSACTION ──────────────────────  │
   │               │               │─INSERT cameras ──────────────►│             │
   │               │               │─INSERT outbox_events ─────────────────────►│
   │               │               │── COMMIT ─────────────────────────────────  │
   │               │               │                               │             │
   │◄─201 { id, name, status }─────│               │              │             │
   │               │               │               │              │             │
   │               │       OutboxRelayJob (async, 1s)             │             │
   │               │               │─send Kafka: camera.created ──────────────► │
   │               │               │─UPDATE outbox_events.sent_at ─────────────►│
```

### 6.2 Geração de Stream URL com Token Assinado

```java
// StreamUrlService.java
public StreamUrlResponse generateLiveUrl(UUID cameraId, UUID tenantId, UUID userId) {
    Camera camera = cameraRepository.findByIdAndTenantId(cameraId, tenantId)
        .orElseThrow(() -> new CameraNotFoundException(cameraId));

    // Token de stream: HMAC-SHA256 com payload { camera_id, tenant_id, user_id, exp }
    String streamToken = streamTokenSigner.sign(StreamTokenClaims.builder()
        .cameraId(cameraId)
        .tenantId(tenantId)
        .userId(userId)
        .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
        .build());

    String whepUrl = String.format("%s/tenant_%s/camera_%s/main/whep?token=%s",
        mediamtxPublicUrl, tenantId, cameraId, streamToken);

    // Auditoria assíncrona via outbox
    outboxService.publish("audit.events", "audit.action", AuditPayload.builder()
        .action("VIEW_CAMERA_LIVE")
        .resourceType("CAMERA")
        .resourceId(cameraId)
        .userId(userId)
        .build());

    return StreamUrlResponse.builder()
        .type("webrtc")
        .url(whepUrl)
        .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
        .fallbackHls(whepUrl.replace("/whep", "/index.m3u8"))
        .build();
}
```

### 6.3 Webhook de Auth do MediaMTX

```java
// MediaAuthWebhookController.java
// Endpoint interno — acessível apenas via mTLS (verificado pelo Istio sidecar)
@PostMapping("/internal/media/auth")
public ResponseEntity<Void> validateStreamAccess(@RequestBody MediaAuthRequest req) {
    // req.path = "tenant_abc123/camera_xyz789/main"
    // req.token = JWT de stream do query param

    try {
        StreamTokenClaims claims = streamTokenSigner.verify(req.getToken());

        // Validar que o path corresponde ao token
        String expectedPath = "tenant_%s/camera_%s".formatted(
            claims.getTenantId(), claims.getCameraId());

        if (!req.getPath().startsWith(expectedPath)) {
            return ResponseEntity.status(403).build(); // tenant A tentando acessar câmera do tenant B
        }

        // Validar que a câmera ainda existe e está ativa
        if (!cameraRepository.existsByIdAndTenantIdAndStatusNot(
                claims.getCameraId(), claims.getTenantId(), CameraStatus.DELETED)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok().build(); // autorizado

    } catch (InvalidTokenException e) {
        return ResponseEntity.status(401).build();
    }
}
```

---

## 7. Design do CHMS Service {#design-chms}

### 7.1 Consistent Hashing para Particionamento de Câmeras

Com múltiplas instâncias do CHMS, cada uma monitora um subset das câmeras. O particionamento usa consistent hashing baseado no `instance_id` (nome do pod no Kubernetes).

```java
// CameraPartitioner.java
public class CameraPartitioner {

    private final String instanceId;      // nome do pod: "chms-0", "chms-1", etc.
    private final int totalInstances;     // lido do ConfigMap do Kubernetes

    public boolean isResponsibleFor(UUID cameraId) {
        int bucket = Math.abs(cameraId.hashCode()) % totalInstances;
        int myBucket = parseInstanceIndex(instanceId); // "chms-2" → 2
        return bucket == myBucket;
    }
}

// CameraHealthService.java
@Scheduled(fixedDelay = 30_000)
public void pollCameraHealth() {
    // Buscar apenas câmeras do meu range de partição
    List<Camera> myCameras = cameraRepository.findAll().stream()
        .filter(c -> partitioner.isResponsibleFor(c.getId()))
        .toList();

    Set<String> activePaths = fetchActivePathsFromMediaMTX();

    for (Camera camera : myCameras) {
        String expectedPath = "tenant_%s/camera_%s".formatted(
            camera.getTenantId(), camera.getId());
        boolean isActive = activePaths.contains(expectedPath + "/main");

        CameraStatus currentStatus = statusMap.get(camera.getId());

        if (!isActive && currentStatus != CameraStatus.OFFLINE) {
            handleTransition(camera, CameraStatus.ONLINE, CameraStatus.OFFLINE);
        } else if (isActive && currentStatus == CameraStatus.OFFLINE) {
            handleTransition(camera, CameraStatus.OFFLINE, CameraStatus.ONLINE);
        }
    }
}
```

### 7.2 Máquina de Estado — Camera.status

```
                 câmera cadastrada
                       │
                   [UNKNOWN]
                  /         \
        stream ativo       timeout (60s)
              │                  │
          [ONLINE]           [OFFLINE]
              │    \        /    │
              │     timeout  stream    │
              │     (60s)   ativo      │
              │          \  /          │
              │         [OFFLINE]      │
              │              │         │
              └──────────────┘         │
                stream ativo           │
                                   deletada
                                   [DELETED]
                                 (soft delete)
```

**Regra de transição:**
- `UNKNOWN → ONLINE`: primeiro heartbeat recebido do MediaMTX
- `ONLINE → OFFLINE`: 2 ciclos consecutivos de polling (60s) sem atividade
- `OFFLINE → ONLINE`: aparece nos paths ativos do MediaMTX
- Qualquer estado → `DELETED`: ADMIN deleta a câmera

```java
// handleTransition — sempre via Outbox Pattern
private void handleTransition(Camera camera, CameraStatus from, CameraStatus to) {
    log.info("Camera {} transitioning {} → {}", camera.getId(), from, to);

    // Atualizar mapa em memória IMEDIATAMENTE (sem esperar o banco)
    statusMap.put(camera.getId(), to);

    String eventType = to == CameraStatus.OFFLINE
        ? "camera.went_offline"
        : "camera.came_online";

    // Persistir e enfileirar evento em uma transação
    transactionTemplate.execute(status -> {
        cameraRepository.updateStatus(camera.getId(), to);
        outboxRepository.save(OutboxEvent.of("health.events", eventType,
            Map.of("camera_id", camera.getId(),
                   "tenant_id", camera.getTenantId(),
                   "detected_at", Instant.now())));
        return null;
    });
}
```

### 7.3 Recording Confidence Score — Algoritmo

```java
// RecordingConfidenceCalculator.java
public double calculate(UUID cameraId, Duration period) {
    Instant from = Instant.now().minus(period);
    Instant to = Instant.now();

    // Segundos totais no período
    long expectedSeconds = period.toSeconds();

    // Soma dos segmentos gravados no período (usa read replica)
    long recordedSeconds = recordingRepository
        .sumDurationByCamera(cameraId, from, to)
        .orElse(0L);

    // Descontar gaps causados por manutenção programada (futuro)
    // long maintenanceSeconds = maintenanceRepository.sumByCamera(cameraId, from, to);
    // expectedSeconds -= maintenanceSeconds;

    double score = (double) recordedSeconds / expectedSeconds * 100.0;
    return Math.min(score, 100.0); // cap em 100% (possível overlap em borda de segmento)
}
```

---

## 8. Design do Alert Service {#design-alert}

### 8.1 Diagrama de Sequência — Alerta Offline → WebSocket

```
CHMS Svc    Kafka     Alert Svc(inst.A)  Alert Svc(inst.B)  Redis      Browser(tenant T)
    │          │              │                  │              │              │
    │─PRODUCE──►              │                  │              │              │
    │  health.events          │                  │              │              │
    │  camera.went_offline    │                  │              │              │
    │          │─CONSUME─────►│                  │              │              │
    │          │              │─dedup check──────────────────►  │              │
    │          │              │◄─ key não existe (novo evento)  │              │
    │          │              │─INSERT alerts ────────────────  │              │
    │          │              │─PUBLISH tenant:T:alerts ──────►│              │
    │          │              │─SET dedup key TTL 24h ─────────►│              │
    │          │              │                  │              │              │
    │          │              │                  │─SUBSCRIBE────►(já assinado) │
    │          │              │                  │◄─ mensagem ──│              │
    │          │              │                  │─STOMP send────────────────►│
    │          │              │                  │              │  toast alerta│
```

**Nota:** O Browser pode estar conectado à instância A ou B do Alert Service. A instância que recebeu o evento do Kafka publica no Redis. A instância que tem a conexão WebSocket do tenant T consome do Redis e entrega. **O Redis é o barramento que desacopla as instâncias.**

### 8.2 Máquina de Estado — Alert

```
                  evento de saúde detectado
                            │
                       [TRIGGERED]
                            │
                 operador visualiza / reconhece
                            │
                    [ACKNOWLEDGED]
                            │
                   problema resolvido
                            │
                      [RESOLVED]
                            │
                    retenção expirada
                            │
                      [ARCHIVED]
```

### 8.3 WebSocket Hub — Design de Sessões

```java
// AlertWebSocketHandler.java
@Component
public class AlertWebSocketHandler {

    // Mapa: tenant_id → Set de sessões WebSocket ativas nesta instância
    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> tenantSessions
        = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID tenantId = extractTenantId(session); // do JWT no query param
        tenantSessions.computeIfAbsent(tenantId, k -> ConcurrentHashMap.newKeySet())
            .add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID tenantId = extractTenantId(session);
        Set<WebSocketSession> sessions = tenantSessions.get(tenantId);
        if (sessions != null) sessions.remove(session);
    }

    // Chamado pelo RedisSubscriber quando chega mensagem
    public void broadcast(UUID tenantId, String payload) {
        Set<WebSocketSession> sessions = tenantSessions.getOrDefault(tenantId, Set.of());
        sessions.removeIf(session -> !session.isOpen()); // limpar sessões mortas

        sessions.forEach(session -> {
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException e) {
                log.warn("Falha ao enviar WS para sessão {}", session.getId());
            }
        });
    }
}
```

---

## 9. Design do Recording Service {#design-recording}

### 9.1 CQRS — Separação de Escrita e Leitura

```
WRITE SIDE (Kafka Consumer)              READ SIDE (REST API)
─────────────────────────────            ────────────────────────────
Consumer: recording.events               Controller: GET /cameras/{id}/timeline
         ↓                                           ↓
Insert no PostgreSQL primary             Query na PostgreSQL read replica
(schema recordings)                      (índice composto: camera_id + started_at)
                                                      ↓
                                         Resposta paginada por cursor de tempo
```

### 9.2 Diagrama de Sequência — Timeline Query

```
Browser      API Gateway    Recording Svc    PG Read Replica    Cloudflare R2
   │               │               │                │                 │
   │─GET /cameras/{id}/timeline?from=...&to=...──────►              │
   │               │─injeta X-Tenant-Id────►         │               │
   │               │               │─SELECT recordings                │
   │               │               │  WHERE camera_id = ?             │
   │               │               │  AND started_at >= ?             │
   │               │               │  AND ended_at <= ?   ──────────►│
   │               │               │◄─── rows (com r2_key) ──────────│
   │               │               │─detectar gaps (lógica in-memory) │
   │               │               │─gerar thumbnail URLs (R2 signed) │
   │◄─200 { segments: [...], gaps: [...] }──────────                 │
```

### 9.3 Algoritmo de Detecção de Gaps

```java
// GapDetector.java
public List<TimelineGap> detectGaps(List<Recording> segments, Instant from, Instant to) {
    List<TimelineGap> gaps = new ArrayList<>();

    // Segmentos já vêm ordenados por started_at (ORDER BY no banco)
    Instant cursor = from;

    for (Recording segment : segments) {
        if (segment.getStartedAt().isAfter(cursor.plus(GAP_THRESHOLD))) {
            // Há um gap entre cursor e o início deste segmento
            gaps.add(TimelineGap.builder()
                .from(cursor)
                .to(segment.getStartedAt())
                .durationSeconds(Duration.between(cursor, segment.getStartedAt()).toSeconds())
                .build());
        }
        cursor = segment.getEndedAt();
    }

    // Checar gap no final (câmera pode estar offline agora)
    if (cursor.isBefore(to.minus(GAP_THRESHOLD))) {
        gaps.add(TimelineGap.builder()
            .from(cursor)
            .to(to)
            .build());
    }

    return gaps;
}

private static final Duration GAP_THRESHOLD = Duration.ofSeconds(15); // tolerância para borda de segmento
```

### 9.4 Signed URL para Download

```java
// DownloadUrlService.java
public DownloadUrlResponse generateDownloadUrl(UUID recordingId, UUID tenantId) {
    Recording recording = recordingRepository.findByIdAndTenantId(recordingId, tenantId)
        .orElseThrow(() -> new RecordingNotFoundException(recordingId));

    // Signed URL do R2 (compatível com S3 presigned URL)
    URL signedUrl = r2Client.generatePresignedGetObjectRequest(
        GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(15))
            .getObjectRequest(r -> r
                .bucket(r2BucketName)
                .key(recording.getR2Key())
                .responseContentDisposition("attachment; filename=\"%s.ts\""
                    .formatted(recording.getFilename())))
            .build());

    // Auditoria
    outboxService.publish("audit.events", "audit.action",
        AuditPayload.of("DOWNLOAD_RECORDING", "RECORDING", recordingId));

    return DownloadUrlResponse.of(signedUrl.toString(),
        Instant.now().plus(15, ChronoUnit.MINUTES));
}
```

---

## 10. Design do Audit Service {#design-audit}

### 10.1 Padrão Append-Only

O Audit Service é o único consumidor do tópico `audit.events`. Ele nunca atualiza nem deleta registros — apenas insere.

```java
// AuditEventConsumer.java
@KafkaListener(topics = "audit.events", groupId = "audit-service")
public void consume(ConsumerRecord<String, String> record) {
    DomainEvent event = deserialize(record.value());

    // Idempotência: event_id como chave de deduplicação
    if (auditRepository.existsByEventId(event.getEventId())) {
        return; // já processado (at-least-once delivery do Kafka)
    }

    AuditLog log = AuditLog.builder()
        .eventId(event.getEventId())           // para idempotência
        .tenantId(event.getTenantId())
        .userId(extractUserId(event))
        .action(event.getEventType())
        .resourceType(extractResourceType(event))
        .resourceId(extractResourceId(event))
        .ipAddress(extractIp(event))
        .occurredAt(event.getOccurredAt())
        .rawPayload(event.getPayload())        // payload completo para investigação
        .build();

    auditRepository.save(log);
    // Sem Outbox aqui — o Audit Service SÓ LÊ do Kafka, nunca produz eventos
}
```

### 10.2 Query de Auditoria com Cursor Pagination

```java
// AuditLogRepository.java
@Query("""
    SELECT a FROM AuditLog a
    WHERE a.tenantId = :tenantId
      AND (:userId IS NULL OR a.userId = :userId)
      AND (:action IS NULL OR a.action = :action)
      AND a.occurredAt >= :from
      AND a.occurredAt <= :to
      AND (:cursor IS NULL OR a.occurredAt < :cursor)
    ORDER BY a.occurredAt DESC
    LIMIT :limit
    """)
List<AuditLog> findWithCursor(
    @Param("tenantId") UUID tenantId,
    @Param("userId") UUID userId,
    @Param("action") String action,
    @Param("from") Instant from,
    @Param("to") Instant to,
    @Param("cursor") Instant cursor,   // cursor = occurred_at do último item da página anterior
    @Param("limit") int limit
);
```

---

## 11. Design do Tenant Service {#design-tenant}

### 11.1 Cache de Configuração no Redis

```java
// TenantConfigCache.java
@Component
public class TenantConfigCache {

    private static final String KEY_PREFIX = "tenant:config:";
    private static final Duration TTL = Duration.ofMinutes(5);

    public TenantConfig get(UUID tenantId) {
        String key = KEY_PREFIX + tenantId;
        String cached = redis.opsForValue().get(key);

        if (cached != null) {
            return deserialize(cached, TenantConfig.class);
        }

        // Cache miss: buscar no banco e popular cache
        TenantConfig config = tenantRepository.findConfigById(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));
        redis.opsForValue().set(key, serialize(config), TTL);
        return config;
    }

    // Invalidar cache ao atualizar tenant (consumindo tenant.events do Kafka)
    @KafkaListener(topics = "tenant.events", groupId = "tenant-cache-invalidator")
    public void onTenantEvent(ConsumerRecord<String, String> record) {
        DomainEvent event = deserialize(record.value());
        UUID tenantId = UUID.fromString(event.getPayload().get("tenant_id").asText());
        redis.delete(KEY_PREFIX + tenantId);
    }
}
```

### 11.2 Enforcement de Limite de Plano

```java
// PlanLimitEnforcer.java — chamado antes de criar câmera ou usuário
public void assertCameraLimit(UUID tenantId) {
    TenantConfig config = tenantConfigCache.get(tenantId);
    int maxCameras = PlanLimits.maxCameras(config.getPlan());

    // Contador em Redis para performance (evita COUNT(*) no banco a cada criação)
    String countKey = "tenant:" + tenantId + ":camera_count";
    Long currentCount = redis.opsForValue().increment(countKey, 0); // ler sem incrementar

    if (currentCount == null) {
        // Cold start: popular do banco e setar no Redis
        currentCount = cameraRepository.countByTenantId(tenantId);
        redis.opsForValue().set(countKey, currentCount.toString());
    }

    if (currentCount >= maxCameras) {
        throw new PlanLimitExceededException(
            "Plano %s permite %d câmeras. Atual: %d".formatted(
                config.getPlan(), maxCameras, currentCount));
    }
}

// Ao criar câmera com sucesso: incrementar contador
public void incrementCameraCount(UUID tenantId) {
    redis.opsForValue().increment("tenant:" + tenantId + ":camera_count");
}

// Ao deletar câmera: decrementar contador
public void decrementCameraCount(UUID tenantId) {
    redis.opsForValue().decrement("tenant:" + tenantId + ":camera_count");
}
```

---

## 12. Contratos Internos entre Serviços {#contratos-internos}

Interfaces que NÃO passam pelo API Gateway público — tráfego interno via mTLS.

### 12.1 Camera Service → MediaMTX

```
POST {MEDIAMTX_API_URL}/v3/config/paths/add/{path}
Authorization: Bearer {MEDIAMTX_AUTH_TOKEN}
Content-Type: application/json

{
  "source": "rtsp://...",           ← URL RTSP decriptada
  "sourceOnDemand": true,           ← conecta só quando há viewer
  "record": true,
  "recordPath": "/tmp/recordings/{path}/{Y}-{M}-{D}_{h}-{m}-{s}-{f}"
}

Response: 200 OK (path registrado)
Response: 409 Conflict (path já existe — idempotente: ignorar)
```

### 12.2 MediaMTX → Camera Service (Webhook Auth)

```
POST /internal/media/auth
X-Internal-Secret: {INTERNAL_API_SECRET}   ← validado antes do mTLS (defesa em profundidade)
Content-Type: application/json

{
  "action":   "read",
  "path":     "tenant_abc123/camera_xyz789/main",
  "query":    "token=eyJ...",
  "id":       "session-uuid"
}

Response 200: autorizado
Response 401: token inválido ou expirado
Response 403: tenant/camera mismatch ou câmera deletada
```

### 12.3 CHMS Service → MediaMTX

```
GET {MEDIAMTX_API_URL}/v3/paths/list
Authorization: Bearer {MEDIAMTX_AUTH_TOKEN}

Response 200:
{
  "pageCount": 1,
  "items": [
    {
      "name": "tenant_abc123/camera_xyz789/main",
      "ready": true,
      "readyTime": "2026-06-04T14:00:00Z",
      "tracks": ["H264", "PCMU"],
      "bytesReceived": 104857600,
      "bytesSent": 524288000,
      "readers": [
        { "type": "webRTCSession", "id": "..." }
      ]
    }
  ]
}
```

### 12.4 API Gateway → Auth Service (JWKS)

```
GET /api/v1/auth/.well-known/jwks.json
(sem autenticação — endpoint público para o gateway)

Response 200:
{
  "keys": [
    {
      "kty": "oct",
      "use": "sig",
      "kid": "v2",
      "k": "base64url_encoded_key",
      "alg": "HS256"
    }
  ]
}
```

---

## 13. Tratamento de Erros e Resiliência {#resiliencia}

### 13.1 Circuit Breaker — Chamadas entre Serviços

```java
// MediaMtxClient.java — com Resilience4j
@CircuitBreaker(name = "mediamtx", fallbackMethod = "fallbackGetPaths")
@Retry(name = "mediamtx")
@TimeLimiter(name = "mediamtx")
public List<String> getActivePaths() {
    return webClient.get()
        .uri(mediamtxApiUrl + "/v3/paths/list")
        .retrieve()
        .bodyToMono(MediaMtxPathsResponse.class)
        .map(MediaMtxPathsResponse::extractPathNames)
        .block(Duration.ofSeconds(5));
}

// Fallback: CHMS continua com dados do último poll bem-sucedido
public List<String> fallbackGetPaths(Exception ex) {
    log.warn("MediaMTX indisponível, usando cache do último poll: {}", ex.getMessage());
    return lastKnownActivePaths; // campo em memória, atualizado no último poll bem-sucedido
}
```

**Configuração Resilience4j:**

```yaml
resilience4j:
  circuitbreaker:
    instances:
      mediamtx:
        slidingWindowSize: 10
        failureRateThreshold: 50       # abre após 5/10 falhas
        waitDurationInOpenState: 30s   # tenta fechar após 30s
        permittedNumberOfCallsInHalfOpenState: 3
  retry:
    instances:
      mediamtx:
        maxAttempts: 3
        waitDuration: 1s
        exponentialBackoffMultiplier: 2
  timelimiter:
    instances:
      mediamtx:
        timeoutDuration: 5s
```

### 13.2 Dead Letter Queue para Kafka

```java
// Mensagens que falham após 3 tentativas vão para DLQ
@KafkaListener(topics = "health.events.DLQ", groupId = "dlq-monitor")
public void consumeDLQ(ConsumerRecord<String, String> record) {
    // Alerta para o time de engenharia
    alertingService.notifyEngineering(
        "Mensagem na DLQ de health.events",
        record.value(),
        record.headers()
    );
    // Salvar para análise manual
    deadLetterRepository.save(DeadLetterRecord.from(record));
}
```

### 13.3 Health Checks por Serviço

```java
// Cada serviço expõe: GET /actuator/health
// Kubernetes usa como liveness e readiness probe

// Readiness probe verifica dependências:
@Component
public class ServiceReadinessIndicator implements HealthIndicator {

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        // Verificar PostgreSQL
        boolean dbOk = checkDatabase();
        // Verificar Redis
        boolean redisOk = checkRedis();
        // Verificar Kafka (producer consegue conectar)
        boolean kafkaOk = checkKafka();

        if (dbOk && redisOk && kafkaOk) {
            return builder.up().build();
        }
        return builder.down()
            .withDetail("db", dbOk)
            .withDetail("redis", redisOk)
            .withDetail("kafka", kafkaOk)
            .build();
    }
}
```

---

## 14. Diagramas de Sequência — Fluxos Completos {#sequencias}

### 14.1 Primeiro Acesso de um Tenant (Onboarding Completo)

```
Admin Browser   API GW   Tenant Svc   Auth Svc   Email Svc   Kafka
     │             │          │            │           │          │
     │─POST /admin/tenants────►           │           │          │
     │             │          │─valida unicidade slug  │          │
     │             │          │─INSERT tenants         │          │
     │             │          │─INSERT users (owner)   │          │
     │             │          │─INSERT outbox_events ──────────► │
     │             │          │  tenant.created                   │
     │◄─201 { tenant_id }─────│            │           │          │
     │             │          │            │           │          │
     │             │          │◄─ Kafka: tenant.created ──────────│
     │             │          │─gera first-access token           │
     │             │          │─PUBLISH auth.events ─────────────►│
     │             │          │─send email ────────────────────► │
     │             │          │  (link first-access, TTL 24h)     │
     │             │          │            │           │          │
     │                        │            │           │          │
Owner Browser                 │            │           │          │
     │                        │            │           │          │
     │─GET /first-access?token=xxx─────────►           │          │
     │             │          │─valida token (Redis)   │          │
     │◄─302 redirect /setup-password──────────────────            │
     │             │          │            │           │          │
     │─POST /auth/setup-password──────────►            │          │
     │             │          │─bcrypt hash            │          │
     │             │          │─UPDATE users.password  │          │
     │             │          │─emite JWT temp (2fa-only scope)    │
     │◄─200 { requires_2fa_setup: true }───            │          │
     │             │          │            │           │          │
     │─POST /auth/2fa/setup───────────────────────────►           │
     │             │          │            │─gera TOTP secret      │
     │             │          │            │─retorna QR code       │
     │◄─200 { qr_code, backup_codes }──────────────────           │
     │             │          │            │           │          │
     │─POST /auth/2fa/confirm─────────────────────────►           │
     │             │          │            │─TOTP.verify()         │
     │             │          │            │─emite JWT final       │
     │◄─200 { access_token } ─────────────────────────            │
     │─redirect /dashboard────────────────────────────            │
```

### 14.2 Escalonamento Automático do CHMS (Kubernetes HPA)

```
Prometheus           HPA Controller       CHMS Deployment
     │                     │                    │
     │─coleta métricas────►│                    │
     │  cameras_per_pod=3000│                   │
     │  (threshold=2000)    │                   │
     │                     │─calcula réplicas   │
     │                     │  desejadas = 3000/2000 = 2 → arredonda → 2 pods
     │                     │─PATCH deployment ─►│
     │                     │  replicas: 2 → 3   │
     │                     │                    │─novo pod: "chms-2" sobe
     │                     │                    │─CameraPartitioner: myBucket = 2
     │                     │                    │─começa monitorar câmeras do bucket 2
     │                     │                    │─outros pods liberam bucket 2 no próximo tick
```

---

## 15. Glossário de Padrões Usados {#glossario}

| Padrão | Onde é usado | Por quê |
|---|---|---|
| **Hexagonal Architecture** | Todos os serviços | Testabilidade — domain core não depende de Spring, Kafka ou JPA |
| **Outbox Pattern** | Todos os serviços que produzem Kafka | Garante que banco e Kafka sejam consistentes sem transação distribuída |
| **CQRS** | Recording Service, Audit Service | Path de escrita (Kafka consumer) e leitura (REST) têm otimizações diferentes |
| **Consistent Hashing** | CHMS Service | Distribui câmeras entre instâncias sem coordenação centralizada |
| **Circuit Breaker** | Todas as chamadas HTTP externas (MediaMTX) | Isola falhas de dependências externas sem derrubar o serviço todo |
| **Dead Letter Queue** | Todos os Kafka consumers | Mensagens que falham repetidamente não bloqueiam o tópico principal |
| **Cache-Aside** | Tenant Config, Camera Count | Cache preenchido na demanda, invalidado por evento Kafka |
| **Idempotent Consumer** | Todos os Kafka consumers | at-least-once delivery do Kafka exige que reprocessar não cause duplicidade |
| **Append-Only Store** | Audit Service | Imutabilidade por design — trigger PostgreSQL como segunda linha de defesa |
| **Sticky Session (L4)** | Load Balancer → MediaMTX | WebRTC não suporta failover transparente — mesma câmera sempre vai para mesma instância |

---

## 16. Referências Cruzadas

- Diagrama macro de microserviços: [ARCHITECTURE.md](./ARCHITECTURE.md)
- Contratos de API pública (REST + WS): [API_CONTRACTS.md](./API_CONTRACTS.md)
- Entidades de banco por serviço: [DATA_MODEL.md](./DATA_MODEL.md)
- Segurança e LGPD: [SECURITY_LGPD.md](./SECURITY_LGPD.md)
- Pipeline de mídia completa: [MEDIA_PIPELINE.md](./MEDIA_PIPELINE.md)
- Isolamento multi-tenant: [MULTI_TENANCY.md](./MULTI_TENANCY.md)
- Variáveis de ambiente: [ENV_CONFIG.md](./ENV_CONFIG.md)
