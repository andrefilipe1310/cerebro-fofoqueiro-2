<!-- Meta
Versão: v0.1.0
Última atualização: 2026-06-04
Documentos relacionados:
  - [Data Model](./DATA_MODEL.md)
  - [API Contracts](./API_CONTRACTS.md)
  - [Multi-Tenancy](./MULTI_TENANCY.md)
  - [ENV Config](./ENV_CONFIG.md)
-->

# Segurança e LGPD {#security-lgpd}

## 1. Modelo de Autenticação {#autenticacao}

### 1.1 JWT — Tokens de Acesso e Refresh

| Token | Duração | Armazenamento no cliente | Uso |
|---|---|---|---|
| Access Token | 15 minutos | HttpOnly Cookie (SameSite=Strict) | Autenticação de APIs |
| Refresh Token | 7 dias | HttpOnly Cookie (SameSite=Strict) | Renovação do access token |

> **Por que HttpOnly Cookie e não localStorage?** localStorage é acessível via JavaScript, tornando o token vulnerável a ataques XSS. Cookies HttpOnly não podem ser lidos por JS — o browser os envia automaticamente nas requests, mas nenhum script pode acessar o valor diretamente.

**Estrutura do JWT (claims):**

```json
{
  "sub": "uuid-do-usuario",
  "tenant_id": "uuid-do-tenant",
  "role": "OPERATOR",
  "email": "operador@empresa.com",
  "iat": 1748995200,
  "exp": 1748996100,
  "jti": "uuid-unico-do-token"
}
```

**Invalidação de tokens:**
- Access tokens não são invalidáveis individualmente (por design — TTL curto de 15min minimiza o risco)
- Refresh tokens são armazenados no Redis com TTL de 7 dias
- Logout deleta o refresh token do Redis, impedindo renovação

### 1.2 2FA — TOTP (Time-Based One-Time Password)

| Role | 2FA Obrigatório |
|---|---|
| ADMIN | ✅ Sim — bloqueado até configurar |
| OPERATOR | ✅ Sim — bloqueado até configurar |
| VIEWER | ⚠️ Opcional — recomendado mas não obrigatório |

**Fluxo de setup:**

```
1. Usuário faz login com email/senha → recebe access_token temporário
2. Frontend detecta totp_enabled = false + role = ADMIN/OPERATOR
3. Frontend redireciona para /setup-2fa
4. POST /api/v1/auth/2fa/setup → retorna QR code + secret + 8 backup codes
5. Usuário escaneia QR com Google Authenticator / Authy
6. Usuário confirma com código de teste: POST /api/v1/auth/2fa/confirm {code: "123456"}
7. Backend seta totp_enabled = true, armazena totp_secret (AES-256 no banco)
8. Usuário recebe access_token completo e pode acessar o sistema
```

**Códigos de backup:**
- 8 códigos de uso único armazenados hasheados (bcrypt) no banco
- Cada código é invalidado após uso
- Usuário pode regenerar os códigos (invalida os anteriores)

---

## 2. Autorização — Roles e Permissões {#autorizacao}

### 2.1 Roles

| Role | Descrição | Quem tem |
|---|---|---|
| `ADMIN` | Gerencia o tenant: câmeras, usuários, configurações | Owner do tenant (1 por tenant) + usuários com cargo de gerente |
| `OPERATOR` | Monitora câmeras em tempo real, pode controlar PTZ e fazer download | Funcionários de central de monitoramento |
| `VIEWER` | Apenas visualização ao vivo. Sem download, sem configuração | Clientes finais (síndico, gestor) |

### 2.2 Tabela de Permissões por Recurso

| Ação | ADMIN | OPERATOR | VIEWER |
|---|---|---|---|
| Visualizar câmeras ao vivo | ✅ | ✅ | ✅ |
| Visualizar mosaico | ✅ | ✅ | ✅ |
| Abrir câmera em fullscreen | ✅ | ✅ | ✅ |
| Controle PTZ | ✅ | ✅ | ❌ |
| Visualizar timeline de gravações | ✅ | ✅ | ✅ |
| Download de gravações | ✅ | ✅ | ❌ |
| Ver alertas | ✅ | ✅ | ❌ |
| Reconhecer alertas | ✅ | ✅ | ❌ |
| Cadastrar câmeras | ✅ | ❌ | ❌ |
| Editar câmeras | ✅ | ❌ | ❌ |
| Excluir câmeras | ✅ | ❌ | ❌ |
| Gerenciar usuários | ✅ | ❌ | ❌ |
| Configurar white-label | ✅ | ❌ | ❌ |
| Configurar zonas de privacidade | ✅ | ❌ | ❌ |
| Ver logs de auditoria | ✅ | ❌ | ❌ |
| Exportar logs de auditoria | ✅ | ❌ | ❌ |

**Implementação Spring Security:**

```java
// SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/auth/**").permitAll()
        .requestMatchers(HttpMethod.POST, "/api/v1/cameras").hasRole("ADMIN")
        .requestMatchers(HttpMethod.DELETE, "/api/v1/cameras/**").hasRole("ADMIN")
        .requestMatchers("/api/v1/cameras/*/ptz").hasAnyRole("ADMIN", "OPERATOR")
        .requestMatchers("/api/v1/cameras/*/recordings/*/download")
            .hasAnyRole("ADMIN", "OPERATOR")
        .requestMatchers("/api/v1/audit-logs/**").hasRole("ADMIN")
        .anyRequest().authenticated()
    );
    return http.build();
}
```

---

## 3. Zonas de Mascaramento de Privacidade {#zonas-mascaramento}

### 3.1 Como Funciona

As zonas de mascaramento são polígonos definidos pelo administrador sobre o frame da câmera. O mascaramento é aplicado **client-side** via Canvas HTML5 sobreposto ao player de vídeo.

```typescript
// PrivacyZoneOverlay.tsx — componente React
function PrivacyZoneOverlay({ zones, videoRef }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    const video = videoRef.current;

    // Sincronia com o frame de vídeo
    const drawZones = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      zones.filter(z => z.active).forEach(zone => {
        ctx.fillStyle = 'black';
        ctx.beginPath();

        // Coordenadas em % do frame → pixels absolutos
        const points = zone.coordinates.map(([x, y]) => [
          (x / 100) * canvas.width,
          (y / 100) * canvas.height,
        ]);

        ctx.moveTo(points[0][0], points[0][1]);
        points.slice(1).forEach(([x, y]) => ctx.lineTo(x, y));
        ctx.closePath();
        ctx.fill();
      });

      requestAnimationFrame(drawZones);
    };

    drawZones();
  }, [zones]);

  return (
    <canvas
      ref={canvasRef}
      style={{ position: 'absolute', top: 0, left: 0, pointerEvents: 'none' }}
    />
  );
}
```

**Importante:** O mascaramento client-side é suficiente para a maioria dos casos (impede visualização por operadores). Para compliance LGPD mais rigoroso (ex: gravações nunca devem conter a área privativa), o mascaramento deve ser aplicado **server-side pelo FFmpeg** antes da gravação. Implementação server-side é recomendada para produção:

```bash
# FFmpeg com filtro de blur/black na área da zona
ffmpeg -i rtsp://... \
  -vf "drawbox=x=200:y=100:w=300:h=200:color=black:t=fill" \
  -c:v libx264 output.ts
```

### 3.2 Editor de Zonas

O editor permite ao ADMIN desenhar polígonos sobre um frame congelado da câmera:

1. Backend captura um frame: `GET /api/v1/cameras/{id}/snapshot`
2. Frontend exibe o frame como background de um canvas interativo
3. Admin clica para adicionar pontos do polígono
4. Ao fechar o polígono, POST para `POST /api/v1/cameras/{id}/privacy-zones`
5. Zona salva em [privacy_zones](./DATA_MODEL.md#privacy_zones) com `coordinates` em % do frame

---

## 4. Criptografia {#criptografia}

### 4.1 Em Trânsito (TLS)

- Todo o tráfego HTTP, WebSocket e HLS/WebRTC é obrigatoriamente HTTPS/WSS
- Certificados gerenciados por Cloudflare (proxy mode) + Let's Encrypt para domínios customizados
- Versão mínima de TLS: 1.2 (recomendado 1.3)
- HSTS habilitado com `max-age=31536000; includeSubDomains`

### 4.2 Em Repouso — Dados Sensíveis no Banco

Os campos `rtsp_url` e `totp_secret` são criptografados com AES-256 antes de serem persistidos:

```java
// EncryptedStringConverter.java — JPA AttributeConverter
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Value("${ENCRYPTION_KEY}")
    private String encryptionKey;  // chave de 256 bits em Base64

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) return null;
        return AesGcmEncryption.encrypt(plaintext, encryptionKey);
    }

    @Override
    public String convertToEntityAttribute(String ciphertext) {
        if (ciphertext == null) return null;
        return AesGcmEncryption.decrypt(ciphertext, encryptionKey);
    }
}

// Uso na entidade:
@Entity
public class Camera {
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "rtsp_url_encrypted")
    private String rtspUrl;
}
```

Ver variável `ENCRYPTION_KEY` em [ENV_CONFIG.md](./ENV_CONFIG.md).

### 4.3 Senhas de Usuários

- Hash com **bcrypt** (strength 12)
- Nunca armazenadas em texto claro, nunca logadas

---

## 5. Conformidade LGPD {#lgpd}

### 5.1 Base Legal

O monitoramento por câmeras IP em condomínios e empresas é coberto por:

- **Art. 7º, VI da LGPD:** Legítimo interesse do controlador — segurança patrimonial é legítimo interesse documentado
- **Art. 7º, IX da LGPD:** Proteção da vida e incolumidade física — aplicável a câmeras externas de segurança

**Documentação obrigatória (responsabilidade do tenant/revendedor):**
- [ ] Registro das Atividades de Tratamento (RAT) com finalidade e base legal documentadas
- [ ] Aviso de monitoramento visível nos locais monitorados
- [ ] Política de Privacidade atualizada

### 5.2 Aviso de Monitoramento

A plataforma disponibiliza ao tenant:
- Template de aviso de monitoramento para impressão
- Módulo de consentimento digital (futuro, fora do MVP)

### 5.3 Direitos do Titular

| Direito (LGPD Art. 18) | Como implementar na plataforma |
|---|---|
| Acesso aos dados | Exportação de relatório de imagens em que o titular aparece — [DEFINIR: fluxo de solicitação] |
| Correção | N/A para imagens de câmera (dados imutáveis por natureza) |
| Exclusão ("direito ao esquecimento") | `DELETE /api/v1/admin/recordings/by-date-range` — ADMIN pode deletar gravações de um período específico, removendo do R2 e do banco |
| Portabilidade | Exportação de gravações em formato padrão (MP4/TS) |
| Revogação do consentimento | Desativar câmera do local onde o titular está presente |

**Implementação de deleção de gravações:**

```java
// RecordingService.java
@Transactional
public void deleteRecordingsInRange(UUID cameraId, Instant from, Instant to, UUID requestedBy) {
    List<Recording> recordings = recordingRepository.findByCameraAndTimeRange(cameraId, from, to);

    for (Recording rec : recordings) {
        // 1. Deletar do R2
        r2Client.deleteObject(rec.getR2Key());

        // 2. Deletar metadados do banco
        recordingRepository.delete(rec);

        // 3. Registrar deleção no log de auditoria
        auditService.log(AuditAction.DELETE_RECORDING, rec.getId(), requestedBy);
    }
}
```

### 5.4 Política de Retenção por Plano

| Plano | Retenção máxima | Deleção automática |
|---|---|---|
| FREE | 7 dias | Job diário deleta gravações > 7 dias |
| STARTER | 30 dias | Job diário deleta gravações > 30 dias |
| PRO | 90 dias | Job diário deleta gravações > 90 dias |
| ENTERPRISE | 365 dias | Job diário deleta gravações > 365 dias |

```java
// RetentionCleanupJob.java
@Scheduled(cron = "0 2 * * *")  // 2h da manhã todos os dias
public void cleanupExpiredRecordings() {
    List<Tenant> tenants = tenantRepository.findAllActive();
    for (Tenant tenant : tenants) {
        int retentionDays = PlanLimits.retentionDays(tenant.getPlan());
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        recordingService.deleteExpiredRecordings(tenant.getId(), cutoff);
    }
}
```

### 5.5 Aviso de Monitoramento Obrigatório

Por lei, locais monitorados por câmeras devem ter aviso visível. A plataforma:
- Fornece template editável de "ÁREA MONITORADA POR CÂMERAS" para o tenant imprimir
- [DEFINIR: módulo futuro de aviso digital exibido na entrada — fora do MVP]

---

## 6. Imutabilidade dos Logs de Auditoria {#imutabilidade-audit}

Os logs de auditoria devem ser imutáveis para garantir validade probatória.

```sql
-- Trigger que impede UPDATE e DELETE em audit_logs
-- Ver DATA_MODEL.md#audit_logs para a definição completa

CREATE OR REPLACE FUNCTION prevent_audit_log_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION
        'Operação proibida: audit_logs são imutáveis. Tentativa de % por %',
        TG_OP, current_user;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER audit_logs_immutable
BEFORE UPDATE OR DELETE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION prevent_audit_log_modification();

-- Verificação: mesmo o DBA não pode deletar sem desabilitar o trigger
-- Em produção, o acesso direto ao banco é restrito e auditado pelo provedor cloud
```

**O que é auditado automaticamente (via AOP interceptor):**

| Ação | Registrada automaticamente |
|---|---|
| LOGIN | ✅ (sucesso e falha) |
| LOGOUT | ✅ |
| VIEW_CAMERA | ✅ (cada abertura de stream) |
| DOWNLOAD_RECORDING | ✅ |
| PTZ_COMMAND | ✅ |
| CREATE_CAMERA | ✅ |
| UPDATE_CAMERA | ✅ |
| DELETE_CAMERA | ✅ |
| CREATE_USER | ✅ |
| UPDATE_USER | ✅ |
| DEACTIVATE_USER | ✅ |
| UPDATE_BRANDING | ✅ |
| CREATE_PRIVACY_ZONE | ✅ |
| DELETE_RECORDING | ✅ (com motivo LGPD se informado) |

---

## 7. Rate Limiting e Proteção Contra Ataques {#rate-limiting}

### 7.1 Rate Limits por Endpoint

| Endpoint | Limite | Janela |
|---|---|---|
| POST /auth/login | 5 tentativas | 15 minutos por IP |
| POST /auth/2fa/verify | 3 tentativas | 5 minutos por usuário |
| POST /auth/refresh | 10 requests | 1 minuto por usuário |
| GET /cameras (listagem) | 100 requests | 1 minuto por usuário |
| POST /cameras/*/ptz | 60 comandos | 1 minuto por câmera |

### 7.2 Headers de Segurança (via Nginx)

```nginx
add_header X-Frame-Options DENY;
add_header X-Content-Type-Options nosniff;
add_header X-XSS-Protection "1; mode=block";
add_header Referrer-Policy "strict-origin-when-cross-origin";
add_header Content-Security-Policy "default-src 'self'; ...";
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload";
```

---

## 8. Referências Cruzadas

- Entidade audit_logs: [DATA_MODEL.md#audit_logs](./DATA_MODEL.md#audit_logs)
- Entidade privacy_zones: [DATA_MODEL.md#privacy_zones](./DATA_MODEL.md#privacy_zones)
- Endpoints de autenticação: [API_CONTRACTS.md#auth-endpoints](./API_CONTRACTS.md#auth-endpoints)
- Variáveis de segurança: [ENV_CONFIG.md](./ENV_CONFIG.md)
- Isolamento de dados por tenant: [MULTI_TENANCY.md](./MULTI_TENANCY.md)
