package com.fofoqueiro.camera.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record TestConnectionRequest(
        @NotBlank @JsonProperty("rtsp_url") String rtspUrl
) {}
