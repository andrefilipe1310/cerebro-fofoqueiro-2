package com.fofoqueiro.tenant.controller;

import com.fofoqueiro.tenant.dto.request.CreateTenantRequest;
import com.fofoqueiro.tenant.dto.request.UpdateTenantRequest;
import com.fofoqueiro.tenant.dto.response.TenantResponse;
import com.fofoqueiro.tenant.security.TenantContext;
import com.fofoqueiro.tenant.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping("/api/v1/tenants/me")
    public ResponseEntity<TenantResponse> getMyTenant() {
        return ResponseEntity.ok(tenantService.findById(TenantContext.get()));
    }

    @PutMapping("/api/v1/tenants/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TenantResponse> updateMyTenant(@RequestBody UpdateTenantRequest req) {
        return ResponseEntity.ok(tenantService.update(TenantContext.get(), req));
    }

    @GetMapping("/api/v1/tenants/config")
    public ResponseEntity<TenantResponse> getTenantConfig(
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String slug) {
        if (slug != null && !slug.isBlank()) {
            return ResponseEntity.ok(tenantService.findBySlug(slug));
        }
        return ResponseEntity.ok(tenantService.findByDomain(domain != null ? domain : ""));
    }

    @GetMapping("/api/v1/admin/tenants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TenantResponse>> listAll() {
        return ResponseEntity.ok(tenantService.listAll());
    }

    @PostMapping("/api/v1/admin/tenants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody CreateTenantRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.create(req));
    }
}
