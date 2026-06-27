// @path services/audit-service/src/main/java/com/fofoqueiro/audit/event/DomainEvent.java
// @owner audit-service
// @responsibility Envelope padrão para eventos Kafka consumidos — Audit Service NUNCA produz eventos
// @see docs/SDD.md#design-audit | docs/ARCHITECTURE.md#audit-service
package com.fofoqueiro.audit.event;

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
        @JsonProperty("org_id")        UUID orgId,
        @JsonProperty("correlation_id") String correlationId,
        @JsonProperty("payload")       Map<String, Object> payload
) {}
