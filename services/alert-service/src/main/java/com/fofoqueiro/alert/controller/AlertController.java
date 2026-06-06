package com.fofoqueiro.alert.controller;

import com.fofoqueiro.alert.domain.enums.AlertStatus;
import com.fofoqueiro.alert.dto.response.AlertResponse;
import com.fofoqueiro.alert.security.TenantContext;
import com.fofoqueiro.alert.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<Page<AlertResponse>> list(
            @RequestParam(required = false) AlertStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(alertService.findByTenant(TenantContext.get(), status, pageable));
    }

    @PatchMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<AlertResponse> acknowledge(
            @PathVariable UUID id,
            @AuthenticationPrincipal String userId) {
        UUID uid = UUID.fromString(userId);
        return ResponseEntity.ok(alertService.acknowledge(id, uid, TenantContext.get()));
    }
}
