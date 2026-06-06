package com.fofoqueiro.tenant.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateTenantRequest(
        String name,
        String domain,
        @JsonProperty("logo_url") String logoUrl,
        @JsonProperty("css_override") String cssOverride
) {}
