<!-- Meta
Versão: v0.2.0
Última atualização: 2026-06-21
Documentos relacionados:
  - [Data Model](./DATA_MODEL.md)
  - [Security LGPD](./SECURITY_LGPD.md)
  - [User Stories](./USER_STORIES.md)
  - [Arquitetura](./ARCHITECTURE.md)
-->

# API Contracts {#api-contracts}

## 1. Convenções Gerais {#convencoes}

### 1.1 Base URL e Versionamento

```
https://api.{domain}/api/v1/
```

- Todos os endpoints são prefixados com `/api/v1/`
- Versão incrementada apenas para mudanças incompatíveis (v2, v3...)
- Ambiente local: `http://localhost:8080/api/v1/`

### 1.2 Autenticação

Todos os endpoints (exceto `/auth/*`) exigem header:

```
Authorization: Bearer <access_token>
```

O `access_token` é um JWT com validade de 15 minutos. Use o refresh token para renovar. Ver [SECURITY_LGPD.md](./SECURITY_LGPD.md).

### 1.3 Paginação (cursor-based)

```json
{
  "data": [...],
  "pagination": {
    "next_cursor": "eyJpZCI6IjEyMyJ9",
    "has_more": true,
    "limit": 20
  }
}
```

Request com paginação: `GET /api/v1/cameras?limit=20&cursor=eyJpZCI6IjEyMyJ9`

### 1.4 Formato de Erros Padrão

```json
{
  "error": {
    "code": "CAMERA_NOT_FOUND",
    "message": "Câmera com ID 'xyz' não encontrada ou sem permissão de acesso",
    "status": 404,
    "timestamp": "2026-06-04T14:30:00Z",
    "request_id": "req_abc123"
  }
}
```

### 1.5 Códigos de Erro HTTP

| Status | Uso |
|---|---|
| 200 | Sucesso com corpo |
| 201 | Recurso criado com sucesso |
| 204 | Sucesso sem corpo (DELETE, PATCH sem retorno) |
| 400 | Dados inválidos na request (validação) |
| 401 | Não autenticado (token ausente ou expirado) |
| 403 | Autenticado mas sem permissão para o recurso |
| 404 | Recurso não encontrado (ou RLS ocultando) |
| 409 | Conflito (ex: e-mail já cadastrado) |
| 422 | Entidade não processável (lógica de negócio) |
| 429 | Rate limit atingido |
| 500 | Erro interno do servidor |

---

## 2. Autenticação e Sessão {#auth-endpoints}

### POST /api/v1/auth/login {#auth-login}

Autentica o usuário com e-mail + senha. **Não requer slug de organização** — o sistema descobre as orgs do usuário automaticamente.

**Auth:** Nenhuma

**Request Body:**
```json
{
  "email": "operador@empresa.com",
  "password": "senha_segura"
}
```

**Response 200 — Usuário com 1 org (auto-seleção):**
```json
{
  "access_token": "eyJ...",
  "refresh_token": "eyJ...",
  "expires_in": 900,
  "requires_2fa": false,
  "requires_org_selection": false,
  "user_id": "uuid",
  "org_id": "uuid",
  "role": "OPERATOR"
}
```

**Response 200 — Usuário com múltiplas orgs (picker):**
```json
{
  "requires_org_selection": true,
  "temp_token": "eyJ...",
  "user_id": "uuid",
  "orgs": [
    { "id": "uuid-1", "name": "Empresa A", "slug": "empresa-a", "logo_url": null, "role": "ADMIN" },
    { "id": "uuid-2", "name": "Empresa B", "slug": "empresa-b", "logo_url": "https://...", "role": "VIEWER" }
  ]
}
```

**Response 200 — 2FA pendente:**
```json
{
  "requires_2fa": true,
  "temp_token": "eyJ...",
  "user_id": "uuid"
}
```

**Erros:** 401 (credenciais inválidas), 403 (conta desativada), 422 (sem org ativa)

---

### POST /api/v1/auth/select-org {#auth-select-org}

Seleciona a organização após o org picker. Emite JWT scoped para a org escolhida.

**Auth:** Nenhuma (temp_token no body)

**Request Body:**
```json
{
  "temp_token": "eyJ...",
  "org_id": "uuid"
}
```

**Response 200:**
```json
{
  "access_token": "eyJ...",
  "refresh_token": "eyJ...",
  "expires_in": 900,
  "user_id": "uuid",
  "org_id": "uuid",
  "role": "ADMIN"
}
```

**Erros:** 401 (temp_token inválido ou expirado), 403 (org_id não pertence ao usuário)

---

### POST /api/v1/auth/2fa/verify {#auth-2fa}

Verifica o código TOTP após login. Retorna access token completo.

**Auth:** Bearer (token temporário do login)

**Request Body:**
```json
{
  "code": "123456"
}
```

**Response 200:**
```json
{
  "access_token": "eyJ...",
  "refresh_token": "eyJ...",
  "expires_in": 900
}
```

**Erros:** 401 (código inválido), 429 (muitas tentativas)

---

### POST /api/v1/auth/refresh {#auth-refresh}

Renova o access token usando o refresh token.

**Auth:** Nenhuma (refresh token no body)

**Request Body:**
```json
{
  "refresh_token": "eyJ..."
}
```

**Response 200:**
```json
{
  "access_token": "eyJ...",
  "expires_in": 900
}
```

**Erros:** 401 (refresh token inválido ou expirado)

---

### POST /api/v1/auth/2fa/setup {#auth-2fa-setup}

Gera o QR code para configurar 2FA. Obrigatório no primeiro acesso de ADMIN/OPERATOR.

**Auth:** Bearer (access token completo)

**Response 200:**
```json
{
  "qr_code_url": "data:image/png;base64,...",
  "secret": "JBSWY3DPEHPK3PXP",
  "backup_codes": ["abc123", "def456", "..."]
}
```

---

## 3. Câmeras {#camera-endpoints}

### GET /api/v1/cameras {#cameras-list}

Lista câmeras do tenant autenticado. RLS garante isolamento automático.
Ver entidade [cameras](./DATA_MODEL.md#cameras).

**Auth:** Bearer | **Roles:** ADMIN, OPERATOR, VIEWER

**Query Params:**
- `status` (opcional): ONLINE | OFFLINE | UNKNOWN
- `location_id` (opcional): UUID da localização
- `limit` (padrão: 20, máx: 100)
- `cursor` (opcional): cursor de paginação

**Response 200:**
```json
{
  "data": [
    {
      "id": "uuid",
      "name": "Portaria Norte",
      "status": "ONLINE",
      "ptz_enabled": true,
      "lat": -23.5505,
      "lng": -46.6333,
      "location": {
        "id": "uuid",
        "name": "Sede Principal"
      },
      "thumbnail_url": "https://r2.../tenant/camera/thumb_latest.jpg",
      "created_at": "2026-01-15T10:00:00Z"
    }
  ],
  "pagination": { "next_cursor": null, "has_more": false, "limit": 20 }
}
```

**Nota de segurança:** `rtsp_url` nunca é retornada na listagem. Ver [SECURITY_LGPD.md](./SECURITY_LGPD.md).

---

### POST /api/v1/cameras {#cameras-create}

Cadastra nova câmera. Backend valida conectividade RTSP antes de salvar.

**Auth:** Bearer | **Roles:** ADMIN

**Request Body:**
```json
{
  "name": "Portaria Norte",
  "location_id": "uuid",
  "rtsp_url": "rtsp://admin:senha@192.168.1.100:554/stream1",
  "sub_stream_url": "rtsp://admin:senha@192.168.1.100:554/stream2",
  "lat": -23.5505,
  "lng": -46.6333,
  "ptz_enabled": false
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "name": "Portaria Norte",
  "status": "ONLINE",
  "created_at": "2026-06-04T14:30:00Z"
}
```

**Erros:** 422 (limite de câmeras do plano atingido), 400 (RTSP URL inválida ou inacessível)

---

### GET /api/v1/cameras/{id} {#cameras-get}

Retorna detalhes de uma câmera específica.

**Auth:** Bearer | **Roles:** ADMIN, OPERATOR, VIEWER

**Response 200:**
```json
{
  "id": "uuid",
  "name": "Portaria Norte",
  "status": "ONLINE",
  "ptz_enabled": true,
  "lat": -23.5505,
  "lng": -46.6333,
  "location": { "id": "uuid", "name": "Sede Principal" },
  "privacy_zones_count": 2,
  "health": {
    "last_seen_at": "2026-06-04T14:29:45Z",
    "recording_confidence": 98.5
  }
}
```

---

### GET /api/v1/cameras/{id}/stream/live {#cameras-stream-live}

Retorna URL do stream ao vivo (WebRTC via WHEP).

**Auth:** Bearer | **Roles:** ADMIN, OPERATOR, VIEWER

**Response 200:**
```json
{
  "type": "webrtc",
  "url": "https://media.domain.com/org_abc/camera_xyz/main/whep",
  "expires_at": "2026-06-04T15:30:00Z",
  "fallback_hls": "https://media.domain.com/org_abc/camera_xyz/main/index.m3u8"
}
```

A URL gerada contém um token assinado válido por 1 hora. MediaMTX valida via webhook de auth. Ver [MEDIA_PIPELINE.md](./MEDIA_PIPELINE.md).

---

### GET /api/v1/cameras/{id}/stream/hls/{quality} {#cameras-stream-hls}

Retorna URL de stream HLS. Usado para sub-streams no mosaico.

**Auth:** Bearer | **Roles:** ADMIN, OPERATOR, VIEWER

**Path Params:**
- `quality`: `main` (alta resolução) | `sub` (baixa resolução para mosaico)

**Response 200:**
```json
{
  "type": "hls",
  "url": "https://media.domain.com/org_abc/camera_xyz/sub/index.m3u8",
  "expires_at": "2026-06-04T15:30:00Z"
}
```

---

### GET /api/v1/cameras/{id}/health {#cameras-health}

Retorna estado de saúde atual da câmera. Ver entidade [health_events](./DATA_MODEL.md#health_events).

**Auth:** Bearer | **Roles:** ADMIN, OPERATOR

**Response 200:**
```json
{
  "camera_id": "uuid",
  "status": "ONLINE",
  "last_seen_at": "2026-06-04T14:29:45Z",
  "recording_confidence": 98.5,
  "active_events": [
    {
      "id": "uuid",
      "type": "LOW_CONFIDENCE",
      "severity": "WARNING",
      "detected_at": "2026-06-04T12:00:00Z",
      "message": "Câmera gravou apenas 75% do tempo esperado nas últimas 24h"
    }
  ]
}
```

---

### GET /api/v1/cameras/{id}/timeline {#cameras-timeline}

Retorna mapa de disponibilidade de gravações para a timeline visual.

**Auth:** Bearer | **Roles:** ADMIN, OPERATOR, VIEWER

**Query Params:**
- `from`: ISO8601 datetime (obrigatório)
- `to`: ISO8601 datetime (obrigatório, máx. 7 dias de intervalo)

**Response 200:**
```json
{
  "camera_id": "uuid",
  "from": "2026-06-04T00:00:00Z",
  "to": "2026-06-04T23:59:59Z",
  "segments": [
    {
      "started_at": "2026-06-04T00:00:00Z",
      "ended_at": "2026-06-04T02:15:30Z",
      "recording_id": "uuid",
      "has_motion": false
    },
    {
      "started_at": "2026-06-04T02:18:00Z",
      "ended_at": "2026-06-04T06:00:00Z",
      "recording_id": "uuid",
      "has_motion": true
    }
  ],
  "gaps": [
    {
      "from": "2026-06-04T02:15:30Z",
      "to": "2026-06-04T02:18:00Z",
      "reason": "CAMERA_OFFLINE"
    }
  ]
}
```

---

### GET /api/v1/cameras/{id}/recordings {#cameras-recordings}

Lista gravações de uma câmera. Ver entidade [recordings](./DATA_MODEL.md#recordings).

**Auth:** Bearer | **Roles:** ADMIN, OPERATOR, VIEWER

**Query Params:**
- `from`, `to`: intervalo de tempo (ISO8601)
- `limit`, `cursor`: paginação

**Response 200:**
```json
{
  "data": [
    {
      "id": "uuid",
      "started_at": "2026-06-04T00:00:00Z",
      "ended_at": "2026-06-04T02:15:30Z",
      "duration_seconds": 8130,
      "size_bytes": 524288000,
      "thumbnail_url": "https://r2.../thumb.jpg"
    }
  ],
  "pagination": { "next_cursor": null, "has_more": false, "limit": 20 }
}
```

---

### GET /api/v1/cameras/{camera_id}/recordings/{recording_id}/download {#recordings-download}

Gera URL de download temporária (signed URL do R2).

**Auth:** Bearer | **Roles:** ADMIN, OPERATOR

**Response 200:**
```json
{
  "download_url": "https://r2.cloudflare.com/...?X-Amz-Signature=...",
  "expires_at": "2026-06-04T15:00:00Z",
  "filename": "camera_portaria-norte_2026-06-04_00-00.mp4"
}
```

Log de auditoria registra o download. Ver [audit_logs](./DATA_MODEL.md#audit_logs).

---

### POST /api/v1/cameras/{id}/ptz {#cameras-ptz}

Envia comando de controle PTZ para câmera. Requer `ptz_enabled = true`.

**Auth:** Bearer | **Roles:** ADMIN, OPERATOR

**Request Body:**
```json
{
  "direction": "left",
  "speed": 5,
  "action": "start"
}
```

`direction`: `left | right | up | down | zoom_in | zoom_out`
`action`: `start | stop`

**Response 204:** (sem corpo)

---

## 4. Organizações {#organizations}

### GET /api/v1/organizations/me {#organizations-me}

Retorna dados da organização atual (scoped pelo JWT). Ver entidade [organizations](./DATA_MODEL.md#organizations).

**Auth:** Bearer | **Roles:** ADMIN, OPERATOR, VIEWER

**Response 200:**
```json
{
  "id": "uuid",
  "name": "Segurança ABC Ltda",
  "slug": "seguranca-abc",
  "plan": "PRO",
  "logo_url": "https://r2.../org/logo.png",
  "max_cameras": 64,
  "max_users": 20,
  "retention_days": 90,
  "status": "ACTIVE"
}
```

---

### PUT /api/v1/organizations/me {#organizations-update}

Atualiza nome, logo e CSS da organização (white-label).

**Auth:** Bearer | **Roles:** ADMIN

**Request Body:**
```json
{
  "name": "Segurança ABC Ltda",
  "logo_url": "https://r2.../org/logo.png",
  "css_override": "body { --primary: #1a73e8; }"
}
```

**Response 200:** mesmo formato do GET.

---

### GET /api/v1/organizations/config {#organizations-config}

Endpoint **público** (sem auth) para resolução de white-label por domínio ou slug.

**Query Params:**
- `domain`: domínio customizado (ex: `monitor.empresa.com`)
- `slug`: slug da org (ex: `seguranca-abc`)

**Response 200:**
```json
{
  "id": "uuid",
  "name": "Segurança ABC Ltda",
  "slug": "seguranca-abc",
  "logo_url": "https://r2.../org/logo.png",
  "css_override": ":root { --primary: #1a73e8; }"
}
```

---

## 5. Alertas {#alert-endpoints}

### GET /api/v1/alerts {#alerts-list}

Lista alertas do tenant. Ver entidade [alerts](./DATA_MODEL.md#alerts).

**Auth:** Bearer | **Roles:** ADMIN, OPERATOR

**Query Params:**
- `acknowledged`: `true | false` (filtrar por status)
- `type`: `CAMERA_OFFLINE | MOTION_DETECTED | ...`
- `camera_id`: filtrar por câmera
- `limit`, `cursor`

**Response 200:**
```json
{
  "data": [
    {
      "id": "uuid",
      "type": "CAMERA_OFFLINE",
      "message": "Câmera 'Portaria Norte' está offline há 5 minutos",
      "camera": { "id": "uuid", "name": "Portaria Norte" },
      "triggered_at": "2026-06-04T14:00:00Z",
      "acknowledged_at": null
    }
  ],
  "pagination": { "next_cursor": null, "has_more": false, "limit": 20 }
}
```

---

### PATCH /api/v1/alerts/{id}/acknowledge {#alerts-ack}

Reconhece um alerta, indicando que o operador está ciente.

**Auth:** Bearer | **Roles:** ADMIN, OPERATOR

**Response 200:**
```json
{
  "id": "uuid",
  "acknowledged_at": "2026-06-04T14:05:00Z",
  "acknowledged_by": { "id": "uuid", "email": "operador@empresa.com" }
}
```

---

## 6. Logs de Auditoria {#audit-endpoints}

### GET /api/v1/audit-logs {#audit-logs-list}

Consulta logs de auditoria. Ver entidade [audit_logs](./DATA_MODEL.md#audit_logs).

**Auth:** Bearer | **Roles:** ADMIN

**Query Params:**
- `user_id`: filtrar por usuário
- `action`: ex: `VIEW_CAMERA`, `DOWNLOAD_RECORDING`
- `from`, `to`: intervalo de tempo
- `limit`, `cursor`

**Response 200:**
```json
{
  "data": [
    {
      "id": "uuid",
      "user": { "id": "uuid", "email": "operador@empresa.com" },
      "action": "DOWNLOAD_RECORDING",
      "resource_type": "RECORDING",
      "resource_id": "uuid",
      "ip_address": "189.10.20.30",
      "occurred_at": "2026-06-04T14:00:00Z"
    }
  ],
  "pagination": { "next_cursor": null, "has_more": false, "limit": 20 }
}
```

---

## 7. WebSocket — Alertas em Tempo Real {#websocket}

### WS /ws/alerts {#ws-alerts}

Conexão WebSocket usando protocolo STOMP sobre SockJS.

**Auth:** Token JWT passado como query param: `/ws/alerts?token=eyJ...`

**Endpoint de conexão:** `wss://api.{domain}/ws/alerts`

**Tópico de assinatura (subscribe):**
```
/topic/org/{org_id}/alerts
```

**Formato da mensagem recebida:**
```json
{
  "type": "CAMERA_OFFLINE",
  "alert_id": "uuid",
  "camera_id": "uuid",
  "camera_name": "Portaria Norte",
  "message": "Câmera offline detectada",
  "triggered_at": "2026-06-04T14:00:00Z",
  "severity": "CRITICAL"
}
```

**Exemplo de conexão com STOMP.js:**
```typescript
import { Client } from '@stomp/stompjs';

const client = new Client({
  brokerURL: `wss://api.domain.com/ws/alerts?token=${accessToken}`,
  onConnect: () => {
    client.subscribe(`/topic/tenant/${tenantId}/alerts`, (message) => {
      const alert = JSON.parse(message.body);
      // exibir toast, atualizar mosaico, etc.
    });
  },
});

client.activate();
```

---

## 8. Referências Cruzadas

- Entidades retornadas nos endpoints: [DATA_MODEL.md](./DATA_MODEL.md)
- Campos sensíveis e restrições LGPD: [SECURITY_LGPD.md](./SECURITY_LGPD.md)
- Fluxos de usuário que usam estes endpoints: [USER_STORIES.md](./USER_STORIES.md)
- Autenticação detalhada: [SECURITY_LGPD.md#autenticacao](./SECURITY_LGPD.md)
