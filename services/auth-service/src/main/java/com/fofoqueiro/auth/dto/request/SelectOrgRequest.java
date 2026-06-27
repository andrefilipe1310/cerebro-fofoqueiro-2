package com.fofoqueiro.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SelectOrgRequest(
        @JsonProperty("temp_token") @NotBlank String tempToken,
        @JsonProperty("org_id") @NotNull UUID orgId
) {}
