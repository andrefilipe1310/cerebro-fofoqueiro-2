package com.fofoqueiro.notification.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fofoqueiro.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "auth.events", groupId = "notification-service-auth")
    public void consume(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String eventType = node.path("eventType").asText();
            if (!"USER_CREATED".equals(eventType)) return;

            String email = node.path("email").asText();
            String name = node.path("name").asText(email);
            if (email.isBlank()) return;

            emailService.sendWelcome(email, name);
        } catch (Exception e) {
            log.error("Error processing auth event: {}", e.getMessage(), e);
        }
    }
}
