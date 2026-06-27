package com.fofoqueiro.alert.service;

import com.fofoqueiro.alert.domain.entity.Alert;
import com.fofoqueiro.alert.domain.enums.AlertSeverity;
import com.fofoqueiro.alert.domain.enums.AlertStatus;
import com.fofoqueiro.alert.domain.enums.AlertType;
import com.fofoqueiro.alert.dto.response.AlertResponse;
import com.fofoqueiro.alert.repository.AlertRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertBroadcastService broadcastService;
    private final EntityManager em;

    private void setRls(UUID orgId) {
        if (orgId != null) {
            em.createNativeQuery("SELECT set_config('app.current_org_id', :tid, true)")
              .setParameter("tid", orgId.toString())
              .getSingleResult();
        }
    }

    @Transactional
    public AlertResponse create(UUID cameraId, UUID orgId, AlertType type,
                                String message, AlertSeverity severity, String kafkaEventId) {
        if (alertRepository.findByKafkaEventId(kafkaEventId).isPresent()) {
            return null;
        }
        setRls(orgId);
        Alert alert = Alert.builder()
                .cameraId(cameraId)
                .orgId(orgId)
                .type(type)
                .message(message)
                .severity(severity)
                .status(AlertStatus.TRIGGERED)
                .kafkaEventId(kafkaEventId)
                .build();
        alert = alertRepository.save(alert);
        broadcastService.broadcast(alert);
        return AlertResponse.from(alert);
    }

    @Transactional
    public AlertResponse acknowledge(UUID alertId, UUID userId, UUID orgId) {
        setRls(orgId);
        Alert alert = alertRepository.findById(alertId)
                .filter(a -> a.getOrgId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(userId);
        alert.setAcknowledgedAt(OffsetDateTime.now());
        return AlertResponse.from(alertRepository.save(alert));
    }

    @Transactional(readOnly = true)
    public Page<AlertResponse> findByOrg(UUID orgId, AlertStatus status, Pageable pageable) {
        setRls(orgId);
        if (status != null) {
            return alertRepository.findByOrgIdAndStatus(orgId, status, pageable)
                    .map(AlertResponse::from);
        }
        return alertRepository.findByOrgId(orgId, pageable).map(AlertResponse::from);
    }
}
