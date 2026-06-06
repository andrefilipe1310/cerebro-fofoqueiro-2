package com.fofoqueiro.recording.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fofoqueiro.recording.domain.entity.Recording;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RecordingResponse(
        UUID id,
        @JsonProperty("camera_id") UUID cameraId,
        @JsonProperty("tenant_id") UUID tenantId,
        @JsonProperty("r2_key") String r2Key,
        @JsonProperty("started_at") OffsetDateTime startedAt,
        @JsonProperty("ended_at") OffsetDateTime endedAt,
        @JsonProperty("duration_seconds") Integer durationSeconds,
        @JsonProperty("size_bytes") Long sizeBytes,
        @JsonProperty("has_motion") boolean hasMotion
) {
    public static RecordingResponse from(Recording r) {
        return new RecordingResponse(r.getId(), r.getCameraId(), r.getTenantId(),
                r.getR2Key(), r.getStartedAt(), r.getEndedAt(),
                r.getDurationSeconds(), r.getSizeBytes(), r.isHasMotion());
    }
}
