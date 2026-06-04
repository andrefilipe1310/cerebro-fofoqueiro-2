<!-- Meta
Versão: v0.1.0
Última atualização: 2026-06-04
Documentos relacionados:
  - [Tech Stack](./TECH_STACK.md)
  - [ENV Config](./ENV_CONFIG.md)
  - [Arquitetura](./ARCHITECTURE.md)
-->

# Padrões de Código {#code-style}

## 1. Estrutura de Pastas — Frontend (Next.js App Router) {#estrutura-frontend}

```
src/
├── app/                        # Rotas Next.js (App Router)
│   ├── (auth)/                 # Grupo de rotas sem layout principal
│   │   ├── login/
│   │   │   └── page.tsx
│   │   └── setup-2fa/
│   │       └── page.tsx
│   ├── (dashboard)/            # Rotas autenticadas com layout principal
│   │   ├── layout.tsx          # Layout com sidebar, header
│   │   ├── page.tsx            # Dashboard / mosaico
│   │   ├── cameras/
│   │   │   ├── page.tsx        # Lista de câmeras
│   │   │   └── [id]/
│   │   │       ├── page.tsx    # Detalhe da câmera
│   │   │       └── timeline/
│   │   │           └── page.tsx
│   │   ├── map/
│   │   │   └── page.tsx
│   │   ├── alerts/
│   │   │   └── page.tsx
│   │   └── settings/
│   │       └── page.tsx
│   ├── layout.tsx              # Root layout (providers, fonts)
│   └── globals.css
│
├── components/
│   ├── ui/                     # Componentes Shadcn/ui (copiados, não importados de lib)
│   │   ├── button.tsx
│   │   ├── card.tsx
│   │   └── ...
│   ├── features/               # Componentes de domínio
│   │   ├── cameras/
│   │   │   ├── CameraGrid.tsx
│   │   │   ├── CameraCell.tsx
│   │   │   ├── CameraPlayer.tsx
│   │   │   ├── CameraForm.tsx
│   │   │   └── PrivacyZoneEditor.tsx
│   │   ├── alerts/
│   │   │   ├── AlertsList.tsx
│   │   │   └── AlertToast.tsx
│   │   ├── timeline/
│   │   │   ├── TimelineBar.tsx
│   │   │   └── TimelinePlayer.tsx
│   │   └── map/
│   │       └── CameraMap.tsx
│   └── layouts/
│       ├── DashboardLayout.tsx
│       ├── Sidebar.tsx
│       └── Header.tsx
│
├── lib/
│   ├── api/                    # Funções de chamada de API (React Query + fetch)
│   │   ├── cameras.ts
│   │   ├── alerts.ts
│   │   ├── auth.ts
│   │   └── client.ts           # Axios/fetch configurado com interceptors
│   ├── hooks/                  # Custom hooks
│   │   ├── useCameras.ts
│   │   ├── useAlerts.ts
│   │   └── useWebSocket.ts
│   └── utils/
│       ├── date.ts
│       ├── format.ts
│       └── stream.ts           # Helpers de WebRTC/HLS
│
├── stores/                     # Zustand stores (apenas estado de UI)
│   ├── cameraStore.ts          # Câmera selecionada, layout do mosaico
│   ├── alertStore.ts           # Alertas não lidos (contagem)
│   └── uiStore.ts              # Sidebar aberta/fechada, tema
│
└── types/                      # Types TypeScript globais
    ├── api.ts                  # Tipos de response/request da API
    ├── camera.ts
    ├── alert.ts
    └── tenant.ts
```

---

## 2. Estrutura de Pastas — Backend (Spring Boot) {#estrutura-backend}

```
src/main/java/com/[produto]/
├── controller/                 # Controllers REST (@RestController)
│   ├── AuthController.java
│   ├── CameraController.java
│   ├── AlertController.java
│   ├── TenantController.java
│   └── AuditLogController.java
│
├── service/                    # Lógica de negócio (@Service)
│   ├── AuthService.java
│   ├── CameraService.java
│   ├── CameraHealthService.java  # CHMS
│   ├── RecordingService.java
│   ├── AlertService.java
│   └── TenantService.java
│
├── repository/                 # Repositórios JPA (@Repository)
│   ├── CameraRepository.java
│   ├── RecordingRepository.java
│   ├── AlertRepository.java
│   ├── AuditLogRepository.java
│   └── TenantRepository.java
│
├── domain/                     # Entidades JPA e Enums
│   ├── entity/
│   │   ├── Camera.java
│   │   ├── User.java
│   │   ├── Tenant.java
│   │   ├── Recording.java
│   │   ├── Alert.java
│   │   ├── HealthEvent.java
│   │   ├── AuditLog.java
│   │   └── PrivacyZone.java
│   └── enums/
│       ├── CameraStatus.java
│       ├── UserRole.java
│       ├── AlertType.java
│       ├── HealthEventType.java
│       └── TenantPlan.java
│
├── dto/                        # Data Transfer Objects
│   ├── request/
│   │   ├── CreateCameraRequest.java
│   │   ├── LoginRequest.java
│   │   └── UpdateBrandingRequest.java
│   └── response/
│       ├── CameraResponse.java
│       ├── CameraListResponse.java
│       ├── AlertResponse.java
│       └── StreamUrlResponse.java
│
├── config/                     # Configurações Spring
│   ├── SecurityConfig.java
│   ├── WebSocketConfig.java
│   ├── RedisConfig.java
│   ├── JpaConfig.java
│   └── CorsConfig.java
│
├── security/                   # Componentes de segurança
│   ├── JwtService.java
│   ├── JwtAuthenticationFilter.java
│   ├── TotpService.java
│   ├── TenantContext.java
│   └── UserDetailsServiceImpl.java
│
├── exception/                  # Exceções e handler global
│   ├── GlobalExceptionHandler.java   # @ControllerAdvice
│   ├── PlanLimitExceededException.java
│   ├── CameraNotFoundException.java
│   ├── UnauthorizedTenantException.java
│   └── MediaServerException.java
│
└── infrastructure/             # Integrações externas
    ├── mediamtx/
    │   └── MediaMtxClient.java
    ├── r2/
    │   └── CloudflareR2Client.java
    └── email/
        └── EmailService.java
```

---

## 3. Convenções de Nomenclatura {#nomenclatura}

### 3.1 Frontend (Next.js / TypeScript)

| Elemento | Convenção | Exemplo |
|---|---|---|
| Arquivos de rota | kebab-case | `camera-detail/page.tsx` |
| Componentes React | PascalCase | `CameraGrid.tsx` |
| Hooks customizados | camelCase com prefixo `use` | `useCameras.ts` |
| Funções e variáveis | camelCase | `fetchCameraList()` |
| Constantes | SCREAMING_SNAKE_CASE | `MAX_CAMERAS_PER_ROW` |
| Types e Interfaces | PascalCase | `CameraResponse`, `ApiError` |
| Stores Zustand | camelCase com sufixo `Store` | `cameraStore.ts` |
| CSS classes | kebab-case (Tailwind utility) | `text-sm font-medium` |

### 3.2 Backend (Java / Spring Boot)

| Elemento | Convenção | Exemplo |
|---|---|---|
| Classes | PascalCase | `CameraService` |
| Métodos | camelCase | `findCamerasByTenantId()` |
| Constantes | SCREAMING_SNAKE_CASE | `MAX_CAMERAS_FREE_PLAN = 4` |
| Pacotes | lowercase | `com.produto.service` |
| Enums | PascalCase para tipo, UPPER para valores | `CameraStatus.ONLINE` |
| Endpoints | kebab-case nas URLs | `/api/v1/audit-logs` |

### 3.3 Banco de Dados

| Elemento | Convenção | Exemplo |
|---|---|---|
| Tabelas | snake_case, plural | `health_events` |
| Colunas | snake_case | `tenant_id`, `created_at` |
| Índices | `idx_{tabela}_{colunas}` | `idx_cameras_tenant_status` |
| Foreign Keys | `{tabela}_id` | `camera_id`, `tenant_id` |
| Enums no banco | VARCHAR com valores UPPER | `'ONLINE'`, `'OFFLINE'` |

### 3.4 Git — Branches e Commits

**Branches:**
```
feat/CAM-123-adicionar-suporte-ptz
fix/CAM-456-corrigir-timeout-rtsp
chore/CAM-789-atualizar-dependencias
docs/CAM-321-documentar-api-alertas
refactor/CAM-654-extrair-servico-health
```

**Conventional Commits:**
```
feat: adicionar endpoint de controle PTZ (#123)
fix: corrigir timeout de conexão RTSP após 30s (#456)
docs: documentar endpoints de autenticação (#321)
chore: atualizar MediaMTX para v1.9 (#789)
refactor: extrair CameraHealthService do CameraService (#654)
test: adicionar testes de integração para RLS (#987)
```

---

## 4. Padrões Obrigatórios {#padroes-obrigatorios}

### 4.1 Frontend — React Query para Todo Fetch

```typescript
// ✅ CORRETO — usar React Query
function CameraList() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['cameras', { status: 'ONLINE' }],
    queryFn: () => api.getCameras({ status: 'ONLINE' }),
    staleTime: 30_000,  // revalida a cada 30s
  });

  if (isLoading) return <Skeleton />;
  if (error) return <ErrorMessage error={error} />;
  return <Grid cameras={data.data} />;
}

// ❌ PROIBIDO — nunca usar useEffect para fetch
function CameraList() {
  const [cameras, setCameras] = useState([]);
  useEffect(() => {
    fetch('/api/v1/cameras').then(r => r.json()).then(setCameras); // PROIBIDO
  }, []);
}
```

### 4.2 Frontend — Zustand Apenas para Estado de UI

```typescript
// ✅ CORRETO — Zustand para UI state
const useCameraStore = create<CameraUIState>((set) => ({
  selectedCameraId: null,
  gridLayout: '2x2',
  setSelectedCamera: (id) => set({ selectedCameraId: id }),
  setGridLayout: (layout) => set({ gridLayout: layout }),
}));

// ❌ ERRADO — nunca guardar dados do servidor no Zustand
const useCameraStore = create((set) => ({
  cameras: [],  // PROIBIDO — use React Query para dados do servidor
  fetchCameras: async () => {
    const data = await api.getCameras();
    set({ cameras: data });  // PROIBIDO
  },
}));
```

### 4.3 Frontend — TypeScript Explícito (sem `any`)

```typescript
// ✅ CORRETO
interface CameraStreamResponse {
  type: 'webrtc' | 'hls';
  url: string;
  expires_at: string;
  fallback_hls?: string;
}

async function getStreamUrl(cameraId: string): Promise<CameraStreamResponse> {
  const response = await fetch(`/api/v1/cameras/${cameraId}/stream/live`);
  return response.json() as Promise<CameraStreamResponse>;
}

// ❌ PROIBIDO
async function getStreamUrl(cameraId: any): Promise<any> {
  const response: any = await fetch(`/api/v1/cameras/${cameraId}/stream/live`);
  return response.json();
}
```

### 4.4 Backend — Sempre Usar DTOs (nunca expor entidades JPA)

```java
// ✅ CORRETO — DTO de response
@GetMapping("/{id}")
public ResponseEntity<CameraResponse> getCamera(@PathVariable UUID id) {
    Camera camera = cameraService.findById(id);
    return ResponseEntity.ok(CameraResponse.from(camera));  // converte para DTO
}

// ❌ PROIBIDO — retornar entidade JPA diretamente
@GetMapping("/{id}")
public ResponseEntity<Camera> getCamera(@PathVariable UUID id) {
    return ResponseEntity.ok(cameraRepository.findById(id).get());  // vaza: rtsp_url, totp_secret, etc.
}
```

### 4.5 Backend — GlobalExceptionHandler para Todos os Erros

```java
// GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CameraNotFoundException.class)
    public ResponseEntity<ApiError> handleCameraNotFound(CameraNotFoundException ex) {
        return ResponseEntity.status(404).body(
            new ApiError("CAMERA_NOT_FOUND", ex.getMessage(), 404)
        );
    }

    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<ApiError> handlePlanLimit(PlanLimitExceededException ex) {
        return ResponseEntity.status(422).body(
            new ApiError("PLAN_LIMIT_EXCEEDED", ex.getMessage(), 422)
        );
    }

    // Nunca deixar stack trace vazar para o cliente
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex) {
        log.error("Erro não tratado", ex);
        return ResponseEntity.status(500).body(
            new ApiError("INTERNAL_ERROR", "Erro interno do servidor", 500)
        );
    }
}
```

### 4.6 Backend — `@Transactional` apenas em Service

```java
// ✅ CORRETO — transação na camada de serviço
@Service
public class CameraService {
    @Transactional
    public Camera createCamera(CreateCameraRequest request, UUID tenantId) {
        // múltiplas operações em uma transação
    }
}

// ❌ PROIBIDO — transação em controller
@RestController
public class CameraController {
    @Transactional  // PROIBIDO aqui
    @PostMapping
    public ResponseEntity<CameraResponse> createCamera(...) { ... }
}
```

---

## 5. Linter e Formatter {#linter}

### 5.1 Frontend

```bash
# ESLint + Prettier configurados via next/eslint
npm run lint        # verificar
npm run lint:fix    # corrigir automaticamente

# Configuração (.eslintrc.json)
{
  "extends": ["next/core-web-vitals", "next/typescript"],
  "rules": {
    "@typescript-eslint/no-explicit-any": "error",
    "react-hooks/rules-of-hooks": "error",
    "react-hooks/exhaustive-deps": "warn"
  }
}
```

### 5.2 Backend

```bash
# Checkstyle via Maven
mvn checkstyle:check

# Spotless para formatação (Google Java Style)
mvn spotless:apply    # formatar
mvn spotless:check    # apenas verificar (usado no CI)
```

### 5.3 Pre-commit Hook

```bash
# .husky/pre-commit
#!/bin/sh
cd frontend && npm run lint && npm run type-check
```

---

## 6. Padrão de Commits {#commits}

Seguimos **Conventional Commits** (https://www.conventionalcommits.org):

```
<type>[optional scope]: <description>

[optional body]

[optional footer]
```

**Tipos aceitos:**
- `feat`: nova funcionalidade
- `fix`: correção de bug
- `docs`: apenas documentação
- `chore`: manutenção, atualização de dependências
- `refactor`: refatoração sem mudança de comportamento
- `test`: adição ou correção de testes
- `perf`: melhoria de performance
- `ci`: mudanças no pipeline CI/CD

---

## 7. Referências Cruzadas

- Tecnologias usadas: [TECH_STACK.md](./TECH_STACK.md)
- Variáveis de ambiente: [ENV_CONFIG.md](./ENV_CONFIG.md)
- Arquitetura de componentes: [ARCHITECTURE.md](./ARCHITECTURE.md)
