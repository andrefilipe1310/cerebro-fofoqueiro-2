package com.fofoqueiro.camera.controller;

import com.fofoqueiro.camera.dto.request.CreateCameraRequest;
import com.fofoqueiro.camera.dto.request.TestConnectionRequest;
import com.fofoqueiro.camera.dto.request.UpdateCameraRequest;
import com.fofoqueiro.camera.dto.response.CameraResponse;
import com.fofoqueiro.camera.dto.response.StreamUrlResponse;
import com.fofoqueiro.camera.dto.response.TestConnectionResponse;
import com.fofoqueiro.camera.security.OrgContext;
import com.fofoqueiro.camera.service.CameraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cameras")
@RequiredArgsConstructor
public class CameraController {

    private final CameraService cameraService;

    @GetMapping
    public ResponseEntity<Page<CameraResponse>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(cameraService.list(OrgContext.get(), pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CameraResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(cameraService.getById(OrgContext.get(), id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<CameraResponse> create(@Valid @RequestBody CreateCameraRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cameraService.create(OrgContext.get(), req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<CameraResponse> update(@PathVariable UUID id,
                                                  @RequestBody UpdateCameraRequest req) {
        return ResponseEntity.ok(cameraService.update(OrgContext.get(), id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        cameraService.delete(OrgContext.get(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/stream-url")
    public ResponseEntity<StreamUrlResponse> getStreamUrl(@PathVariable UUID id) {
        return ResponseEntity.ok(cameraService.getStreamUrl(OrgContext.get(), id));
    }

    @PostMapping("/test-connection")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<TestConnectionResponse> testConnection(
            @Valid @RequestBody TestConnectionRequest req) {
        return ResponseEntity.ok(cameraService.testConnection(req.rtspUrl()));
    }
}
