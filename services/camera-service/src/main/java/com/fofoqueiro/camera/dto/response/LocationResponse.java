package com.fofoqueiro.camera.dto.response;

import com.fofoqueiro.camera.domain.entity.Location;

import java.math.BigDecimal;
import java.util.UUID;

public record LocationResponse(
        UUID id,
        String name,
        String address,
        BigDecimal lat,
        BigDecimal lng
) {
    public static LocationResponse from(Location l) {
        return new LocationResponse(l.getId(), l.getName(), l.getAddress(), l.getLat(), l.getLng());
    }
}
