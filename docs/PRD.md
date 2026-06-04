<!-- Meta
Versão: v0.1.0
Última atualização: 2026-06-04
Documentos relacionados:
  - [User Stories](./USER_STORIES.md)
  - [Arquitetura](./ARCHITECTURE.md)
  - [Multi-Tenancy](./MULTI_TENANCY.md)
  - [Tech Stack](./TECH_STACK.md)
-->

# PRD — Product Requirements Document {#prd}

## 1. Problema e Público-Alvo {#problema}

### 1.1 Problema

Integradores de segurança e revendedores precisam oferecer monitoramento de câmeras IP para seus clientes finais (condomínios, empresas, estabelecimentos comerciais), mas as soluções existentes exigem instalação de software proprietário, plugins de browser ou VMS desktop caro — o que aumenta o custo de suporte, dificulta o onboarding e impede o acesso mobile.

Além disso, as plataformas atuais não oferecem:
- Painel white-label configurável por revendedor
- Monitoramento proativo de saúde das câmeras (CHMS)
- Conformidade LGPD nativa
- Modelo SaaS escalável com isolamento por tenant

### 1.2 Público-Alvo (ICP)

| Persona | Perfil | Dor Principal |
|---|---|---|
| **Revendedor / Integrador** | Empresa de segurança eletrônica com 10–500 clientes | Custo de suporte e licença por cliente |
| **Operador de Monitoramento** | Funcionário que assiste câmeras em tempo real | Latência alta, interface confusa, sem alerta proativo |
| **Gestor do Cliente Final** | Responsável pelo condomínio ou empresa | Não consegue acessar câmeras remotamente sem instalar software |

---

## 2. Proposta de Valor {#proposta-valor}

**[DEFINIR: nome do produto]** é uma plataforma SaaS de videomonitoramento multi-tenant que permite a integradores de segurança oferecer acesso a câmeras IP via browser, sem plugins, com painel white-label, inteligência proativa e conformidade LGPD.

### 2.1 Diferencial vs. Concorrentes

| Critério | [DEFINIR: nome do produto] | Milestone | Digifort | Sunell |
|---|---|---|---|---|
| Acesso via browser puro | ✅ WebRTC nativo | ⚠️ Plugin ActiveX | ❌ Desktop only | ⚠️ Parcial |
| Multi-tenant SaaS | ✅ RLS + white-label | ❌ On-premise | ❌ On-premise | ❌ On-premise |
| CHMS proativo | ✅ Heartbeat + alertas | ❌ Manual | ⚠️ Add-on caro | ❌ |
| Conformidade LGPD | ✅ Nativa | ❌ | ❌ | ❌ |
| Latência live view | < 500ms (WebRTC) | ~2s (MJPEG) | ~1-3s | ~1s |
| Modelo de negócio | SaaS por câmera | Licença perpétua | Licença perpétua | Hardware + licença |

---

## 3. Escopo do MVP {#escopo-mvp}

### 3.1 O que entra no MVP

- [ ] Autenticação com JWT + 2FA (TOTP) obrigatório para ADMIN/OPERATOR
- [ ] Gestão de tenants (revendedores) com isolamento RLS
- [ ] Cadastro e gerenciamento de câmeras IP (RTSP)
- [ ] Mosaico de câmeras ao vivo com WebRTC (latência < 500ms)
- [ ] Player fullscreen com resolução adaptativa (sub-stream / main-stream)
- [ ] Gravação contínua com segmentação HLS → Cloudflare R2
- [ ] Linha do tempo de gravações com playback
- [ ] CHMS: detecção de câmera offline por heartbeat
- [ ] Alertas em tempo real via WebSocket (STOMP)
- [ ] Painel white-label (logo + CSS por tenant)
- [ ] Mapa interativo com localização de câmeras (Leaflet.js)
- [ ] Logs de auditoria imutáveis
- [ ] Zonas de mascaramento de privacidade
- [ ] API REST completa documentada

### 3.2 Fora do MVP (próximas versões)

- Reconhecimento facial / análise de IA avançada
- PTZ programático (tours automáticos)
- Integração com alarmes físicos (sensores, portas)
- App mobile nativo (iOS/Android)
- Relatórios automáticos agendados por e-mail
- Integração com centrais de monitoramento via protocolo SIA/Contact ID
- Alertas SMS (Twilio) — opcional pós-MVP

---

## 4. Épicos e Funcionalidades {#epicos}

### Épico 1 — Visualização ao Vivo {#epico-1}

**Objetivo:** Operador consegue visualizar qualquer câmera do seu tenant com latência < 500ms, sem plugins.

| Funcionalidade | Descrição | Prioridade |
|---|---|---|
| Gateway de Transmuxing | Converte RTSP/RTMP → WebRTC (live) e HLS (playback) via MediaMTX | Must Have |
| Mosaico Dinâmico (Adaptive Grid) | Resolution ladder com sub-streams para miniaturas e main-stream para tela cheia | Must Have |
| Player WebRTC de baixa latência | < 500ms, ideal para controle PTZ em tempo real | Must Have |
| Controle PTZ | Move câmera via comandos HTTP/ONVIF integrados ao player | Should Have |

### Épico 2 — Gestão e Operação (Multi-Tenancy) {#epico-2}

**Objetivo:** Revendedor consegue cadastrar seus clientes, câmeras e usuários com isolamento completo de dados.

| Funcionalidade | Descrição | Prioridade |
|---|---|---|
| Arquitetura Multi-Tenant | Isolamento lógico via tenant_id + RLS no PostgreSQL | Must Have |
| Painel White-Label | CSS/logos dinâmicos carregados por domínio de acesso | Must Have |
| Mapa Interativo | Localização visual de câmeras com status online/offline via Leaflet.js | Should Have |
| Gestão de Usuários | CRUD de usuários por tenant com controle de roles | Must Have |

### Épico 3 — Inteligência e Saúde (Proatividade) {#epico-3}

**Objetivo:** Sistema detecta e notifica proativamente problemas de câmeras sem intervenção humana.

| Funcionalidade | Descrição | Prioridade |
|---|---|---|
| CHMS — Camera Health Monitoring System | Heartbeat alerts + Recording Confidence Score | Must Have |
| Detecção de Movimento e Alertas | Via analíticos da câmera ou gateway de processamento | Should Have |
| Linha do Tempo de Gravações | Scroll fluido integrado com Cloudflare R2 | Must Have |
| Thumbnails Automáticos | FFmpeg gera preview a cada 30s para a timeline | Must Have |

### Épico 4 — Segurança, Auditoria e LGPD {#epico-4}

**Objetivo:** Plataforma está em conformidade com LGPD e possui rastreabilidade completa de ações.

| Funcionalidade | Descrição | Prioridade |
|---|---|---|
| Zonas de Mascaramento de Privacidade | Overlay estático sobre stream de vídeo (polígonos configuráveis) | Must Have |
| Logs de Auditoria Imutáveis | Quem, o quê, quando, onde — serviço isolado com trigger anti-deleção | Must Have |
| Criptografia TLS + 2FA | HTTPS obrigatório + TOTP para todos os operadores | Must Have |
| Retenção Configurável | Política de retenção de gravações por plano | Must Have |

---

## 5. Métricas de Sucesso {#metricas}

| Métrica | Meta | Como medir |
|---|---|---|
| Latência live view (WebRTC) | < 500ms p95 | Monitoramento client-side via WebRTC stats API |
| Uptime da plataforma | ≥ 99,5% | Uptime Robot / Datadog |
| Tempo de onboarding (novo tenant) | < 10 minutos | Analytics de funil |
| Detecção de câmera offline | < 60s após queda | Logs do CHMS |
| Taxa de falha de stream | < 0,5% das sessões | MediaMTX metrics + Sentry |
| Churn mensal de tenants | < 3% | CRM |

---

## 6. Modelo de Negócio {#modelo-negocio}

### 6.1 Planos por Tenant (Revendedor)

| Plano | Câmeras | Usuários | Retenção | White-label | Preço sugerido |
|---|---|---|---|---|---|
| **Free** | Até 4 | 2 | 7 dias | ❌ | R$ 0/mês |
| **Starter** | Até 16 | 5 | 30 dias | ✅ básico | [DEFINIR] |
| **Pro** | Até 64 | 20 | 90 dias | ✅ completo | [DEFINIR] |
| **Enterprise** | Ilimitado | Ilimitado | 365 dias | ✅ + domínio próprio | Contrato |

### 6.2 Cobranças Adicionais

- Câmeras extras além do plano: R$ [DEFINIR] por câmera/mês
- Armazenamento adicional além da retenção padrão: R$ [DEFINIR]/GB/mês
- SLA premium (suporte 24/7): Add-on no plano Enterprise

---

## 7. Referências Cruzadas

- Histórias de usuário detalhadas: [USER_STORIES.md](./USER_STORIES.md)
- Arquitetura técnica: [ARCHITECTURE.md](./ARCHITECTURE.md)
- Modelo de dados: [DATA_MODEL.md](./DATA_MODEL.md)
- Isolamento multi-tenant: [MULTI_TENANCY.md](./MULTI_TENANCY.md)
