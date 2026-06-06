package com.fofoqueiro.tenant.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fofoqueiro.tenant.domain.entity.Tenant;

import java.util.UUID;

public record TenantResponse(
        UUID id,
        String slug,
        String name,
        String domain,
        String plan,
        @JsonProperty("logo_url") String logoUrl,
        @JsonProperty("css_override") String cssOverride,
        @JsonProperty("max_cameras") int maxCameras,
        @JsonProperty("max_users") int maxUsers,
        @JsonProperty("retention_days") int retentionDays,
        String status
) {
    public static TenantResponse from(Tenant t) {
        return new TenantResponse(t.getId(), t.getSlug(), t.getName(), t.getDomain(),
                t.getPlan().name(), t.getLogoUrl(), t.getCssOverride(),
                t.getMaxCameras(), t.getMaxUsers(), t.getRetentionDays(), t.getStatus().name());
    }
}
