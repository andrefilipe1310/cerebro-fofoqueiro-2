package com.fofoqueiro.camera.controller;

import com.fofoqueiro.camera.dto.request.CreateLocationRequest;
import com.fofoqueiro.camera.dto.response.LocationResponse;
import com.fofoqueiro.camera.security.TenantContext;
import com.fofoqueiro.camera.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public ResponseEntity<List<LocationResponse>> list() {
        return ResponseEntity.ok(locationService.list(TenantContext.get()));
    }

    @PostMapping
    public ResponseEntity<LocationResponse> create(@Valid @RequestBody CreateLocationRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(locationService.create(TenantContext.get(), req));
    }
}
