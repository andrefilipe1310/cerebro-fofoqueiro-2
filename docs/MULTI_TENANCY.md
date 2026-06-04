<!-- Meta
Versão: v0.1.0
Última atualização: 2026-06-04
Documentos relacionados:
  - [Data Model](./DATA_MODEL.md)
  - [Security LGPD](./SECURITY_LGPD.md)
  - [API Contracts](./API_CONTRACTS.md)
  - [Arquitetura](./ARCHITECTURE.md)
  - [Media Pipeline](./MEDIA_PIPELINE.md)
-->

# Multi-Tenancy {#multi-tenancy}

## 1. Estratégia de Isolamento {#estrategia}

A plataforma adota o modelo **Single Database, Shared Schema** com isolamento por **Row-Level Security (RLS)** do PostgreSQL.

| Estratégia | Isolamento | Complexidade de migração | Custo operacional | Decisão |
|---|---|---|---|---|
| Database por tenant | Alto | Alta (N migrations) | Alto (N conexões) | ❌ Descartado |
| Schema por tenant | Médio | Alta (DDL por schema) | Médio (pool limitado) | ❌ Descartado |
| **RLS (shared schema)** | **Alto** | **Baixa (1 migration)** | **Baixo** | **✅ Adotado** |

> **ADR:** RLS foi escolhido porque: (1) uma única migration atualiza todos os tenants, (2) PgBouncer funciona normalmente em transaction pooling mode, (3) PostgreSQL garante o isolamento em nível de banco — bugs no código de aplicação não conseguem vazar dados entre tenants mesmo sem filtro `WHERE` explícito.

---

## 2. Como o tenant_id é Injetado {#injecao-tenant}

### 2.1 Fluxo de Autenticação → RLS

```
Request HTTP chega com: Authorization: Bearer <JWT>
        │
        ▼
JwtAuthenticationFilter (Spring Security)
        │ valida JWT, extrai claims
        ▼
TenantContext.setCurrentTenantId(jwt.getClaim("tenant_id"))
        │ armazena em ThreadLocal
        ▼
HibernateTenantConnectionProvider (antes de cada query)
        │ executa: SET LOCAL app.current_tenant_id = '<uuid>'
        ▼
PostgreSQL RLS Policy avalia: tenant_id = current_setting('app.current_tenant_id')
        │
        ▼
Query retorna apenas dados do tenant correto
```

### 2.2 Implementação Spring Boot

```java
// TenantContext.java — ThreadLocal para armazenar o tenant da request atual
public class TenantContext {
    private static final ThreadLocal<UUID> currentTenantId = new ThreadLocal<>();

    public static void setCurrentTenantId(UUID tenantId) {
        currentTenantId.set(tenantId);
    }

    public static UUID getCurrentTenantId() {
        return currentTenantId.get();
    }

    public static void clear() {
        currentTenantId.remove();
    }
}

// JwtAuthenticationFilter.java — extrai tenant_id do JWT e configura contexto
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws Exception {
        String token = extractToken(request);
        if (token != null && jwtService.isValid(token)) {
            UUID tenantId = jwtService.extractTenantId(token);
            TenantContext.setCurrentTenantId(tenantId);
            // configurar SecurityContext...
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear(); // SEMPRE limpar após request
        }
    }
}

// TenantAwareJpaInterceptor.java — define variável de sessão PostgreSQL
@Component
public class TenantAwareJpaInterceptor implements EmptyInterceptor {
    @Override
    public void beforeTransactionCompletion(Transaction tx) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        if (tenantId != null) {
            // Executado antes de cada query dentro da transação
            entityManager.createNativeQuery(
                "SET LOCAL app.current_tenant_id = '" + tenantId + "'"
            ).executeUpdate();
        }
    }
}
```

---

## 3. Política RLS Completa — Exemplo com cameras {#rls-exemplo}

```sql
-- Habilitar RLS na tabela
ALTER TABLE cameras ENABLE ROW LEVEL SECURITY;

-- Política para usuários da aplicação (role: app_user)
CREATE POLICY tenant_isolation_cameras ON cameras
    AS PERMISSIVE
    FOR ALL
    TO app_user
    USING (
        tenant_id = current_setting('app.current_tenant_id', TRUE)::UUID
    )
    WITH CHECK (
        tenant_id = current_setting('app.current_tenant_id', TRUE)::UUID
    );

-- Superuser (migrations, admin interno) ignora RLS:
-- BYPASSRLS atributo no role de migrations

-- Verificação: tentativa de acessar câmera de outro tenant retorna 0 rows
-- (não lança erro — comportamento padrão do RLS)
```

**Política aplicada em todas as tabelas com tenant_id:**
- `users` — via [DATA_MODEL.md#users](./DATA_MODEL.md#users)
- `cameras` — via [DATA_MODEL.md#cameras](./DATA_MODEL.md#cameras)
- `locations` — via [DATA_MODEL.md#locations](./DATA_MODEL.md#locations)
- `recordings` — via [DATA_MODEL.md#recordings](./DATA_MODEL.md#recordings)
- `health_events` — via [DATA_MODEL.md#health_events](./DATA_MODEL.md#health_events)
- `audit_logs` — via [DATA_MODEL.md#audit_logs](./DATA_MODEL.md#audit_logs)
- `privacy_zones` — via [DATA_MODEL.md#privacy_zones](./DATA_MODEL.md#privacy_zones)
- `alerts` — via [DATA_MODEL.md#alerts](./DATA_MODEL.md#alerts)

---

## 4. Fluxo de Onboarding de Novo Tenant {#onboarding}

```
Admin do Sistema acessa /admin/tenants → clica "Novo Tenant"
        │
        ▼
POST /api/v1/admin/tenants
{
  "name": "Segurança ABC Ltda",
  "owner_email": "owner@seguranca-abc.com.br",
  "plan": "PRO",
  "domain": "monitor.seguranca-abc.com.br"  // opcional
}
        │
        ├── 1. Validar unicidade de slug (gerado do name) e domain
        ├── 2. INSERT INTO tenants (slug, name, domain, plan, status='ACTIVE')
        ├── 3. INSERT INTO users (tenant_id, email, role='ADMIN', active=true)
        │      → senha temporária gerada (ou link de definição de senha)
        ├── 4. Enviar e-mail de boas-vindas com link: /first-access?token=<uuid>
        │      Token válido por 24h, uso único
        └── 5. Retornar tenant criado
        │
        ▼
Owner acessa link de first-access
        │
        ├── Define senha (mínimo 12 chars, 1 maiúscula, 1 número, 1 especial)
        ├── Configura 2FA obrigatório (TOTP): POST /api/v1/auth/2fa/setup
        └── Redirecionado para /dashboard (vazio, sem câmeras ainda)
```

---

## 5. White-Label — Carregamento Dinâmico por Domínio {#white-label}

### 5.1 Como Funciona

Quando o browser acessa `https://monitor.seguranca-abc.com.br`, o frontend:

1. Faz request para `GET /api/v1/tenants/config?domain=monitor.seguranca-abc.com.br` (sem auth — endpoint público para carregar branding)
2. Backend consulta `SELECT logo_url, css_override, name FROM tenants WHERE domain = ?`
3. Frontend injeta no `<head>`:

```html
<!-- Logo dinâmica -->
<link rel="icon" href="{logo_url}">

<!-- CSS customizado do tenant -->
<style id="tenant-theme">
  {css_override}
  /* Exemplo: body { --primary: #1a73e8; --brand-name: "Segurança ABC"; } */
</style>
```

4. Componentes React usam as variáveis CSS para renderizar com a identidade visual do tenant

### 5.2 Configuração de Domínio Customizado

Para ativar `monitor.seguranca-abc.com.br`:

1. Tenant configura CNAME: `monitor.seguranca-abc.com.br → app.{plataforma}.com`
2. Admin do sistema atualiza `tenants.domain = 'monitor.seguranca-abc.com.br'`
3. Nginx já tem wildcard TLS (Cloudflare proxy lida com o certificado do subdomínio customizado)

### 5.3 Estrutura do css_override

```css
/* Variáveis de tema que os componentes Shadcn/ui respeitam */
:root {
  --primary: #1a73e8;
  --primary-foreground: #ffffff;
  --secondary: #f0f4ff;
  --background: #0a0a0a;
  --card: #1a1a2e;
}
```

---

## 6. Isolamento de Streams de Mídia {#isolamento-midia}

O MediaMTX organiza os paths de stream com o tenant_id no prefixo, garantindo que um tenant não acesse o stream de outro:

```
RTSP path: /tenant_{tenant_id}/camera_{camera_id}/main
           /tenant_{tenant_id}/camera_{camera_id}/sub

HLS URL:   https://media.domain.com/tenant_{tenant_id}/camera_{camera_id}/main/index.m3u8

WebRTC:    https://media.domain.com/tenant_{tenant_id}/camera_{camera_id}/main/whep
```

**Autenticação de stream via webhook:**

Quando o browser tenta acessar um path, o MediaMTX chama um webhook no backend:

```
MediaMTX → POST https://api.domain.com/internal/media/auth
{
  "action": "read",
  "path": "/tenant_abc123/camera_xyz789/main",
  "token": "eyJ..."  // JWT passado como query param pelo frontend
}

Backend valida:
  1. JWT válido
  2. tenant_id no path == tenant_id no JWT (evita tenant A acessar câmera do tenant B)
  3. camera_id existe e pertence ao tenant

Resposta 200 = autorizado, 403 = negado
```

---

## 7. Planos e Limites por Tenant {#planos}

Os limites são verificados no backend ao criar câmeras/usuários. Nunca confiar no frontend para enforcement.

| Limite | FREE | STARTER | PRO | ENTERPRISE |
|---|---|---|---|---|
| Câmeras máximas | 4 | 16 | 64 | Ilimitado |
| Usuários máximos | 2 | 5 | 20 | Ilimitado |
| Retenção de gravações | 7 dias | 30 dias | 90 dias | 365 dias |
| White-label | ❌ | ✅ básico (logo) | ✅ completo (CSS + logo) | ✅ + domínio próprio |
| Suporte SLA | Community | E-mail | E-mail prioritário | Dedicado 24/7 |
| API Access | ❌ | ❌ | ✅ | ✅ |
| PTZ | ✅ | ✅ | ✅ | ✅ |

**Verificação de limite no Spring Boot:**

```java
// CameraService.java
public Camera createCamera(CreateCameraRequest request, UUID tenantId) {
    Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
    int currentCount = cameraRepository.countByTenantId(tenantId);
    int maxCameras = PlanLimits.maxCameras(tenant.getPlan());

    if (currentCount >= maxCameras) {
        throw new PlanLimitExceededException(
            "Plano " + tenant.getPlan() + " permite no máximo " + maxCameras + " câmeras"
        );
    }
    // continuar com criação...
}
```

---

## 8. Referências Cruzadas

- Entidade tenants completa: [DATA_MODEL.md#tenants](./DATA_MODEL.md#tenants)
- Segurança e LGPD: [SECURITY_LGPD.md](./SECURITY_LGPD.md)
- Endpoints relacionados: [API_CONTRACTS.md#tenants-current](./API_CONTRACTS.md#tenants-current)
- Isolamento de mídia detalhado: [MEDIA_PIPELINE.md](./MEDIA_PIPELINE.md)
