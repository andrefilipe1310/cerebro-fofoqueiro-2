package com.fofoqueiro.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record AuthResponse(
        @JsonProperty("access_token")    String accessToken,
        @JsonProperty("refresh_token")   String refreshToken,
        @JsonProperty("expires_in")      long expiresIn,
        @JsonProperty("requires_2fa")    boolean requiresTwoFa,
        @JsonProperty("temp_token")      String tempToken,
        @JsonProperty("user_id")         UUID userId,
        @JsonProperty("tenant_id")       UUID tenantId,
        @JsonProperty("role")            String role
) {
    public static AuthResponse pending2fa(String tempToken, UUID userId, UUID tenantId) {
        return new AuthResponse(null, null, 0, true, tempToken, userId, tenantId, null);
    }

    public static AuthResponse authenticated(String accessToken, String refreshToken,
                                              long expiresIn, UUID userId, UUID tenantId, String role) {
        return new AuthResponse(accessToken, refreshToken, expiresIn, false, null, userId, tenantId, role);
    }
}
