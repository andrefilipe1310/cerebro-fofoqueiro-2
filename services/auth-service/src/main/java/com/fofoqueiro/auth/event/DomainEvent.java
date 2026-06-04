// @path services/auth-service/src/main/java/com/fofoqueiro/auth/event/DomainEvent.java
// @owner auth-service
// @responsibility Envelope padrão para todos os eventos Kafka publicados por este serviço
// @see docs/SDD.md#eventos-kafka
package com.fofoqueiro.auth.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
public record DomainEvent(
        @JsonProperty("event_id")     String eventId,
        @JsonProperty("event_type")   String eventType,
        @JsonProperty("version")      String version,
        @JsonProperty("occurred_at")  Instant occurredAt,
        @JsonProperty("producer")     String producer,
        @JsonProperty("tenant_id")    UUID tenantId,
        @JsonProperty("correlation_id") String correlationId,
        @JsonProperty("payload")      Map<String, Object> payload
) {
    public static DomainEvent of(String eventType, UUID tenantId, Map<String, Object> payload) {
        return DomainEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .version("1.0")
                .occurredAt(Instant.now())
                .producer("auth-service")
                .tenantId(tenantId)
                .payload(payload)
                .build();
    }
}
