package com.fofoqueiro.alert.service;

import com.fofoqueiro.alert.domain.entity.Alert;
import com.fofoqueiro.alert.dto.response.AlertResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(Alert alert) {
        try {
            AlertResponse response = AlertResponse.from(alert);
            String destination = "/topic/tenant/" + alert.getTenantId() + "/alerts";
            messagingTemplate.convertAndSend(destination, response);
            log.debug("Alert broadcast to {}", destination);
        } catch (Exception e) {
            log.warn("Failed to broadcast alert {}: {}", alert.getId(), e.getMessage());
        }
    }
}
