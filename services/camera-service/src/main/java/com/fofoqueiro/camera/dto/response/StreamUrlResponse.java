package com.fofoqueiro.camera.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record StreamUrlResponse(
        @JsonProperty("webrtc_url") String webrtcUrl,
        @JsonProperty("hls_url") String hlsUrl,
        @JsonProperty("thumbnail_url") String thumbnailUrl,
        @JsonProperty("expires_at") OffsetDateTime expiresAt
) {}
