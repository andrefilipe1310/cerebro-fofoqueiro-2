package com.fofoqueiro.chms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fofoqueiro.chms.domain.entity.CameraHealthState;
import com.fofoqueiro.chms.domain.entity.HealthEvent;
import com.fofoqueiro.chms.domain.entity.OutboxEvent;
import com.fofoqueiro.chms.repository.CameraHealthStateRepository;
import com.fofoqueiro.chms.repository.HealthEventRepository;
import com.fofoqueiro.chms.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaMtxPollingService {

    private final CameraHealthStateRepository healthStateRepository;
    private final HealthEventRepository healthEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${mediamtx.api-url:http://mediamtx:9997}")
    private String mediamtxApiUrl;

    @Scheduled(fixedDelayString = "${chms.polling-interval-ms:30000}")
    @Transactional
    public void pollMediaMtx() {
        Set<String> activePaths = fetchActivePaths();
        List<CameraHealthState> allStates = healthStateRepository.findAll();

        for (CameraHealthState state : allStates) {
            String expectedPath = String.format("tenant_%s/camera_%s/main",
                    state.getTenantId(), state.getCameraId());
            boolean isOnline = activePaths.contains(expectedPath);

            String previousStatus = state.getStatus();
            String newStatus = isOnline ? "ONLINE" : "OFFLINE";

            if (!newStatus.equals(previousStatus)) {
                state.setStatus(newStatus);
                state.setLastSeenAt(isOnline ? OffsetDateTime.now() : state.getLastSeenAt());
                state.setConsecutiveFailures(isOnline ? 0 : state.getConsecutiveFailures() + 1);
                if (isOnline) {
                    state.setRecordingConfidenceScore(BigDecimal.valueOf(100));
                }
                healthStateRepository.save(state);

                String eventType = isOnline ? "CAME_ONLINE" : "WENT_OFFLINE";
                String severity = isOnline ? "INFO" : (state.getConsecutiveFailures() > 3 ? "CRITICAL" : "WARNING");

                HealthEvent event = HealthEvent.builder()
                        .cameraId(state.getCameraId())
                        .tenantId(state.getTenantId())
                        .type(eventType)
                        .severity(severity)
                        .build();
                healthEventRepository.save(event);

                publishHealthEvent(state, eventType, severity);
                log.info("Camera {} transitioned {} → {}", state.getCameraId(), previousStatus, newStatus);
            } else if (isOnline) {
                state.setLastSeenAt(OffsetDateTime.now());
                healthStateRepository.save(state);
            }
        }
    }

    private Set<String> fetchActivePaths() {
        try {
            RestTemplate rt = new RestTemplate();
            ResponseEntity<String> response = rt.getForEntity(
                    mediamtxApiUrl + "/v3/paths/list", String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            Set<String> paths = new HashSet<>();
            JsonNode items = root.get("items");
            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    JsonNode name = item.get("name");
                    if (name != null) paths.add(name.asText());
                }
            }
            return paths;
        } catch (Exception e) {
            log.warn("Falha ao consultar MediaMTX: {}", e.getMessage());
            return Collections.emptySet();
        }
    }

    private void publishHealthEvent(CameraHealthState state, String eventType, String severity) {
        try {
            Map<String, Object> payload = Map.of(
                    "cameraId", state.getCameraId().toString(),
                    "tenantId", state.getTenantId().toString(),
                    "eventType", eventType,
                    "severity", severity,
                    "status", state.getStatus()
            );
            OutboxEvent event = OutboxEvent.builder()
                    .topic("health.events")
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .attempts(0)
                    .build();
            outboxEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Falha ao publicar evento health: {}", e.getMessage());
        }
    }
}
