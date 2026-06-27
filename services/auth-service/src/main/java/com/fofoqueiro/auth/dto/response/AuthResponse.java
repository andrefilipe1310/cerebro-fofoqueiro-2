package com.fofoqueiro.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record AuthResponse(
        @JsonProperty("access_token")          String accessToken,
        @JsonProperty("refresh_token")          String refreshToken,
        @JsonProperty("expires_in")             long expiresIn,
        @JsonProperty("requires_2fa")           boolean requiresTwoFa,
        @JsonProperty("requires_org_selection") boolean requiresOrgSelection,
        @JsonProperty("temp_token")             String tempToken,
        @JsonProperty("user_id")                UUID userId,
        @JsonProperty("org_id")                 UUID orgId,
        @JsonProperty("role")                   String role,
        @JsonProperty("orgs")                   List<OrgOption> orgs
) {
    /** Retornado quando 2FA está ativado — aguarda código TOTP */
    public static AuthResponse pending2fa(String tempToken, UUID userId) {
        return new AuthResponse(null, null, 0, true, false, tempToken, userId, null, null, null);
    }

    /** Retornado quando usuário pertence a múltiplas orgs — aguarda seleção */
    public static AuthResponse pendingOrgSelection(String tempToken, UUID userId, List<OrgOption> orgs) {
        return new AuthResponse(null, null, 0, false, true, tempToken, userId, null, null, orgs);
    }

    /** Autenticação completa com JWT scoped para uma organização */
    public static AuthResponse authenticated(String accessToken, String refreshToken,
                                              long expiresIn, UUID userId, UUID orgId, String role) {
        return new AuthResponse(accessToken, refreshToken, expiresIn, false, false, null, userId, orgId, role, null);
    }
}
