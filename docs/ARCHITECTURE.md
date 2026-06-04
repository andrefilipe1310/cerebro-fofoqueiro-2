<!-- Meta
Versão: v0.1.0
Última atualização: 2026-06-04
Documentos relacionados:
  - [Tech Stack](./TECH_STACK.md)
  - [Media Pipeline](./MEDIA_PIPELINE.md)
  - [Multi-Tenancy](./MULTI_TENANCY.md)
  - [Data Model](./DATA_MODEL.md)
  - [API Contracts](./API_CONTRACTS.md)
  - [ENV Config](./ENV_CONFIG.md)
-->

# Arquitetura do Sistema {#architecture}

## 1. Visão Geral {#visao-geral}

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CAMADA FRONTEND                             │
│  Browser (Next.js 14 + TypeScript + Tailwind + Shadcn/ui)          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐              │
│  │ Mosaico  │ │ Timeline │ │   Mapa   │ │  Admin   │              │
│  │ (WebRTC) │ │  (HLS)   │ │(Leaflet) │ │ (CRUD)   │              │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘              │
└───────┼────────────┼────────────┼────────────┼────────────────────┘
        │ WebRTC/HLS │ HLS        │ REST       │ REST + WS (STOMP)
┌───────┼────────────┼────────────┼────────────┼────────────────────┐
│       │      CAMADA DE MEDIA    │            │  CAMADA BACKEND     │
│  ┌────▼──────────────────┐      │     ┌──────▼──────────────────┐ │
│  │      MediaMTX         │      │     │    Spring Boot 3.x       │ │
│  │  (RTSP → WebRTC/HLS)  │      │     │    (Java 21, Maven)      │ │
│  │  + mediasoup/Janus    │      │     │    Spring Security + JWT  │ │
│  └────────────┬──────────┘      │     │    WebSocket (STOMP)      │ │
│               │ FFmpeg          │     └──────┬──────────┬─────────┘ │
│  ┌────────────▼──────────┐      │            │          │           │
│  │   Cloudflare R2       │      │     ┌──────▼──┐ ┌────▼────────┐ │
│  │  (HLS segments +      │◄─────┘     │PostgreSQL│ │   Redis 7   │ │
│  │   thumbnails)         │            │16 + RLS  │ │(cache+pub/  │ │
│  └───────────────────────┘            └──────────┘ │  sub+sess.) │ │
│                                                     └─────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
        ▲
        │ RTSP/RTMP
┌───────┴─────────────────────┐
│     CÂMERAS IP (clientes)   │
│  Câmera 1 ... Câmera N      │
└─────────────────────────────┘
```

---

## 2. Componentes e Responsabilidades {#componentes}

### 2.1 Frontend — Next.js 14 (App Router) {#frontend}

| Responsabilidade | Tecnologia |
|---|---|
| Renderização de páginas (SSR/SSG) | Next.js App Router |
| Estado global de UI (modais, tema, tenant ativo) | Zustand |
| Data fetching e cache do servidor | React Query (TanStack Query) |
| Componentes de UI | Shadcn/ui + Tailwind CSS |
| Player de vídeo WebRTC | Web APIs nativas + adaptador mediasoup |
| Player HLS (playback/timeline) | hls.js |
| Mapa interativo | Leaflet.js + OpenStreetMap tiles |
| WebSocket (alertas em tempo real) | STOMP.js sobre SockJS |

> **ADR:** Zustand é usado exclusivamente para estado de UI (ex: câmera selecionada, layout do mosaico). Dados do servidor sempre via React Query — nunca via Zustand store. Isso evita inconsistência entre cache do servidor e estado local. Ver [CODE_STYLE.md](./CODE_STYLE.md).

### 2.2 Backend — Spring Boot 3.x {#backend}

| Responsabilidade | Componente |
|---|---|
| API REST (/api/v1/) | Spring MVC Controllers |
| Autenticação e autorização | Spring Security + JWT + TOTP |
| Isolamento multi-tenant | Hibernate Filter injetado por tenant_id do JWT |
| Alertas em tempo real | WebSocket STOMP + Redis pub/sub |
| CHMS heartbeat polling | Spring `@Scheduled` a cada 30s |
| Comunicação com MediaMTX | REST HTTP client (MediaMTX Admin API) |
| Auditoria | AOP interceptor que grava em audit_logs |
| Criptografia de dados sensíveis | JPA AttributeConverter (AES-256) |

### 2.3 MediaMTX — Gateway de Mídia {#mediamtx}

| Responsabilidade | Detalhe |
|---|---|
| Ingresso de streams | Recebe RTSP/RTMP de câmeras IP |
| Transmuxing para clientes | RTSP → WebRTC (live) e HLS (playback) |
| Segmentação HLS | Segmentos de 2s para low-latency HLS |
| Autenticação de streams | Webhook auth chamando backend antes de permitir acesso |
| Sub-streams | Configura quality ladder (main=1080p, sub=480p) |
| API de status | REST API para o CHMS consultar câmeras ativas |

> **ADR:** MediaMTX escolhido sobre Wowza e Nginx-RTMP porque é open-source, tem WebRTC nativo, API REST de administração, e suporte a autenticação por webhook — essencial para o modelo multi-tenant. Wowza seria ~$200/mês por servidor. Ver [MEDIA_PIPELINE.md](./MEDIA_PIPELINE.md).

### 2.4 PostgreSQL 16 com RLS {#postgres}

| Responsabilidade | Detalhe |
|---|---|
| Armazenamento principal | Todas as entidades de negócio |
| Isolamento multi-tenant | Row-Level Security (RLS) com tenant_id |
| Imutabilidade de audit logs | Trigger que bloqueia UPDATE/DELETE |
| JSONB | coordinates em privacy_zones, map_bounds em locations |

> **ADR:** RLS no PostgreSQL escolhido sobre schema-per-tenant porque: (1) DDL compartilhado simplifica migrações, (2) conexão pooling (PgBouncer) funciona normalmente, (3) PostgreSQL gerencia o isolamento de forma transparente sem mudança no código de negócio. Ver [MULTI_TENANCY.md](./MULTI_TENANCY.md).

### 2.5 Redis 7 {#redis}

| Responsabilidade | Detalhe |
|---|---|
| Cache de JWT / sessões | TTL alinhado ao `JWT_EXPIRATION_MS` |
| Pub/sub de alertas | Canal `tenant:{id}:alerts` consumido por WebSocket workers |
| Cache de configuração de tenant | Logo URL, CSS, plano — evita queries frequentes |
| Rate limiting | Proteção por IP e por usuário |

### 2.6 Cloudflare R2 {#r2}

| Responsabilidade | Detalhe |
|---|---|
| Gravações HLS | Segmentos `.ts` + playlists `.m3u8` |
| Thumbnails | Frames gerados pelo FFmpeg a cada 30s |
| Egress gratuito | Zero custo de saída de dados (vs AWS S3) |

> **ADR:** Cloudflare R2 escolhido sobre AWS S3 pelo egress gratuito. Em um sistema de vídeo com muitos downloads/streams, o custo de egress do S3 pode chegar a centenas de dólares/mês. R2 é compatível com API S3. Ver [ENV_CONFIG.md](./ENV_CONFIG.md).

---

## 3. Fluxos de Dados Críticos {#fluxos}

### 3.1 Live View — RTSP → WebRTC (latência < 500ms) {#fluxo-live}

```
Câmera IP
    │ RTSP stream contínuo
    ▼
MediaMTX (path: /tenant_{id}/camera_{id}/main)
    │ WebRTC offer/answer via WHEP
    ▼
Browser (Web APIs: RTCPeerConnection)
    │ ICE negotiation (STUN/TURN)
    ▼
Player no mosaico ou fullscreen
```

**Passos técnicos:**
1. Frontend chama `GET /api/v1/cameras/{id}/stream/live`
2. Backend valida JWT + tenant_id, consulta `rtsp_url` da câmera (decriptada)
3. Backend retorna URL WebRTC do MediaMTX: `https://media.{domain}/tenant_{id}/camera_{id}/main/whep`
4. Frontend inicializa `RTCPeerConnection` com a URL WHEP
5. MediaMTX negocia ICE e retorna SDP answer
6. Stream de vídeo flui diretamente câmera → MediaMTX → browser (sem reencoding desnecessário)

**Configuração de latência:**
- Buffer de playout: 0ms (WHEP não usa buffer por padrão)
- ICE servers: STUN público + TURN próprio para NAT traversal
- Codec: H.264 ou H.265 (depende da câmera, sem transcodificação sempre que possível)

---

### 3.2 Gravação Contínua — Stream → R2 {#fluxo-gravacao}

```
Câmera IP
    │ RTSP stream
    ▼
MediaMTX
    │ Pipe interno para FFmpeg
    ▼
FFmpeg
    ├── Segmentação HLS: segmentos de 6s (.ts files)
    │   Naming: {tenant_id}/{camera_id}/YYYY/MM/DD/HH/seg_{N}.ts
    │
    └── Thumbnail: frame a cada 30s
        Naming: {tenant_id}/{camera_id}/YYYY/MM/DD/HH/thumb_{N}.jpg
    │
    ▼
Cloudflare R2 (upload via SDK S3-compatible)
    │
    ▼
Backend registra metadados em recordings [ver DATA_MODEL.md#recordings]
```

**Nota:** O upload para R2 é feito por um processo worker separado que consome os segmentos gerados pelo FFmpeg em disco temporário (ou via pipe), evitando que o backend principal seja bloqueado.

---

### 3.3 Alerta de Câmera Offline {#fluxo-alerta}

```
CHMS (@Scheduled, 30s)
    │ GET {MEDIAMTX_API_URL}/v3/paths (lista paths ativos)
    ▼
Backend compara paths ativos com cameras.status no banco
    │ Detecta câmera sem atividade
    ▼
Backend cria health_event (type=OFFLINE) [ver DATA_MODEL.md#health_events]
    │
    ├── Atualiza cameras.status = OFFLINE
    │
    ├── Publica no Redis pub/sub: PUBLISH tenant:{id}:alerts {payload JSON}
    │
    └── Envia e-mail via SMTP/SendGrid ao tenant owner
    │
    ▼
WebSocket Worker (subscriber Redis)
    │ Consome mensagem do canal
    ▼
STOMP Broker (Spring WebSocket)
    │ Envia para /topic/tenant/{id}/alerts
    ▼
Browser (STOMP.js subscriber)
    │ Recebe mensagem
    ▼
UI: Toast de alerta + célula do mosaico com overlay vermelho
```

---

## 4. Decisões Arquiteturais (ADRs) {#adrs}

### ADR-001 — WebSocket STOMP para alertas {#adr-001}

> **Por que STOMP sobre WebSocket em vez de Server-Sent Events (SSE)?**

SSE é unidirecional (servidor → cliente) e não suporta múltiplos tópicos na mesma conexão sem multiplexação manual. STOMP sobre WebSocket oferece: (1) tópicos nomeados por tenant nativo, (2) possibilidade futura de o cliente enviar mensagens (ex: ACK de alerta), (3) suporte de primeira classe no Spring Framework com `@EnableWebSocketMessageBroker`. SSE seria mais simples, mas limitaria extensibilidade.

---

### ADR-002 — mediasoup/Janus para WebRTC server-side {#adr-002}

> **Por que um media server WebRTC em vez de WebRTC puro P2P?**

P2P direto entre câmera e browser não é viável porque: (1) câmeras IP não suportam WebRTC, apenas RTSP/RTMP, (2) NAT traversal em redes de clientes é não-confiável sem TURN, (3) mosaico com N câmeras abertas simultaneamente exige SFU (Selective Forwarding Unit) para não sobrecarregar a câmera. mediasoup ou Janus atuam como SFU, recebendo um stream e distribuindo para N clientes.

---

### ADR-003 — Nginx como reverse proxy {#adr-003}

> **Por que Nginx em frente a todos os serviços?**

Nginx centraliza: TLS termination (Let's Encrypt / Cloudflare), roteamento por subdomínio/path (api.domain → Spring Boot, media.domain → MediaMTX), rate limiting e compressão gzip. Sem Nginx, cada serviço precisaria de TLS próprio e a configuração de segurança seria fragmentada.

---

## 5. Referências Cruzadas

- Tecnologias usadas: [TECH_STACK.md](./TECH_STACK.md)
- Pipeline de mídia detalhada: [MEDIA_PIPELINE.md](./MEDIA_PIPELINE.md)
- Isolamento multi-tenant: [MULTI_TENANCY.md](./MULTI_TENANCY.md)
- Modelo de dados completo: [DATA_MODEL.md](./DATA_MODEL.md)
- Variáveis de ambiente: [ENV_CONFIG.md](./ENV_CONFIG.md)
