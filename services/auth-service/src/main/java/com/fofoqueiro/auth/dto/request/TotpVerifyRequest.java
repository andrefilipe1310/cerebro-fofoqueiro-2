package com.fofoqueiro.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TotpVerifyRequest(
        @NotBlank String tempToken,
        @NotBlank @Pattern(regexp = "\\d{6}") String totpCode
) {}
