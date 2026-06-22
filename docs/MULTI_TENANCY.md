<!-- Meta
Versão: v0.2.0
Última atualização: 2026-06-21
Documentos relacionados:
  - [Data Model](./DATA_MODEL.md)
  - [Security LGPD](./SECURITY_LGPD.md)
  - [API Contracts](./API_CONTRACTS.md)
  - [Arquitetura](./ARCHITECTURE.md)
  - [Media Pipeline](./MEDIA_PIPELINE.md)
-->

# Multi-Tenancy — Organizações {#multi-tenancy}

> **Mudança de nomenclatura (v0.2.0):** O conceito de "tenant" foi renomeado para **"organization"** (org) em todo o sistema. A tabela `tenants` virou `organizations`, o serviço `tenant-service` virou `organization-service`, e o campo `tenant_id` virou `org_id`. O JWT agora carrega `orgId` em vez de `tenantId`.

---

## 1. Estratégia de Isolamento {#estrategia}

A plataforma adota o modelo **Single Database, Shared Schema** com isolamento por **Row-Level Security (RLS)** do PostgreSQL.

| Estratégia | Isolamento | Complexidade de migração | Custo operacional | Decisão |
|---|---|---|---|---|
| Database por org | Alto | Alta (N migrations) | Alto (N conexões) | ❌ Descartado |
| Schema por org | Médio | Alta (DDL por schema) | Médio (pool limitado) | ❌ Descartado |
| **RLS (shared schema)** | **Alto** | **Baixa (1 migration)** | **Baixo** | **✅ Adotado** |

> **ADR:** RLS foi escolhido porque: (1) uma única migration atualiza todas as orgs, (2) PgBouncer funciona normalmente em transaction pooling mode, (3) PostgreSQL garante isolamento em nível de banco — bugs no código de aplicação não conseguem vazar dados entre orgs.

---

## 2. Modelo de Usuário — Muitos-para-Muitos (N:M) {#modelo-nm}

### 2.1 Motivação

Na versão anterior, cada usuário pertencia a **exatamente uma** organização (`tenant_id` direto em `auth.users`). Isso impedia casos como:

- CEO que gerencia várias empresas
- Consultor externo auditando múltiplos clientes
- Usuário admin do sistema com acesso a várias orgs

### 2.2 Modelo Atual — N:M via `user_memberships`

```
auth.users              auth.user_memberships        organizations.organizations
──────────────          ─────────────────────        ───────────────────────────
id (PK)        ◄──────  user_id (FK)                 id (PK)
email (UK)              org_id         ──────────►   slug
password_hash           role                          name
totp_enabled            active                        plan
...                     created_at                    ...
                        UNIQUE(user_id, org_id)
```

**Benefícios:**
- Um usuário pode pertencer a várias orgs com roles diferentes em cada uma
- Remoção de membro = `active = false` na membership (não deleta o usuário)
- Roles por org: ADMIN em "Empresa A", VIEWER em "Empresa B"

### 2.3 Tabela `auth.user_memberships`

```sql
CREATE TABLE auth.user_memberships (
    id         UUID     PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID     NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    org_id     UUID     NOT NULL,
    role       VARCHAR  NOT NULL CHECK (role IN ('ADMIN', 'OPERATOR', 'VIEWER')),
    active     BOOLEAN  NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, org_id)
);
```

---

## 3. Fluxo de Autenticação {#fluxo-auth}

### 3.1 Login com Org Picker (Novo Fluxo)

```
POST /api/v1/auth/login { email, password }
        │
        ▼
Busca user por email (global — sem tenant_id)
        │
        ├── Valida senha + lockout
        │
        ├── Carrega user_memberships ativas
        │
        ├── Se 1 org  → emite JWT scoped diretamente
        │                { access_token, org_id, role, ... }
        │
        └── Se N orgs → retorna lista para o picker
                         { requires_org_selection: true,
                           temp_token: "...",
                           orgs: [{ id, name, slug, logo_url, role }] }

[Usuário seleciona org no picker estilo Netflix]

POST /api/v1/auth/select-org { temp_token, org_id }
        │
        ▼
Valida temp_token + verifica membership ativa
        │
        ▼
Emite JWT scoped: { access_token, org_id, role, ... }
```

### 3.2 JWT Claims

```json
{
  "sub": "<user_id>",
  "orgId": "<org_id>",
  "role": "ADMIN",
  "email": "usuario@empresa.com",
  "iss": "fofoqueiro",
  "exp": 1234567890
}
```

> **Importante:** O claim agora é `orgId`, não mais `tenantId`. Tokens antigos com `tenantId` não são aceitos pelos serviços.

### 3.3 Injeção de org_id → RLS

```
Request com JWT válido
        │
        ▼
JwtAuthFilter
  → extrai claim "orgId"
  → OrgContext.set(UUID.fromString(orgId))
        │
        ▼
Service método executa
  → em qualquer query que precise de RLS:
    SET LOCAL app.current_org_id = '<uuid>'
        │
        ▼
PostgreSQL RLS Policy:
  USING (org_id = current_setting('app.current_org_id', TRUE)::UUID)
```

### 3.4 Implementação Spring Boot

```java
// OrgContext.java — ThreadLocal para armazenar a org da request atual
public final class OrgContext {
    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    public static void set(UUID orgId) { CURRENT.set(orgId); }
    public static UUID get() { return CURRENT.get(); }
    public static void clear() { CURRENT.remove(); }
}

// JwtAuthFilter.java — extrai orgId do JWT e configura contexto
@Override
protected void doFilterInternal(...) {
    Claims claims = jwtService.validateAndExtractClaims(token);
    String orgId = claims.get("orgId", String.class);
    if (orgId != null) {
        OrgContext.set(UUID.fromString(orgId));
    }
    try {
        chain.doFilter(request, response);
    } finally {
        OrgContext.clear(); // SEMPRE limpar após request
    }
}
```

---

## 4. Política RLS — Exemplo com cameras {#rls-exemplo}

```sql
-- Habilitar RLS na tabela
ALTER TABLE cameras.cameras ENABLE ROW LEVEL SECURITY;

-- Política usando org_id
CREATE POLICY org_isolation_cameras ON cameras.cameras
    AS PERMISSIVE FOR ALL
    USING (
        org_id = current_setting('app.current_org_id', TRUE)::UUID
    )
    WITH CHECK (
        org_id = current_setting('app.current_org_id', TRUE)::UUID
    );
```

**RLS aplicada em todas as tabelas com org_id:**
- `cameras.cameras`, `cameras.locations`, `cameras.privacy_zones`
- `health.camera_health_state`, `health.health_events`
- `recordings.recordings`
- `alerts.alerts`
- `audit.audit_logs` (sem RLS — imutável por design)

> **Nota:** `auth.users` **não tem RLS** — a coluna `org_id` foi removida dela (agora em `user_memberships`). A isolation de users ocorre pela membership.

---

## 5. Onboarding de Nova Organização {#onboarding}

```
Admin do Sistema → POST /api/v1/admin/organizations
{
  "name": "Segurança ABC Ltda",
  "slug": "seguranca-abc",
  "plan": "PRO",
  "domain": "monitor.seguranca-abc.com.br"
}
        │
        ├── 1. Valida unicidade de slug e domain
        ├── 2. INSERT INTO organizations.organizations
        ├── 3. Cria usuário admin:
        │      INSERT INTO auth.users (email, password_hash, ...)
        │      INSERT INTO auth.user_memberships (user_id, org_id, role='ADMIN')
        ├── 4. Envia e-mail de boas-vindas
        └── 5. Retorna org criada

Owner faz login → picker mostra sua org → acessa dashboard
```

---

## 6. White-Label — Carregamento Dinâmico por Domínio {#white-label}

Quando o browser acessa `https://monitor.seguranca-abc.com.br`:

1. Frontend faz `GET /api/v1/organizations/config?domain=monitor.seguranca-abc.com.br` (público, sem auth)
2. Backend consulta `SELECT logo_url, css_override, name FROM organizations WHERE domain = ?`
3. Frontend injeta CSS e logo da organização

```css
/* css_override — variáveis de tema */
:root {
  --primary: #1a73e8;
  --primary-foreground: #ffffff;
  --background: #0a0a0a;
}
```

---

## 7. Isolamento de Streams de Mídia {#isolamento-midia}

O MediaMTX organiza paths de stream com o org_id no prefixo:

```
RTSP:    /org_{org_id}/camera_{camera_id}/main
HLS:     https://media.domain.com/org_{org_id}/camera_{camera_id}/main/index.m3u8
WebRTC:  https://media.domain.com/org_{org_id}/camera_{camera_id}/main/whep
```

**Autenticação de stream via webhook:**

```
MediaMTX → POST /internal/media/auth
{
  "action": "read",
  "path": "/org_abc123/camera_xyz789/main",
  "token": "eyJ..."
}

Backend valida:
  1. JWT válido e com orgId
  2. org_id no path == orgId no JWT
  3. camera_id pertence à org
```

---

## 8. Planos e Limites por Organização {#planos}

| Limite | FREE | STARTER | PRO | ENTERPRISE |
|---|---|---|---|---|
| Câmeras máximas | 4 | 16 | 64 | Ilimitado |
| Usuários máximos | 2 | 5 | 20 | Ilimitado |
| Retenção de gravações | 7 dias | 30 dias | 90 dias | 365 dias |
| White-label | ❌ | ✅ básico (logo) | ✅ completo | ✅ + domínio próprio |
| API Access | ❌ | ❌ | ✅ | ✅ |

```java
// CameraService.java — verificação de limite
public Camera createCamera(CreateCameraRequest request, UUID orgId) {
    Organization org = orgRepository.findById(orgId).orElseThrow();
    long currentCount = cameraRepository.countActiveByOrgId(orgId);

    if (currentCount >= org.getMaxCameras()) {
        throw new PlanLimitExceededException(
            "Plano " + org.getPlan() + " permite no máximo " + org.getMaxCameras() + " câmeras"
        );
    }
}
```

---

## 9. Referências Cruzadas

- Entidade organizations completa: [DATA_MODEL.md#organizations](./DATA_MODEL.md#organizations)
- Segurança e LGPD: [SECURITY_LGPD.md](./SECURITY_LGPD.md)
- Endpoints: [API_CONTRACTS.md#organizations](./API_CONTRACTS.md#organizations)
- Isolamento de mídia: [MEDIA_PIPELINE.md](./MEDIA_PIPELINE.md)
