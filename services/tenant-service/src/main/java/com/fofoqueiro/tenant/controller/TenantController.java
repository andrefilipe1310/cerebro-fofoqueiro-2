package com.fofoqueiro.tenant.controller;

import com.fofoqueiro.tenant.dto.request.CreateTenantRequest;
import com.fofoqueiro.tenant.dto.request.UpdateTenantRequest;
import com.fofoqueiro.tenant.dto.response.TenantResponse;
import com.fofoqueiro.tenant.security.OrgContext;
import com.fofoqueiro.tenant.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping("/api/v1/organizations/me")
    public ResponseEntity<TenantResponse> getMyOrg() {
        return ResponseEntity.ok(tenantService.findById(requireOrgContext()));
    }

    @PutMapping("/api/v1/organizations/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TenantResponse> updateMyOrg(@RequestBody UpdateTenantRequest req) {
        return ResponseEntity.ok(tenantService.update(requireOrgContext(), req));
    }

    /** Endpoint público para resolução de white-label — sem autenticação */
    @GetMapping("/api/v1/organizations/config")
    public ResponseEntity<TenantResponse> getOrgConfig(
            @RequestParam(required = false) String domain,
            @RequestParam(required = false) String slug) {
        if (slug != null && !slug.isBlank()) {
            return ResponseEntity.ok(tenantService.findBySlug(slug));
        }
        return ResponseEntity.ok(tenantService.findByDomain(domain != null ? domain : ""));
    }

    @GetMapping("/api/v1/admin/organizations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TenantResponse>> listAll() {
        return ResponseEntity.ok(tenantService.listAll());
    }

    @PostMapping("/api/v1/admin/organizations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody CreateTenantRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.create(req));
    }

    /** Valida que o OrgContext foi populado pelo JWT — retorna 401 se não. */
    private UUID requireOrgContext() {
        UUID orgId = OrgContext.get();
        if (orgId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Token inválido ou sem org_id — use um token scoped (POST /auth/select-org)");
        }
        return orgId;
    }
}
