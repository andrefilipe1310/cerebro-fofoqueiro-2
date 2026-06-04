<!-- Meta
Versão: v0.1.0
Última atualização: 2026-06-04
Documentos relacionados:
  - [Arquitetura](./ARCHITECTURE.md)
  - [ENV Config](./ENV_CONFIG.md)
  - [Code Style](./CODE_STYLE.md)
  - [Media Pipeline](./MEDIA_PIPELINE.md)
-->

# Tech Stack {#tech-stack}

## 1. Stack Completo {#stack-completo}

### 1.1 Frontend

| Camada | Tecnologia | Versão | Motivo da escolha |
|---|---|---|---|
| Framework | Next.js (App Router) | 14+ | SSR/SSG nativo, file-based routing, Server Components reduzem JS no cliente |
| Linguagem | TypeScript | 5.x | Type safety obrigatório — evita bugs em runtime em código de vídeo/WebRTC |
| Estilização | Tailwind CSS | 3.x | Utility-first, sem CSS em arquivos separados, tree-shaking automático |
| Componentes UI | Shadcn/ui | latest | Componentes acessíveis baseados em Radix UI, sem lock-in, copiáveis |
| Estado global (UI) | Zustand | 4.x | Simples, sem boilerplate, sem Provider wrapper obrigatório |
| Data fetching | TanStack Query (React Query) | 5.x | Cache automático, revalidação, deduplicação de requests, estado de loading/error |
| Player HLS | hls.js | 1.x | Suporte universal a HLS em browsers que não têm HLS nativo |
| WebRTC | Web APIs nativas | — | RTCPeerConnection nativo, sem biblioteca extra para WHEP |
| Mapa | Leaflet.js | 1.x | Open-source, leve, sem custo de tiles com OpenStreetMap |
| WebSocket client | STOMP.js + SockJS | latest | Protocolo STOMP compatível com Spring WebSocket broker |
| Icons | Lucide React | latest | Set de ícones consistente, tree-shakeable |

### 1.2 Backend

| Camada | Tecnologia | Versão | Motivo da escolha |
|---|---|---|---|
| Framework | Spring Boot | 3.x | Ecossistema maduro, Spring Security robusto, WebSocket STOMP nativo |
| Linguagem | Java | 21 (LTS) | Virtual Threads (Project Loom) para alta concorrência sem reactive programming |
| Build | Maven | 3.9+ | Convencional no ecossistema Spring, CI/CD bem estabelecido |
| Segurança | Spring Security + JWT | — | Integração nativa com Spring Boot, extensível para 2FA |
| 2FA | TOTP (Google Authenticator) | — | RFC 6238, sem dependência de SMS, funciona offline |
| ORM | Spring Data JPA + Hibernate | — | Mapeamento objeto-relacional, Hibernate Filter para multi-tenancy |
| WebSocket | Spring WebSocket (STOMP) | — | Broker nativo, integração com Redis para scale horizontal |
| Scheduler | Spring `@Scheduled` | — | CHMS heartbeat polling sem overhead de sistema externo |
| HTTP Client | Spring WebClient / RestTemplate | — | Comunicação com MediaMTX Admin API |

### 1.3 Banco de Dados

| Camada | Tecnologia | Versão | Motivo da escolha |
|---|---|---|---|
| Banco principal | PostgreSQL | 16 | RLS nativo para multi-tenancy, JSONB para dados flexíveis, extensível |
| Cache / Sessões | Redis | 7 | Pub/sub nativo para alertas, alta performance para cache de configurações |
| Pool de conexões | PgBouncer | latest | Reduz conexões abertas ao PostgreSQL (essencial com RLS por sessão) |

### 1.4 Mídia

| Camada | Tecnologia | Versão | Motivo da escolha |
|---|---|---|---|
| Media gateway | MediaMTX (ex rtsp-simple-server) | latest | Open-source, WebRTC nativo (WHEP/WHIP), API REST, auth por webhook |
| WebRTC SFU | mediasoup ou Janus | latest | SFU para distribuir streams a múltiplos viewers sem sobrecarregar câmera |
| Transcodificação | FFmpeg | 6.x | Padrão de mercado, suporta todos os codecs, segmentação HLS eficiente |
| Protocolo live | WebRTC via WHEP | — | Latência < 500ms, sem plugin, nativo em browsers modernos |
| Protocolo playback | HLS (HTTP Live Streaming) | — | Compatibilidade universal, suporte a DVR/timeline, compatível com R2 |

### 1.5 Storage

| Camada | Tecnologia | Versão | Motivo da escolha |
|---|---|---|---|
| Armazenamento de vídeo | Cloudflare R2 | — | API S3-compatible, egress gratuito, integração com CDN Cloudflare |
| Storage local (dev) | MinIO | latest | Emula R2/S3 localmente sem custo |

### 1.6 Infraestrutura

| Camada | Tecnologia | Versão | Motivo da escolha |
|---|---|---|---|
| Containerização (dev) | Docker + Docker Compose | — | Ambiente reproduzível, sem "funciona na minha máquina" |
| Orquestração (prod) | Kubernetes ou Railway | — | [DEFINIR: escolher antes do primeiro deploy prod] |
| Reverse proxy | Nginx | 1.25+ | TLS termination, roteamento por domínio, rate limiting |
| CI/CD | GitHub Actions | — | Integrado ao repositório, gratuito para repos privados até certo ponto |
| TURN server | Coturn | latest | NAT traversal para WebRTC em redes corporativas restritas |
| STUN server | Google STUN (stun.l.google.com) | — | Gratuito para descoberta de IP público |

### 1.7 Serviços Externos

| Serviço | Tecnologia | Motivo |
|---|---|---|
| E-mail transacional | SMTP / SendGrid | Alertas, convites, notificações |
| Tiles de mapa | OpenStreetMap | Gratuito, sem API key obrigatória |
| SMS (opcional, pós-MVP) | Twilio | Alertas urgentes por SMS |

---

## 2. O que NÃO Usar e Por Quê {#nao-usar}

| ❌ Não usar | ✅ Usar em vez | Motivo |
|---|---|---|
| Redux / Redux Toolkit | Zustand | Redux tem muito boilerplate para o caso de uso; Zustand é suficiente e simples |
| `useEffect` para fetch | React Query | `useEffect` não tem cache, deduplicação nem revalidação — leads to bugs |
| `any` em TypeScript | Tipos explícitos | `any` anula os benefícios do TypeScript; proibido por regra de estilo |
| Wowza Streaming Engine | MediaMTX | Wowza custa ~$200/mês/servidor, fechado, sem WebRTC nativo |
| Nginx-RTMP module | MediaMTX | Nginx-RTMP é legado, não tem WebRTC, requer compilação manual |
| Schema-per-tenant | RLS no PostgreSQL | Schema-per-tenant complica migrações e inviabiliza pooling com PgBouncer |
| JWT em localStorage | HttpOnly Cookie | localStorage é vulnerável a XSS; cookies HttpOnly são mais seguros |
| Entidades JPA direto na API | DTOs | Expor entidades vaza campos internos e acopla API ao modelo de dados |
| `@Transactional` em Controller | `@Transactional` em Service | Controllers não devem gerenciar transações — responsabilidade da camada de serviço |
| WebRTC P2P câmera-browser | MediaMTX + SFU | Câmeras não suportam WebRTC; NAT traversal sem servidor central é não-confiável |
| AWS S3 (com egress) | Cloudflare R2 | Custo de egress do S3 é proibitivo para streams de vídeo frequentes |

---

## 3. Comandos de Setup {#setup}

### 3.1 Frontend

```bash
# Criar projeto Next.js com TypeScript e Tailwind
npx create-next-app@latest . --typescript --tailwind --app --src-dir

# Instalar dependências de UI
npx shadcn-ui@latest init
npx shadcn-ui@latest add button card dialog table badge

# Estado e data fetching
npm install zustand @tanstack/react-query

# Vídeo e mapa
npm install hls.js leaflet @types/leaflet

# WebSocket
npm install @stomp/stompjs sockjs-client @types/sockjs-client

# Ícones
npm install lucide-react
```

### 3.2 Backend (Maven `pom.xml` — dependências principais)

```xml
<!-- Spring Boot Starter Parent -->
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.x.x</version>
</parent>

<!-- Dependências -->
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
  </dependency>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.x</version>
  </dependency>
  <dependency>
    <groupId>dev.samstevens.totp</groupId>
    <artifactId>totp</artifactId>
    <version>1.7.1</version>
  </dependency>
  <dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
  </dependency>
</dependencies>
```

### 3.3 Docker Compose (desenvolvimento local)

```bash
# Subir todos os serviços de infra
docker compose up -d postgres redis mediamtx minio nginx

# Verificar status
docker compose ps

# Ver logs de um serviço
docker compose logs -f mediamtx
```

---

## 4. Links para Documentação Oficial {#docs-links}

| Tecnologia | Documentação |
|---|---|
| Next.js 14 App Router | https://nextjs.org/docs/app |
| Shadcn/ui | https://ui.shadcn.com |
| TanStack Query | https://tanstack.com/query/latest |
| Zustand | https://zustand-demo.pmnd.rs |
| Spring Boot 3 | https://docs.spring.io/spring-boot/docs/current/reference/html/ |
| Spring Security | https://docs.spring.io/spring-security/reference/ |
| MediaMTX | https://github.com/bluenviron/mediamtx |
| PostgreSQL RLS | https://www.postgresql.org/docs/current/ddl-rowsecurity.html |
| Cloudflare R2 | https://developers.cloudflare.com/r2/ |
| hls.js | https://github.com/video-dev/hls.js |
| Leaflet.js | https://leafletjs.com/reference.html |
| mediasoup | https://mediasoup.org/documentation/ |

---

## 5. Referências Cruzadas

- Onde cada tecnologia se encaixa na arquitetura: [ARCHITECTURE.md](./ARCHITECTURE.md)
- Variáveis de ambiente de cada serviço: [ENV_CONFIG.md](./ENV_CONFIG.md)
- Padrões de código obrigatórios: [CODE_STYLE.md](./CODE_STYLE.md)
- Pipeline de mídia detalhada: [MEDIA_PIPELINE.md](./MEDIA_PIPELINE.md)
