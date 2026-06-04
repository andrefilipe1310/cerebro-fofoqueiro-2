<!-- Meta
Versão: v0.1.0
Última atualização: 2026-06-04
Documentos relacionados:
  - [PRD](./PRD.md)
  - [API Contracts](./API_CONTRACTS.md)
  - [Arquitetura](./ARCHITECTURE.md)
  - [Data Model](./DATA_MODEL.md)
-->

# User Stories {#user-stories}

## 1. Personas {#personas}

| Persona | Descrição | Acesso |
|---|---|---|
| **Admin do Sistema** | Equipe interna que gerencia a plataforma, cria tenants, define planos | Acesso total a todos os tenants |
| **Revendedor (Tenant Owner)** | Integrador de segurança que contratou o serviço e gerencia seus clientes finais | Acesso total dentro do seu tenant |
| **Operador** | Funcionário do revendedor que monitora câmeras ativamente | Visualiza câmeras, recebe alertas, controla PTZ |
| **Visualizador** | Gestor do cliente final (ex: síndico) com acesso somente-leitura | Apenas visualiza câmeras ao vivo e gravações |

---

## 2. Histórias por Épico {#historias-por-epico}

### Épico 1 — Visualização ao Vivo {#epico-1-stories}

**US-101** — Visualização em mosaico
> Como **Operador**, quero visualizar todas as câmeras do meu tenant em um mosaico na tela principal para ter visão geral em tempo real.

**Critérios de aceite:**
- [ ] Mosaico exibe até [DEFINIR: N] câmeras simultâneas sem travamento
- [ ] Cada célula usa o sub-stream (baixa resolução) para economizar banda
- [ ] Status online/offline visível em cada célula
- [ ] Layout adaptativo (1x1, 2x2, 3x3, 4x4, customizado)

---

**US-102** — Câmera em fullscreen
> Como **Operador**, quero clicar em uma câmera do mosaico para abri-la em fullscreen com stream principal (alta resolução) para analisar detalhes.

**Critérios de aceite:**
- [ ] Transição do sub-stream para o main-stream ocorre em < 2s
- [ ] Latência do stream principal < 500ms (WebRTC)
- [ ] Overlay mostra nome da câmera, localização e horário sincronizado
- [ ] Botão de fechar retorna ao mosaico sem recarregar a página

---

**US-103** — Controle PTZ
> Como **Operador**, quero controlar a rotação e zoom de câmeras PTZ diretamente no player para acompanhar eventos em tempo real.

**Critérios de aceite:**
- [ ] Controles visíveis apenas para câmeras com `ptz_enabled = true`
- [ ] Resposta ao comando PTZ < 300ms após clique
- [ ] Suporte a movimento contínuo (segurar botão) e passo a passo
- [ ] Log de auditoria registra cada comando PTZ (ver [audit_logs](./DATA_MODEL.md#audit_logs))

---

**US-104** — Qualidade adaptativa
> Como **Operador**, quero que o sistema ajuste automaticamente a qualidade do stream conforme minha conexão para evitar travamentos.

**Critérios de aceite:**
- [ ] Sistema detecta degradação de conexão e troca para HLS se WebRTC falhar
- [ ] Indicador de qualidade visível no player (HD/SD/baixa)
- [ ] Sem interrupção perceptível na troca de qualidade

---

### Épico 2 — Gestão e Operação {#epico-2-stories}

**US-201** — Onboarding de novo tenant
> Como **Admin do Sistema**, quero criar um novo tenant (revendedor) na plataforma para que ele possa começar a cadastrar suas câmeras imediatamente.

**Critérios de aceite:**
- [ ] Formulário coleta: nome, e-mail do owner, domínio (opcional), plano
- [ ] Sistema gera slug único a partir do nome
- [ ] Owner recebe e-mail de boas-vindas com link de primeiro acesso
- [ ] Tenant criado com limite de câmeras e usuários conforme plano
- [ ] Todo o processo < 10 minutos (ver [métricas](./PRD.md#metricas))

---

**US-202** — Cadastro de câmera
> Como **Revendedor**, quero cadastrar uma câmera IP informando a URL RTSP para que o sistema comece a transmiti-la imediatamente.

**Critérios de aceite:**
- [ ] Formulário valida URL RTSP antes de salvar
- [ ] Sistema testa conexão com a câmera e exibe status em tempo real
- [ ] Câmera aparece no mosaico e no mapa após cadastro
- [ ] URL RTSP armazenada criptografada (ver [SECURITY_LGPD.md](./SECURITY_LGPD.md))
- [ ] Erro claro se limite de câmeras do plano for atingido

---

**US-203** — Configuração white-label
> Como **Revendedor**, quero personalizar o logotipo e as cores da plataforma para apresentar aos meus clientes com a minha marca.

**Critérios de aceite:**
- [ ] Upload de logo (PNG/SVG, max 2MB)
- [ ] Campo para CSS customizado (cores primárias, fontes)
- [ ] Preview ao vivo das mudanças antes de salvar
- [ ] Mudanças refletidas em até 30s para todos os usuários do tenant
- [ ] Ver entidade [tenants](./DATA_MODEL.md#tenants)

---

**US-204** — Mapa interativo
> Como **Operador**, quero ver um mapa com todas as câmeras do tenant posicionadas geograficamente para identificar rapidamente qual câmera está em qual local.

**Critérios de aceite:**
- [ ] Câmeras exibidas com marcador colorido (verde = online, vermelho = offline, cinza = sem stream)
- [ ] Clique no marcador abre popup com preview da câmera e botão "Abrir ao vivo"
- [ ] Mapa centraliza automaticamente no cluster de câmeras do tenant
- [ ] Atualização de status em tempo real via WebSocket

---

**US-205** — Gestão de usuários
> Como **Revendedor**, quero criar e gerenciar usuários dentro do meu tenant atribuindo diferentes níveis de acesso para controlar o que cada um pode fazer.

**Critérios de aceite:**
- [ ] Roles disponíveis: OPERATOR e VIEWER (ADMIN é apenas o owner do tenant)
- [ ] Usuário criado recebe e-mail de convite
- [ ] Revendedor pode desativar usuário sem deletar histórico de auditoria
- [ ] Limite de usuários conforme plano respeitado

---

### Épico 3 — Inteligência e Saúde {#epico-3-stories}

**US-301** — Alerta de câmera offline
> Como **Operador**, quero receber uma notificação imediata quando uma câmera ficar offline para agir antes que o cliente perceba.

**Critérios de aceite:**
- [ ] Alerta exibido na interface em < 60s após queda da câmera
- [ ] Notificação por e-mail enviada para o Revendedor responsável
- [ ] Célula do mosaico exibe overlay vermelho com ícone de erro
- [ ] Ver entidade [health_events](./DATA_MODEL.md#health_events)
- [ ] Ver endpoint [GET /api/v1/cameras/{id}/health](./API_CONTRACTS.md)

---

**US-302** — Linha do tempo de gravações
> Como **Operador**, quero navegar pela linha do tempo de uma câmera específica para encontrar e rever um evento que ocorreu em horário específico.

**Critérios de aceite:**
- [ ] Timeline exibe barra de tempo com segmentos de gravação disponíveis (verde) e ausentes (vermelho/cinza)
- [ ] Scrub na timeline inicia playback HLS do ponto selecionado em < 3s
- [ ] Zoom na timeline (hora, dia, semana)
- [ ] Download do segmento selecionado disponível para ADMIN e OPERATOR
- [ ] Ver entidade [recordings](./DATA_MODEL.md#recordings)

---

**US-303** — Recording Confidence Score
> Como **Revendedor**, quero saber se minhas câmeras estão gravando corretamente para garantir que não haja gaps na cobertura.

**Critérios de aceite:**
- [ ] Score calculado com base em: % do tempo com gravação ativa nas últimas 24h
- [ ] Câmeras com score < 90% marcadas com alerta amarelo
- [ ] Relatório diário enviado por e-mail com score de cada câmera
- [ ] Dashboard exibe ranking das câmeras com pior performance

---

### Épico 4 — Segurança, Auditoria e LGPD {#epico-4-stories}

**US-401** — Autenticação com 2FA
> Como **Operador**, quero usar autenticação em dois fatores para proteger meu acesso à plataforma mesmo que minha senha seja comprometida.

**Critérios de aceite:**
- [ ] 2FA (TOTP) obrigatório para roles ADMIN e OPERATOR no primeiro acesso
- [ ] QR code exibido para configurar no Google Authenticator / Authy
- [ ] Código de backup gerado no setup (8 códigos de uso único)
- [ ] Ver [POST /api/v1/auth/2fa/verify](./API_CONTRACTS.md)

---

**US-402** — Zonas de mascaramento
> Como **Revendedor**, quero definir regiões da câmera que devem ser ocultadas automaticamente (ex: área privativa de moradores) para cumprir com a LGPD.

**Critérios de aceite:**
- [ ] Editor visual de polígonos sobre o frame da câmera
- [ ] Múltiplas zonas por câmera
- [ ] Zona aplicada tanto no stream ao vivo quanto nas gravações
- [ ] Zona pode ser ativada/desativada sem deletar a configuração
- [ ] Ver entidade [privacy_zones](./DATA_MODEL.md#privacy_zones)

---

**US-403** — Log de auditoria
> Como **Admin do Sistema**, quero visualizar o histórico completo de ações de qualquer usuário para investigar incidentes de segurança.

**Critérios de aceite:**
- [ ] Log registra: login, logout, visualização de câmera, download de gravação, alterações de configuração, comandos PTZ
- [ ] Log imutável (nenhum usuário pode deletar ou editar)
- [ ] Filtros por: tenant, usuário, tipo de ação, intervalo de data
- [ ] Exportação em CSV
- [ ] Ver entidade [audit_logs](./DATA_MODEL.md#audit_logs)

---

## 3. Fluxos Críticos {#fluxos-criticos}

### Fluxo 1 — Onboarding de novo tenant (revendedor) {#fluxo-onboarding}

```
1. Admin acessa /admin/tenants → clica em "Novo Tenant"
2. Preenche: nome da empresa, e-mail do owner, domínio (opcional), plano
3. Sistema valida unicidade do slug e domínio
4. Sistema cria registro na tabela tenants [ver DATA_MODEL.md#tenants]
5. Sistema cria usuário owner com role=ADMIN para o tenant
6. Sistema envia e-mail de boas-vindas com link de first-login (token único 24h)
7. Owner acessa o link → define senha → configura 2FA (TOTP obrigatório)
8. Owner é redirecionado para o dashboard vazio do seu tenant
9. Owner pode agora: cadastrar câmeras, convidar usuários, configurar white-label
```

**Endpoint usado:** `POST /api/v1/admin/tenants` → [ver API_CONTRACTS.md](./API_CONTRACTS.md)

---

### Fluxo 2 — Adicionar câmera IP ao sistema {#fluxo-adicionar-camera}

```
1. Revendedor acessa /cameras → clica em "Adicionar Câmera"
2. Preenche: nome, localização, URL RTSP (main stream), URL RTSP (sub stream, opcional)
3. Define coordenadas GPS (lat/lng) ou usa seletor no mapa
4. Informa se câmera tem PTZ
5. Sistema valida se limite de câmeras do plano não foi atingido
6. Sistema envia URL RTSP para MediaMTX registrar o path [ver MEDIA_PIPELINE.md]
7. MediaMTX confirma conexão bem-sucedida com a câmera
8. Sistema salva câmera com status=ONLINE na tabela cameras [ver DATA_MODEL.md#cameras]
9. Câmera aparece no mosaico e no mapa imediatamente
10. CHMS começa a monitorar heartbeat da câmera a cada 30s
```

---

### Fluxo 3 — Visualizar mosaico ao vivo + abrir câmera em fullscreen {#fluxo-mosaico}

```
1. Operador acessa / (dashboard) → mosaico carrega automaticamente
2. Frontend busca lista de câmeras: GET /api/v1/cameras [ver API_CONTRACTS.md]
3. Para cada câmera, frontend busca URL do sub-stream: GET /api/v1/cameras/{id}/stream/hls/sub
4. Player carrega sub-stream HLS para cada célula do mosaico
5. Operador clica em uma câmera → modal fullscreen abre
6. Frontend busca URL do stream ao vivo: GET /api/v1/cameras/{id}/stream/live
7. Player WebRTC inicializa com a URL retornada (ICE negotiation)
8. Stream ao vivo exibido com latência < 500ms
9. Operador fecha modal → sub-stream do mosaico continua normalmente
```

---

### Fluxo 4 — Receber alerta de câmera offline e investigar na timeline {#fluxo-alerta-offline}

```
1. Câmera para de enviar heartbeat para o MediaMTX
2. Backend CHMS detecta ausência de heartbeat após 30s de polling
3. Backend cria registro em health_events [ver DATA_MODEL.md#health_events] com type=OFFLINE
4. Backend publica evento no Redis pub/sub: channel = tenant:{tenant_id}:alerts
5. WebSocket worker consome o canal Redis e envia via STOMP ao tópico /topic/tenant/{id}/alerts
6. Frontend recebe mensagem WebSocket → exibe toast de alerta + marca câmera no mosaico
7. Operador clica no alerta → vai para página da câmera
8. Operador acessa aba "Timeline" → carrega GET /api/v1/cameras/{id}/timeline
9. Timeline exibe ponto de corte da gravação (gap) no horário da queda
10. Operador reproduce os últimos 5 minutos antes da queda para verificar o que ocorreu
```

---

### Fluxo 5 — Operador PTZ controla câmera em tempo real {#fluxo-ptz}

```
1. Operador abre câmera em fullscreen (câmera com ptz_enabled=true)
2. Controles PTZ aparecem overlay no player (setas direcionais + zoom)
3. Operador clica e segura seta para a esquerda
4. Frontend envia: POST /api/v1/cameras/{id}/ptz com body {direction: "left", speed: 5}
5. Backend valida permissão (OPERATOR role) e encaminha comando ONVIF para câmera
6. Backend registra ação em audit_logs [ver DATA_MODEL.md#audit_logs]
7. Câmera move-se em tempo real (resposta < 300ms)
8. Operador solta o botão → frontend envia comando de stop
```

---

## 4. Referências Cruzadas

- Épicos completos: [PRD.md#epicos](./PRD.md#epicos)
- Endpoints usados nos fluxos: [API_CONTRACTS.md](./API_CONTRACTS.md)
- Entidades de dados: [DATA_MODEL.md](./DATA_MODEL.md)
- Permissões por role: [SECURITY_LGPD.md](./SECURITY_LGPD.md)
