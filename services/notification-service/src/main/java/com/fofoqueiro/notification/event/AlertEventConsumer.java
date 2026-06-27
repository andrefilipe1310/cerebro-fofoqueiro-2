package com.fofoqueiro.notification.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fofoqueiro.notification.service.EmailService;
import com.fofoqueiro.notification.service.ThrottleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEventConsumer {

    private final EmailService emailService;
    private final ThrottleService throttleService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "alert.events", groupId = "notification-service-alert")
    public void consume(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.path("eventType").asText();
            if (!"CAMERA_OFFLINE".equals(eventType)) return;

            JsonNode orgIdNode = node.has("orgId") ? node.get("orgId") : node.get("tenantId");
            String orgId = orgIdNode != null ? orgIdNode.asText() : "";
            String cameraId = node.path("cameraId").asText();
            String cameraName = node.path("cameraName").asText("Camera " + cameraId.substring(0, 8));
            String orgName = node.has("orgName") ? node.get("orgName").asText("Your Organization") : node.path("tenantName").asText("Your Organization");
            String recipientEmail = node.path("recipientEmail").asText();

            if (recipientEmail.isBlank()) return;
            if (throttleService.isThrottled(orgId, "camera_offline", cameraId)) {
                log.debug("Throttled camera_offline notification for camera {}", cameraId);
                return;
            }

            emailService.sendCameraOffline(recipientEmail, cameraName, orgName);
        } catch (Exception e) {
            log.error("Error processing alert event: {}", e.getMessage(), e);
        }
    }
}
