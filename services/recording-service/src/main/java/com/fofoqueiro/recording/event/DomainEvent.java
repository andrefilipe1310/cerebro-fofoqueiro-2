// @path services/recording-service/src/main/java/com/fofoqueiro/recording/event/DomainEvent.java
// @owner recording-service
// @responsibility Envelope padrão para eventos Kafka — recording.segment_stored, recording.deleted
// @see docs/SDD.md#eventos-kafka
package com.fofoqueiro.recording.event;

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
) {
    public static DomainEvent of(String eventType, UUID orgId, Map<String, Object> payload) {
        return DomainEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .version("1.0")
                .occurredAt(Instant.now())
                .producer("recording-service")
                .orgId(orgId)
                .payload(payload)
                .build();
    }
}
