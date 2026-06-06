package com.fofoqueiro.audit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fofoqueiro.audit.domain.entity.AuditLog;
import com.fofoqueiro.audit.dto.response.AuditLogResponse;
import com.fofoqueiro.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void record(String eventId, String topic, String rawPayload) {
        if (auditLogRepository.findByEventId(eventId).isPresent()) {
            log.debug("Skipping duplicate audit event: {}", eventId);
            return;
        }
        try {
            JsonNode node = objectMapper.readTree(rawPayload);
            UUID tenantId = node.has("tenantId") ?
                    UUID.fromString(node.get("tenantId").asText()) : null;
            UUID userId = node.has("userId") ?
                    UUID.fromString(node.get("userId").asText()) : null;

            String action = node.has("eventType") ? node.get("eventType").asText() : topic;
            String resourceType = topic.replace(".events", "").toUpperCase();
            String resourceId = node.has("cameraId") ? node.get("cameraId").asText() :
                               node.has("userId") ? node.get("userId").asText() : null;

            AuditLog log = AuditLog.builder()
                    .eventId(eventId)
                    .tenantId(tenantId)
                    .userId(userId)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .rawPayload(rawPayload)
                    .build();
            auditLogRepository.save(log);
        } catch (DataIntegrityViolationException e) {
            log.debug("Duplicate audit event race condition (ignored): {}", eventId);
        } catch (Exception e) {
            log.error("Failed to record audit log for event {}: {}", eventId, e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findByTenant(UUID tenantId, String action, UUID userId, Pageable pageable) {
        if (action != null) {
            return auditLogRepository.findByTenantIdAndAction(tenantId, action, pageable)
                    .map(AuditLogResponse::from);
        }
        if (userId != null) {
            return auditLogRepository.findByTenantIdAndUserId(tenantId, userId, pageable)
                    .map(AuditLogResponse::from);
        }
        return auditLogRepository.findByTenantId(tenantId, pageable).map(AuditLogResponse::from);
    }
}
