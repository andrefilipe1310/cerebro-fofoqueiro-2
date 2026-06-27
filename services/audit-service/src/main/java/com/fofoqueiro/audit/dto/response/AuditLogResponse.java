package com.fofoqueiro.audit.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fofoqueiro.audit.domain.entity.AuditLog;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        @JsonProperty("event_id") String eventId,
        @JsonProperty("org_id") UUID orgId,
        @JsonProperty("user_id") UUID userId,
        String action,
        @JsonProperty("resource_type") String resourceType,
        @JsonProperty("resource_id") String resourceId,
        @JsonProperty("ip_address") String ipAddress,
        @JsonProperty("occurred_at") OffsetDateTime occurredAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getEventId(), log.getOrgId(),
                log.getUserId(), log.getAction(), log.getResourceType(), log.getResourceId(),
                log.getIpAddress(), log.getOccurredAt());
    }
}
