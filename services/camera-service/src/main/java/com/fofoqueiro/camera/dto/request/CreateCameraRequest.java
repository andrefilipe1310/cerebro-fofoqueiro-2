package com.fofoqueiro.camera.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateCameraRequest(
        @NotBlank String name,
        @JsonProperty("rtsp_url") String rtspUrl,
        @JsonProperty("sub_stream_url") String subStreamUrl,
        @JsonProperty("location_id") UUID locationId,
        BigDecimal lat,
        BigDecimal lng,
        @JsonProperty("ptz_enabled") boolean ptzEnabled
) {}
