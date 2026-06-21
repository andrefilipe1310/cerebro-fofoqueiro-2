package com.fofoqueiro.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fofoqueiro.auth.domain.entity.User;
import com.fofoqueiro.auth.domain.entity.UserMembership;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String role,
        @JsonProperty("org_id") UUID orgId,
        @JsonProperty("totp_enabled") boolean totpEnabled,
        boolean active
) {
    public static UserResponse from(User user, UserMembership membership) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                membership != null ? membership.getRole().name() : null,
                membership != null ? membership.getOrgId() : null,
                user.isTotpEnabled(),
                user.isActive()
        );
    }
}
