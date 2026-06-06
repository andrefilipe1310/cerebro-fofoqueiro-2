package com.fofoqueiro.audit.event;

import com.fofoqueiro.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditLogService auditLogService;

    @KafkaListener(
        topics = {"auth.events", "camera.events", "health.events", "alert.events", "tenant.events"},
        groupId = "audit-service"
    )
    public void consume(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition) {
        String eventId = topic + "-" + partition + "-" + offset;
        try {
            auditLogService.record(eventId, topic, message);
        } catch (Exception e) {
            log.error("Error recording audit log for topic {} offset {}: {}", topic, offset, e.getMessage(), e);
        }
    }
}
