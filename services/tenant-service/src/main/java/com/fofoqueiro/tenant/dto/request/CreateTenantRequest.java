package com.fofoqueiro.tenant.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fofoqueiro.tenant.domain.enums.TenantPlan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTenantRequest(
        @NotBlank String slug,
        @NotBlank String name,
        String domain,
        @NotNull TenantPlan plan,
        @JsonProperty("max_cameras") int maxCameras,
        @JsonProperty("max_users") int maxUsers,
        @JsonProperty("retention_days") int retentionDays
) {}
