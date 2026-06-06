package com.fofoqueiro.recording.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record DownloadUrlResponse(
        String url,
        @JsonProperty("expires_at") OffsetDateTime expiresAt
) {}
