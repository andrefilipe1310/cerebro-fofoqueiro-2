package com.fofoqueiro.audit.repository;

import com.fofoqueiro.audit.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Optional<AuditLog> findByEventId(String eventId);

    @Query(value = "SELECT * FROM audit.audit_logs WHERE tenant_id = :tenantId ORDER BY occurred_at DESC",
           countQuery = "SELECT count(*) FROM audit.audit_logs WHERE tenant_id = :tenantId",
           nativeQuery = true)
    Page<AuditLog> findByTenantId(UUID tenantId, Pageable pageable);

    @Query(value = "SELECT * FROM audit.audit_logs WHERE tenant_id = :tenantId AND action = :action ORDER BY occurred_at DESC",
           countQuery = "SELECT count(*) FROM audit.audit_logs WHERE tenant_id = :tenantId AND action = :action",
           nativeQuery = true)
    Page<AuditLog> findByTenantIdAndAction(UUID tenantId, String action, Pageable pageable);

    @Query(value = "SELECT * FROM audit.audit_logs WHERE tenant_id = :tenantId AND user_id = :userId ORDER BY occurred_at DESC",
           countQuery = "SELECT count(*) FROM audit.audit_logs WHERE tenant_id = :tenantId AND user_id = :userId",
           nativeQuery = true)
    Page<AuditLog> findByTenantIdAndUserId(UUID tenantId, UUID userId, Pageable pageable);
}
