package com.fofoqueiro.chms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fofoqueiro.chms.domain.entity.CameraHealthState;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CameraHealthResponse(
        @JsonProperty("camera_id") UUID cameraId,
        @JsonProperty("org_id") UUID orgId,
        String status,
        @JsonProperty("last_seen_at") OffsetDateTime lastSeenAt,
        @JsonProperty("consecutive_failures") int consecutiveFailures,
        @JsonProperty("recording_confidence_score") BigDecimal recordingConfidenceScore
) {
    public static CameraHealthResponse from(CameraHealthState s) {
        return new CameraHealthResponse(s.getCameraId(), s.getOrgId(), s.getStatus(),
                s.getLastSeenAt(), s.getConsecutiveFailures(), s.getRecordingConfidenceScore());
    }
}
