package com.fofoqueiro.recording.event;

import com.fofoqueiro.recording.domain.entity.OutboxEvent;
import com.fofoqueiro.recording.repository.OutboxEventRepository;
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
    private final OutboxEventRepository repo;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publish() {
        List<OutboxEvent> events = repo.findUnsentEvents();
        for (OutboxEvent e : events) {
            try {
                kafkaTemplate.send(e.getTopic(), e.getPayload());
                e.setSentAt(OffsetDateTime.now());
            } catch (Exception ex) {
                e.setAttempts(e.getAttempts() + 1);
                e.setLastError(ex.getMessage());
            }
            repo.save(e);
        }
    }
}
