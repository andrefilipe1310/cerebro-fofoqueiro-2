package com.fofoqueiro.camera.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateCameraRequest(
        String name,
        @JsonProperty("rtsp_url") String rtspUrl,
        @JsonProperty("location_id") UUID locationId,
        BigDecimal lat,
        BigDecimal lng,
        @JsonProperty("ptz_enabled") Boolean ptzEnabled
) {}
