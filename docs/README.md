<!-- Meta
Versão: v0.1.0
Última atualização: 2026-06-04
Documentos relacionados: todos os documentos abaixo
-->

# Fofoqueiro — Documentação do Projeto

Plataforma SaaS de videomonitoramento multi-tenant. Permite que revendedores (integradores de segurança) ofereçam monitoramento de câmeras IP para clientes finais via navegador, sem plugins, com inteligência proativa (CHMS), conformidade LGPD e painel white-label.

---

## Índice da Documentação {#indice}

| Documento | Descrição |
|---|---|
| [PRD.md](./PRD.md) | Problema, proposta de valor, épicos, métricas de sucesso e modelo de negócio |
| [USER_STORIES.md](./USER_STORIES.md) | Personas, histórias de usuário e fluxos críticos de uso |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Diagrama de componentes, responsabilidades e decisões arquiteturais (ADRs) |
| [TECH_STACK.md](./TECH_STACK.md) | Stack completo com versões, motivos de escolha e o que NÃO usar |
| [DATA_MODEL.md](./DATA_MODEL.md) | ERD, entidades completas com tipos/índices e políticas RLS |
| [API_CONTRACTS.md](./API_CONTRACTS.md) | Todos os endpoints REST + WebSocket com request/response e exemplos |
| [MULTI_TENANCY.md](./MULTI_TENANCY.md) | Estratégia RLS, injeção de tenant_id, white-label e planos/limites |
| [MEDIA_PIPELINE.md](./MEDIA_PIPELINE.md) | Pipeline RTSP→WebRTC/HLS, configuração MediaMTX, gravação e CHMS |
| [SECURITY_LGPD.md](./SECURITY_LGPD.md) | Autenticação JWT+2FA, autorização por roles, zonas de privacidade e LGPD |
| [CODE_STYLE.md](./CODE_STYLE.md) | Estrutura de pastas, nomenclatura, padrões obrigatórios e commits |
| [ENV_CONFIG.md](./ENV_CONFIG.md) | Todas as variáveis de ambiente com exemplos e instruções de setup |
| [SDD.md](./SDD.md) | Design interno de cada microserviço: padrões, sequências, máquinas de estado, algoritmos |

---

## Arquitetura de Alto Nível {#arquitetura}

```
┌─────────────────────────────────────────────────────────────────┐
│                       CAMADA FRONTEND                           │
│           Browser — Next.js 14 + TypeScript + Tailwind          │
│    Mosaico WebRTC │ Timeline HLS │ Mapa Leaflet │ Admin CRUD    │
└────────────────────────────┬────────────────────────────────────┘
                             │ REST + WebSocket (STOMP)
          ┌──────────────────┴──────────────────────┐
          │                                          │
┌─────────▼──────────────┐            ┌─────────────▼─────────────┐
│    CAMADA DE MEDIA      │            │      CAMADA BACKEND        │
│                         │            │                            │
│  MediaMTX               │            │  Spring Boot 3 / Java 21  │
│  (RTSP → WebRTC/HLS)    │            │  Spring Security + JWT     │
│           │             │            │  WebSocket (STOMP)         │
│        FFmpeg            │            │       │                    │
│           │             │            │  ┌────▼──────┐ ┌────────┐ │
│    Cloudflare R2        │            │  │PostgreSQL │ │ Redis  │ │
│    (gravações+thumbs)   │            │  │  16 + RLS │ │   7    │ │
└─────────────────────────┘            │  └───────────┘ └────────┘ │
         ▲                             └────────────────────────────┘
         │ RTSP stream
┌────────┴────────────┐
│   Câmeras IP        │
│   (clientes finais) │
└─────────────────────┘
```

---

## Como Começar a Desenvolver {#quickstart}

### Pré-requisitos
- Docker Desktop instalado e rodando
- Node.js 20+ (para o frontend)
- Java 21 + Maven 3.9+ (para o backend)
- Git

### 5 Passos para Ambiente Local

```bash
# 1. Clonar o repositório
git clone https://github.com/[DEFINIR]/[DEFINIR-repo].git
cd [DEFINIR-repo]

# 2. Copiar e configurar variáveis de ambiente
cp .env.example .env
# Edite o .env com os valores de desenvolvimento (ver ENV_CONFIG.md)

# 3. Subir os serviços de infraestrutura (PostgreSQL, Redis, MediaMTX, MinIO)
docker compose up -d

# 4. Iniciar o backend Spring Boot
cd backend
mvn spring-boot:run
# Backend disponível em http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html

# 5. Iniciar o frontend Next.js
cd ../frontend
npm install
npm run dev
# Frontend disponível em http://localhost:3000
```

### Verificar se está tudo funcionando

```bash
# Verificar serviços Docker
docker compose ps

# Testar API do backend
curl http://localhost:8080/actuator/health

# Testar API do MediaMTX
curl http://localhost:9997/v3/paths/list

# Acessar MinIO (storage local)
# http://localhost:9001 — user: minioadmin / pass: minioadmin
```

---

## Placeholders a Preencher {#placeholders}

Os seguintes valores precisam ser definidos pela equipe:

| Placeholder | Onde aparece | Descrição |
|---|---|---|
| `[DEFINIR: nome do produto]` | PRD, TECH_STACK, ENV_CONFIG | Nome comercial da plataforma |
| `[DEFINIR: repo]` | README quickstart | URL do repositório Git |
| `[DEFINIR]` preços dos planos | PRD.md#modelo-negocio | Valores R$ dos planos Starter/Pro |
| `[DEFINIR: N]` câmeras no mosaico | USER_STORIES.md#epico-1-stories | Máximo de câmeras simultâneas no mosaico |
| `[DEFINIR: fluxo de solicitação LGPD]` | SECURITY_LGPD.md#lgpd | Como titulares solicitam acesso às imagens |
| Railway vs Kubernetes (prod) | ARCHITECTURE.md, TECH_STACK.md | Decidir plataforma de deploy antes do MVP |

---

## Convenções Rápidas {#convencoes}

| Tópico | Regra resumida |
|---|---|
| Data fetching | React Query sempre — nunca `useEffect` para fetch |
| Estado global | Zustand apenas para UI — dados do servidor ficam no React Query |
| TypeScript | Sem `any` — tipos explícitos obrigatórios |
| DTOs no backend | Nunca expor entidades JPA diretamente na API |
| Erros no backend | GlobalExceptionHandler para tudo |
| Commits | Conventional Commits: `feat:`, `fix:`, `docs:`, `chore:` |
| Branches | `feat/TICKET-numero-descricao` |

Ver detalhes completos em [CODE_STYLE.md](./CODE_STYLE.md).
