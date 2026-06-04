// @path services/camera-service/src/main/java/com/fofoqueiro/camera/event/DomainEvent.java
// @owner camera-service
// @responsibility Envelope padrão para eventos Kafka — camera.created, camera.deleted, camera.rtsp_updated
// @see docs/SDD.md#eventos-kafka
package com.fofoqueiro.camera.event;

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
) {
    public static DomainEvent of(String eventType, UUID tenantId, Map<String, Object> payload) {
        return DomainEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .version("1.0")
                .occurredAt(Instant.now())
                .producer("camera-service")
                .tenantId(tenantId)
                .payload(payload)
                .build();
    }
}
