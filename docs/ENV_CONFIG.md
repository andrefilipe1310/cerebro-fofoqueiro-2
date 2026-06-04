<!-- Meta
Versão: v0.1.0
Última atualização: 2026-06-04
Documentos relacionados:
  - [Tech Stack](./TECH_STACK.md)
  - [Arquitetura](./ARCHITECTURE.md)
  - [Security LGPD](./SECURITY_LGPD.md)
  - [Media Pipeline](./MEDIA_PIPELINE.md)
-->

# Configuração de Ambiente {#env-config}

## 1. Banco de Dados (PostgreSQL) {#db-config}

| Variável | Exemplo | Obrigatória | Descrição | Serviço |
|---|---|---|---|---|
| `DB_HOST` | `localhost` | ✅ | Host do PostgreSQL | Backend |
| `DB_PORT` | `5432` | ✅ | Porta do PostgreSQL | Backend |
| `DB_NAME` | `vigilancia_db` | ✅ | Nome do banco de dados | Backend |
| `DB_USER` | `app_user` | ✅ | Usuário da aplicação (com RLS ativo) | Backend |
| `DB_PASSWORD` | `senha_forte_aqui` | ✅ | Senha do usuário da aplicação | Backend |
| `DB_POOL_SIZE` | `20` | ⚠️ | Tamanho do pool de conexões | Backend |
| `DB_MIGRATIONS_USER` | `migrations_user` | ✅ | Usuário com BYPASSRLS para Flyway/Liquibase | Backend |
| `DB_MIGRATIONS_PASSWORD` | `outra_senha_forte` | ✅ | Senha do usuário de migrations | Backend |

---

## 2. Redis {#redis-config}

| Variável | Exemplo | Obrigatória | Descrição | Serviço |
|---|---|---|---|---|
| `REDIS_HOST` | `localhost` | ✅ | Host do Redis | Backend |
| `REDIS_PORT` | `6379` | ✅ | Porta do Redis | Backend |
| `REDIS_PASSWORD` | `redis_senha_forte` | ✅ | Senha do Redis (vazio em dev) | Backend |
| `REDIS_DB` | `0` | ⚠️ | Número do database Redis (0-15) | Backend |
| `REDIS_TTL_REFRESH_TOKEN_SECONDS` | `604800` | ✅ | TTL do refresh token no Redis (7 dias = 604800s) | Backend |

---

## 3. JWT e Segurança {#jwt-config}

| Variável | Exemplo | Obrigatória | Descrição | Serviço |
|---|---|---|---|---|
| `JWT_SECRET` | `base64_string_256bits_aqui` | ✅ | Chave HMAC-SHA256 para assinar JWTs (mínimo 256 bits) | Backend |
| `JWT_EXPIRATION_MS` | `900000` | ✅ | Duração do access token em ms (900000 = 15 min) | Backend |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` | ✅ | Duração do refresh token em ms (604800000 = 7 dias) | Backend |
| `ENCRYPTION_KEY` | `base64_string_256bits_aqui` | ✅ | Chave AES-256 para criptografar rtsp_url e totp_secret no banco | Backend |
| `TOTP_ISSUER` | `[DEFINIR: nome do produto]` | ✅ | Nome exibido no Google Authenticator / Authy | Backend |

**Gerar chaves seguras:**
```bash
# JWT_SECRET e ENCRYPTION_KEY (256 bits em Base64)
openssl rand -base64 32

# NUNCA reutilizar a mesma chave para JWT e Encryption
# NUNCA commitar valores reais — usar secrets manager em produção
```

---

## 4. Cloudflare R2 {#r2-config}

| Variável | Exemplo | Obrigatória | Descrição | Serviço |
|---|---|---|---|---|
| `R2_ACCOUNT_ID` | `abc123def456` | ✅ | Account ID da Cloudflare | Backend |
| `R2_ACCESS_KEY` | `AKIAIOSFODNN7EXAMPLE` | ✅ | Access Key ID (S3-compatible) | Backend |
| `R2_SECRET_KEY` | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` | ✅ | Secret Access Key | Backend |
| `R2_BUCKET_NAME` | `vigilancia-recordings` | ✅ | Nome do bucket R2 | Backend |
| `R2_PUBLIC_URL` | `https://pub-abc.r2.dev` | ✅ | URL pública para acesso a thumbnails | Backend |
| `R2_ENDPOINT` | `https://abc123.r2.cloudflarestorage.com` | ✅ | Endpoint S3-compatible do R2 | Backend |

**Configuração de desenvolvimento (MinIO local):**
```bash
R2_ACCOUNT_ID=local
R2_ACCESS_KEY=minioadmin
R2_SECRET_KEY=minioadmin
R2_BUCKET_NAME=vigilancia-dev
R2_PUBLIC_URL=http://localhost:9000/vigilancia-dev
R2_ENDPOINT=http://localhost:9000
```

---

## 5. MediaMTX {#mediamtx-config}

| Variável | Exemplo | Obrigatória | Descrição | Serviço |
|---|---|---|---|---|
| `MEDIAMTX_API_URL` | `http://mediamtx:9997` | ✅ | URL da API REST de administração do MediaMTX | Backend |
| `MEDIAMTX_AUTH_TOKEN` | `token_secreto_aqui` | ✅ | Token para autenticar chamadas à API do MediaMTX | Backend |
| `MEDIAMTX_PUBLIC_URL` | `https://media.dominio.com` | ✅ | URL pública do MediaMTX (usada para gerar URLs de stream) | Backend + Frontend |
| `MEDIAMTX_RTMP_PORT` | `1935` | ⚠️ | Porta RTMP (se cameras publicarem via RTMP) | MediaMTX |
| `MEDIAMTX_RTSP_PORT` | `8554` | ✅ | Porta RTSP | MediaMTX |
| `MEDIAMTX_HLS_PORT` | `8888` | ✅ | Porta HLS | MediaMTX |
| `MEDIAMTX_WEBRTC_PORT` | `8889` | ✅ | Porta WebRTC (WHEP/WHIP) | MediaMTX |

---

## 6. Aplicação {#app-config}

| Variável | Exemplo | Obrigatória | Descrição | Serviço |
|---|---|---|---|---|
| `APP_URL` | `https://app.dominio.com` | ✅ | URL pública da aplicação frontend | Backend + Frontend |
| `APP_ENV` | `development` | ✅ | Ambiente: `development`, `staging`, `production` | Todos |
| `APP_PORT` | `8080` | ⚠️ | Porta do backend Spring Boot | Backend |
| `CORS_ALLOWED_ORIGINS` | `https://app.dominio.com,https://monitor.cliente.com` | ✅ | Origins permitidas para CORS | Backend |
| `LOG_LEVEL` | `INFO` | ⚠️ | Nível de log: `DEBUG`, `INFO`, `WARN`, `ERROR` | Backend |
| `INTERNAL_API_SECRET` | `segredo_para_mediamtx_webhook` | ✅ | Secret para autenticar webhook do MediaMTX no backend | Backend |

---

## 7. E-mail {#email-config}

| Variável | Exemplo | Obrigatória | Descrição | Serviço |
|---|---|---|---|---|
| `SMTP_HOST` | `smtp.sendgrid.net` | ✅ | Host do servidor SMTP | Backend |
| `SMTP_PORT` | `587` | ✅ | Porta SMTP (587 para TLS, 465 para SSL) | Backend |
| `SMTP_USER` | `apikey` | ✅ | Usuário SMTP (para SendGrid: literal `apikey`) | Backend |
| `SMTP_PASSWORD` | `SG.xxxxx` | ✅ | Senha SMTP (SendGrid API Key) | Backend |
| `EMAIL_FROM` | `noreply@dominio.com` | ✅ | Endereço de remetente dos e-mails | Backend |
| `EMAIL_FROM_NAME` | `[DEFINIR: nome do produto]` | ⚠️ | Nome exibido no campo "De" dos e-mails | Backend |

---

## 8. Frontend (Next.js — variáveis públicas com prefixo NEXT_PUBLIC_) {#frontend-config}

| Variável | Exemplo | Obrigatória | Descrição |
|---|---|---|---|
| `NEXT_PUBLIC_API_URL` | `https://api.dominio.com` | ✅ | URL base da API backend |
| `NEXT_PUBLIC_MEDIAMTX_URL` | `https://media.dominio.com` | ✅ | URL pública do MediaMTX |
| `NEXT_PUBLIC_APP_ENV` | `production` | ✅ | Ambiente atual |
| `NEXT_PUBLIC_MAPBOX_TOKEN` | — | ❌ | Não usado — usamos OpenStreetMap gratuito |

**Atenção:** Variáveis `NEXT_PUBLIC_*` são expostas no bundle do cliente. **Nunca colocar secrets aqui.**

---

## 9. Arquivo .env.example Completo {#env-example}

```bash
# .env.example — copie para .env e preencha os valores reais
# NUNCA commitar o .env com valores reais no repositório

# ─── Banco de Dados (PostgreSQL) ───────────────────────────────────────
DB_HOST=localhost
DB_PORT=5432
DB_NAME=vigilancia_db
DB_USER=app_user
DB_PASSWORD=CHANGE_ME
DB_POOL_SIZE=20
DB_MIGRATIONS_USER=migrations_user
DB_MIGRATIONS_PASSWORD=CHANGE_ME

# ─── Redis ──────────────────────────────────────────────────────────────
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=CHANGE_ME
REDIS_DB=0
REDIS_TTL_REFRESH_TOKEN_SECONDS=604800

# ─── JWT e Segurança ────────────────────────────────────────────────────
JWT_SECRET=CHANGE_ME_256_BIT_BASE64_STRING
JWT_EXPIRATION_MS=900000
JWT_REFRESH_EXPIRATION_MS=604800000
ENCRYPTION_KEY=CHANGE_ME_256_BIT_BASE64_STRING_DIFFERENT_FROM_JWT
TOTP_ISSUER=DEFINIR_NOME_DO_PRODUTO

# ─── Cloudflare R2 ──────────────────────────────────────────────────────
R2_ACCOUNT_ID=CHANGE_ME
R2_ACCESS_KEY=CHANGE_ME
R2_SECRET_KEY=CHANGE_ME
R2_BUCKET_NAME=vigilancia-recordings
R2_PUBLIC_URL=https://pub-CHANGE_ME.r2.dev
R2_ENDPOINT=https://CHANGE_ME.r2.cloudflarestorage.com

# ─── MediaMTX ───────────────────────────────────────────────────────────
MEDIAMTX_API_URL=http://localhost:9997
MEDIAMTX_AUTH_TOKEN=CHANGE_ME
MEDIAMTX_PUBLIC_URL=https://media.seudominio.com
MEDIAMTX_RTSP_PORT=8554
MEDIAMTX_HLS_PORT=8888
MEDIAMTX_WEBRTC_PORT=8889

# ─── Aplicação ──────────────────────────────────────────────────────────
APP_URL=http://localhost:3000
APP_ENV=development
APP_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:3000
LOG_LEVEL=DEBUG
INTERNAL_API_SECRET=CHANGE_ME

# ─── E-mail ─────────────────────────────────────────────────────────────
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=seu@email.com
SMTP_PASSWORD=CHANGE_ME
EMAIL_FROM=noreply@seudominio.com
EMAIL_FROM_NAME=DEFINIR_NOME_DO_PRODUTO

# ─── Frontend (Next.js) ─────────────────────────────────────────────────
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_MEDIAMTX_URL=http://localhost:8889
NEXT_PUBLIC_APP_ENV=development
```

---

## 10. Configuração por Serviço de Terceiros {#terceiros}

### 10.1 Cloudflare R2

1. Acesse [Cloudflare Dashboard](https://dash.cloudflare.com) → R2 → "Create bucket"
2. Copie o Account ID do painel principal
3. Vá em "Manage R2 API tokens" → "Create API token"
4. Selecione permissões: `Object Read & Write` para o bucket específico
5. Copie `Access Key ID` → `R2_ACCESS_KEY` e `Secret Access Key` → `R2_SECRET_KEY`
6. Endpoint do bucket: `https://{account_id}.r2.cloudflarestorage.com`

### 10.2 MediaMTX

1. Baixe o binário ou use a imagem Docker: `bluenviron/mediamtx:latest`
2. Configure `mediamtx.yml` (ver [MEDIA_PIPELINE.md](./MEDIA_PIPELINE.md))
3. A API REST de admin fica em `http://host:9997/v3/`
4. Defina `MEDIAMTX_AUTH_TOKEN` e configure o webhook de auth apontando para o backend

### 10.3 SendGrid (E-mail)

1. Crie conta em [sendgrid.com](https://sendgrid.com)
2. Vá em Settings → API Keys → "Create API Key"
3. Permissão: "Mail Send" (somente envio)
4. Configure SMTP: `SMTP_HOST=smtp.sendgrid.net`, `SMTP_USER=apikey`, `SMTP_PASSWORD={api_key}`

### 10.4 Coturn (TURN Server para WebRTC)

Para redes corporativas com NAT restrito, um servidor TURN garante conectividade WebRTC:

```bash
# Instalar Coturn
apt-get install coturn

# /etc/turnserver.conf
listening-port=3478
tls-listening-port=5349
realm=turn.seudominio.com
server-name=turn.seudominio.com
lt-cred-mech
user=TURN_USERNAME:TURN_CREDENTIAL
cert=/etc/ssl/turn.crt
pkey=/etc/ssl/turn.key
```

Adicione ao mediamtx.yml:
```yaml
webrtcICEServers:
  - urls: [turn:turn.seudominio.com:3478]
    username: TURN_USERNAME
    credential: TURN_CREDENTIAL
```

---

## 11. Referências Cruzadas

- Tech stack que usa estas variáveis: [TECH_STACK.md](./TECH_STACK.md)
- Arquitetura dos serviços: [ARCHITECTURE.md](./ARCHITECTURE.md)
- Configuração detalhada do MediaMTX: [MEDIA_PIPELINE.md](./MEDIA_PIPELINE.md)
- Variáveis de segurança e chaves: [SECURITY_LGPD.md](./SECURITY_LGPD.md)
