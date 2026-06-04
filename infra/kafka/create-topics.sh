#!/bin/bash
# @path infra/kafka/create-topics.sh
# @owner infra
# @responsibility Cria todos os tópicos Kafka/Redpanda no ambiente de desenvolvimento
# @see infra/kafka/topics.yml

set -e

BROKER="${KAFKA_BROKER:-redpanda:9092}"

echo "Aguardando Redpanda ficar disponível em ${BROKER}..."
until rpk cluster info --brokers "${BROKER}" > /dev/null 2>&1; do
    echo "  Redpanda ainda não disponível, aguardando 2s..."
    sleep 2
done

echo "Redpanda disponível. Criando tópicos..."

create_topic() {
    local topic=$1
    local partitions=$2
    local replication=$3

    echo "  Criando tópico: ${topic} (partitions=${partitions}, replication=${replication})"
    rpk topic create "${topic}" \
        --brokers "${BROKER}" \
        --partitions "${partitions}" \
        --replicas "${replication}" \
        --if-not-exists \
        --config retention.ms=604800000 \
        2>&1 || echo "  Tópico ${topic} já existe — ignorando"
}

# Tópicos conforme docs/ARCHITECTURE.md#comunicacao
create_topic "auth.events"       3 1
create_topic "camera.events"     6 1
create_topic "health.events"     6 1
create_topic "alert.events"      3 1
create_topic "recording.events"  6 1
create_topic "tenant.events"     3 1
create_topic "audit.events"      6 1

echo ""
echo "Tópicos criados com sucesso!"
rpk topic list --brokers "${BROKER}"
