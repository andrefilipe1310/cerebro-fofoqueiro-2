// @path services/notification-service/src/main/java/com/fofoqueiro/notification/event/DomainEvent.java
// @owner notification-service
// @responsibility Envelope padrão para eventos Kafka consumidos — não produz eventos
// @see docs/SDD.md#eventos-kafka
package com.fofoqueiro.notification.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
public record DomainEvent(
        @JsonProperty("event_id")      String eventId,
        @JsonProperty("event_type")    String eventType,
        @JsonProperty("version")       String version,
        @JsonProperty("occurred_at")   Instant occurredAt,
        @JsonProperty("producer")      String producer,
        @JsonProperty("tenant_id")     UUID tenantId,
        @JsonProperty("correlation_id") String correlationId,
        @JsonProperty("payload")       Map<String, Object> payload
) {}
