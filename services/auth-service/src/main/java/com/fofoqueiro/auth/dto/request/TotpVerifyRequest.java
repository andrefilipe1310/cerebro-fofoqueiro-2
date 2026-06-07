package com.fofoqueiro.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TotpVerifyRequest(
        @JsonProperty("temp_token") @NotBlank String tempToken,
        @JsonProperty("totp_code") @NotBlank @Pattern(regexp = "\\d{6}") String totpCode
) {}
