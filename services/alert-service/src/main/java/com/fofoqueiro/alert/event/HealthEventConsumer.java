package com.fofoqueiro.alert.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fofoqueiro.alert.domain.enums.AlertSeverity;
import com.fofoqueiro.alert.domain.enums.AlertType;
import com.fofoqueiro.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class HealthEventConsumer {

    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "health.events", groupId = "alert-service-health")
    public void consume(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.get("eventType").asText();
            UUID cameraId = UUID.fromString(node.get("cameraId").asText());
            JsonNode orgIdNode = node.has("orgId") ? node.get("orgId") : node.get("tenantId");
            UUID orgId = UUID.fromString(orgIdNode.asText());
            String kafkaEventId = topic + "-" + offset;

            AlertType alertType;
            AlertSeverity severity;
            String alertMessage;

            switch (eventType) {
                case "WENT_OFFLINE" -> {
                    alertType = AlertType.CAMERA_OFFLINE;
                    severity = AlertSeverity.CRITICAL;
                    alertMessage = "Camera " + cameraId + " went offline";
                }
                case "CAME_ONLINE" -> {
                    alertType = AlertType.CAMERA_ONLINE;
                    severity = AlertSeverity.INFO;
                    alertMessage = "Camera " + cameraId + " came back online";
                }
                default -> {
                    log.debug("Unhandled health event type: {}", eventType);
                    return;
                }
            }

            alertService.create(cameraId, orgId, alertType, alertMessage, severity, kafkaEventId);
        } catch (Exception e) {
            log.error("Error processing health event: {}", e.getMessage(), e);
        }
    }
}
