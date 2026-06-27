package com.fofoqueiro.audit.controller;

import com.fofoqueiro.audit.dto.response.AuditLogResponse;
import com.fofoqueiro.audit.security.OrgContext;
import com.fofoqueiro.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(auditLogService.findByOrg(OrgContext.get(), action, userId, pageable));
    }
}
