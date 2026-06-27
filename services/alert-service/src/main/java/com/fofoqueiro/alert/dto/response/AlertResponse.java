package com.fofoqueiro.alert.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fofoqueiro.alert.domain.entity.Alert;
import com.fofoqueiro.alert.domain.enums.AlertSeverity;
import com.fofoqueiro.alert.domain.enums.AlertStatus;
import com.fofoqueiro.alert.domain.enums.AlertType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        @JsonProperty("camera_id") UUID cameraId,
        @JsonProperty("org_id") UUID orgId,
        AlertType type,
        String message,
        AlertSeverity severity,
        AlertStatus status,
        @JsonProperty("triggered_at") OffsetDateTime triggeredAt,
        @JsonProperty("acknowledged_at") OffsetDateTime acknowledgedAt,
        @JsonProperty("acknowledged_by") UUID acknowledgedBy
) {
    public static AlertResponse from(Alert a) {
        return new AlertResponse(a.getId(), a.getCameraId(), a.getOrgId(),
                a.getType(), a.getMessage(), a.getSeverity(), a.getStatus(),
                a.getTriggeredAt(), a.getAcknowledgedAt(), a.getAcknowledgedBy());
    }
}
