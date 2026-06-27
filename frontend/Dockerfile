# @path frontend/Dockerfile
# @owner frontend
# @responsibility Multi-stage build do frontend Next.js 14 — standalone output para container mínimo
# @see docs/TECH_STACK.md#frontend

# Stage 1: deps — instala dependências
FROM node:20-alpine AS deps
WORKDIR /app
# Copie o lockfile do pnpm no lugar do lock do npm
COPY package.json pnpm-lock.yaml ./

# Instale o pnpm globalmente no contêiner e rode a instalação limpa
RUN npm install -g pnpm && \
    echo "shamefully-hoist=true" > .npmrc && \
    pnpm install --frozen-lockfile

# Stage 2: builder — build de produção
FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED 1
ENV NEXT_BUILD_STANDALONE true
RUN npm run build

# Stage 3: runner — imagem final mínima com standalone output
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV production
ENV NEXT_TELEMETRY_DISABLED 1

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY --from=builder /app/public ./public
COPY --from=builder --chown=appuser:appgroup /app/.next/standalone ./
COPY --from=builder --chown=appuser:appgroup /app/.next/static ./.next/static

USER appuser
EXPOSE 3000
ENV PORT 3000
ENV HOSTNAME "0.0.0.0"

CMD ["node", "server.js"]
