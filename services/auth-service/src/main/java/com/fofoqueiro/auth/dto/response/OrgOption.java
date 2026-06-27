package com.fofoqueiro.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record OrgOption(
        UUID id,
        String slug,
        String name,
        @JsonProperty("logo_url") String logoUrl,
        String role
) {}
