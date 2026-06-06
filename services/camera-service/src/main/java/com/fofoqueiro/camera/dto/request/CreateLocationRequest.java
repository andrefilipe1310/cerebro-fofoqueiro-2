package com.fofoqueiro.camera.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateLocationRequest(
        @NotBlank String name,
        String address,
        BigDecimal lat,
        BigDecimal lng
) {}
