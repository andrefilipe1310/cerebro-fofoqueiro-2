package com.fofoqueiro.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fofoqueiro.auth.domain.entity.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        @JsonProperty("tenant_id") UUID tenantId,
        String email,
        String role,
        @JsonProperty("totp_enabled") boolean totpEnabled,
        boolean active
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                user.getRole().name(),
                user.isTotpEnabled(),
                user.isActive()
        );
    }
}
