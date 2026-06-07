package com.fofoqueiro.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LoginRequest(
        @JsonProperty("tenant_id") @NotNull UUID tenantId,
        @NotBlank @Email String email,
        @NotBlank String password
) {}
