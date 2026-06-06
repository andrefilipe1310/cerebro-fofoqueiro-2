package com.fofoqueiro.tenant.event;

import com.fofoqueiro.tenant.domain.entity.OutboxEvent;
import com.fofoqueiro.tenant.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxEventRepository.findUnsentEvents();
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getPayload());
                event.setSentAt(OffsetDateTime.now());
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Falha ao publicar evento {}: {}", event.getId(), e.getMessage());
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(e.getMessage());
                outboxEventRepository.save(event);
            }
        }
    }
}
