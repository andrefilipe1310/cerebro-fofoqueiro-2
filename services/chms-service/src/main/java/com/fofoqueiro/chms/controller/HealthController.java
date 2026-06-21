package com.fofoqueiro.chms.controller;

import com.fofoqueiro.chms.dto.response.CameraHealthResponse;
import com.fofoqueiro.chms.repository.CameraHealthStateRepository;
import com.fofoqueiro.chms.security.OrgContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final CameraHealthStateRepository healthStateRepository;

    @GetMapping("/cameras")
    public ResponseEntity<List<CameraHealthResponse>> listAll() {
        UUID orgId = OrgContext.get();
        return ResponseEntity.ok(
                healthStateRepository.findByOrgId(orgId)
                        .stream().map(CameraHealthResponse::from).toList()
        );
    }

    @GetMapping("/cameras/{cameraId}")
    public ResponseEntity<CameraHealthResponse> getById(@PathVariable UUID cameraId) {
        return healthStateRepository.findById(cameraId)
                .map(CameraHealthResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
