# @path Makefile
# @owner infra
# @responsibility Comandos de desenvolvimento — setup, build, test e operações do dia a dia
# @see docs/README.md#quickstart

.PHONY: dev dev-infra dev-services dev-frontend migrate seed test build \
        logs logs-svc db redis-cli kafka-topics kafka-consume clean help

# ─── DESENVOLVIMENTO ─────────────────────────────────────────────────────────

dev: ## Sobe toda a stack (infra + todos os serviços + frontend)
	docker compose up --build

dev-infra: ## Sobe apenas infra (postgres, redis, redpanda, mediamtx, kong, nginx)
	docker compose up postgres redis redpanda kafka-init mediamtx kong nginx

dev-services: ## Sobe apenas os 8 microserviços Spring Boot (requer dev-infra rodando)
	docker compose up auth-service tenant-service camera-service chms-service \
	  recording-service alert-service notification-service audit-service

dev-frontend: ## Sobe apenas o frontend Next.js
	docker compose up frontend

# ─── BANCO DE DADOS ──────────────────────────────────────────────────────────

migrate: ## Roda migrations Flyway em todos os serviços com banco
	@echo "=== Migrando todos os serviços ==="
	@for svc in auth tenant camera chms recording alert audit; do \
	  echo ">>> $$svc-service..."; \
	  docker compose run --rm $$svc-service \
	    java -Dspring.profiles.active=dev -jar app.jar \
	    --spring.flyway.validate-on-migrate=true 2>&1 | tail -5 || true; \
	done
	@echo "Migrations concluídas."

seed: ## Insere dados de desenvolvimento
	docker compose exec postgres \
	  psql -U postgres -d platform -f /docker-entrypoint-initdb.d/99_dev_seed.sql

db: ## Abre psql interativo no container postgres
	docker compose exec postgres psql -U postgres -d platform

redis-cli: ## Abre redis-cli interativo
	docker compose exec redis redis-cli -a $${REDIS_PASSWORD:-changeme_redis}

# ─── KAFKA / REDPANDA ────────────────────────────────────────────────────────

kafka-topics: ## Lista tópicos Kafka/Redpanda
	docker compose exec redpanda rpk topic list

kafka-consume: ## Consome mensagens de um tópico: make kafka-consume TOPIC=health.events
	docker compose exec redpanda rpk topic consume $(TOPIC) --offset start

# ─── TESTES ──────────────────────────────────────────────────────────────────

test: ## Roda testes de todos os serviços
	@echo "=== Testando todos os serviços ==="
	@for svc in auth tenant camera chms recording alert notification audit; do \
	  echo ">>> $$svc-service..."; \
	  cd services/$$svc-service && mvn test -B --no-transfer-progress 2>&1 | tail -15; \
	  cd ../..; \
	done
	@if [ -d frontend ]; then \
	  echo ">>> frontend..."; \
	  cd frontend && npm test -- --watchAll=false 2>&1 | tail -10; \
	fi

test-svc: ## Testa um serviço específico: make test-svc SVC=auth
	cd services/$(SVC)-service && mvn test -B --no-transfer-progress

# ─── BUILD ───────────────────────────────────────────────────────────────────

build: ## Build de produção de todos os serviços
	docker compose build --no-cache

build-svc: ## Build de um serviço específico: make build-svc SVC=auth
	docker compose build --no-cache $(SVC)-service

# ─── LOGS ────────────────────────────────────────────────────────────────────

logs: ## Tail de todos os logs
	docker compose logs -f --tail=50

logs-svc: ## Tail de um serviço específico: make logs-svc SVC=auth-service
	docker compose logs -f --tail=100 $(SVC)

# ─── LIMPEZA ─────────────────────────────────────────────────────────────────

clean: ## Remove volumes e containers (DESTRÓI dados locais)
	@echo "ATENÇÃO: Este comando destrói todos os dados locais (PostgreSQL, Redis, Redpanda)."
	@echo "Pressione Ctrl+C para cancelar ou aguarde 5 segundos..."
	@sleep 5
	docker compose down -v --remove-orphans

# ─── HEALTH CHECK ────────────────────────────────────────────────────────────

health: ## Verifica health de todos os serviços
	@echo "=== Health Check ==="
	@for port in 8081 8082 8083 8084 8085 8086 8087 8088; do \
	  status=$$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$$port/actuator/health 2>/dev/null || echo "DOWN"); \
	  echo "  Port $$port: $$status"; \
	done
	@echo "  Kong: $$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8000 2>/dev/null || echo "DOWN")"
	@echo "  MediaMTX: $$(curl -s -o /dev/null -w "%{http_code}" http://localhost:9997/v3/paths/list 2>/dev/null || echo "DOWN")"
	@echo "  Frontend: $$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3000 2>/dev/null || echo "DOWN")"

# ─── AJUDA ───────────────────────────────────────────────────────────────────

help: ## Lista todos os targets disponíveis
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' Makefile | \
	  awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'
