package com.fofoqueiro.camera.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fofoqueiro.camera.domain.entity.Camera;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CameraResponse(
        UUID id,
        @JsonProperty("tenant_id") UUID tenantId,
        @JsonProperty("location_id") UUID locationId,
        String name,
        String status,
        @JsonProperty("ptz_enabled") boolean ptzEnabled,
        BigDecimal lat,
        BigDecimal lng,
        @JsonProperty("created_at") OffsetDateTime createdAt
) {
    public static CameraResponse from(Camera c) {
        return new CameraResponse(
                c.getId(), c.getTenantId(), c.getLocationId(), c.getName(),
                c.getStatus().name(), c.isPtzEnabled(), c.getLat(), c.getLng(), c.getCreatedAt()
        );
    }
}
