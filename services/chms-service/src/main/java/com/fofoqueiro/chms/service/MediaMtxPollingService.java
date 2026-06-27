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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern PATH_PATTERN =
            Pattern.compile("^org_([\\w-]+)/camera_([\\w-]+)/main$");

    @Scheduled(fixedDelayString = "${chms.polling-interval-ms:30000}")
    @Transactional
    public void pollMediaMtx() {
        Map<String, JsonNode> activePaths = fetchActivePathDetails();
        autoRegisterNewCameras(activePaths);
        List<CameraHealthState> allStates = healthStateRepository.findAll();

        for (CameraHealthState state : allStates) {
            String expectedPath = String.format("org_%s/camera_%s/main",
                    state.getOrgId(), state.getCameraId());
            JsonNode pathNode = activePaths.get(expectedPath);
            boolean isOnline = pathNode != null && pathNode.path("ready").asBoolean(false);

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
                        .orgId(state.getOrgId())
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

    private void autoRegisterNewCameras(Map<String, JsonNode> activePaths) {
        for (String pathName : activePaths.keySet()) {
            Matcher m = PATH_PATTERN.matcher(pathName);
            if (!m.matches()) continue;
            UUID cameraId = UUID.fromString(m.group(2));
            UUID orgId    = UUID.fromString(m.group(1));
            if (!healthStateRepository.existsById(cameraId)) {
                CameraHealthState state = CameraHealthState.builder()
                        .cameraId(cameraId)
                        .orgId(orgId)
                        .status("UNKNOWN")
                        .consecutiveFailures(0)
                        .recordingConfidenceScore(BigDecimal.ZERO)
                        .build();
                healthStateRepository.save(state);
                log.info("Auto-registrado health state para câmera {}", cameraId);
            }
        }
    }

    private Map<String, JsonNode> fetchActivePathDetails() {
        try {
            RestTemplate rt = new RestTemplate();
            ResponseEntity<String> response = rt.getForEntity(
                    mediamtxApiUrl + "/v3/paths/list", String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            Map<String, JsonNode> paths = new HashMap<>();
            JsonNode items = root.get("items");
            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    JsonNode name = item.get("name");
                    if (name != null) paths.put(name.asText(), item);
                }
            }
            return paths;
        } catch (Exception e) {
            log.warn("Falha ao consultar MediaMTX: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private void publishHealthEvent(CameraHealthState state, String eventType, String severity) {
        try {
            Map<String, Object> payload = Map.of(
                    "cameraId", state.getCameraId().toString(),
                    "orgId", state.getOrgId().toString(),
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
