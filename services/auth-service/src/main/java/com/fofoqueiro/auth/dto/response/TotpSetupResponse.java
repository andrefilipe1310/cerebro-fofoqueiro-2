package com.fofoqueiro.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TotpSetupResponse(
        String secret,
        @JsonProperty("qr_code_uri") String qrCodeUri
) {}
