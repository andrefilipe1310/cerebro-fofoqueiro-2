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

    @Query(value = "SELECT * FROM audit.audit_logs WHERE org_id = :orgId ORDER BY occurred_at DESC",
           countQuery = "SELECT count(*) FROM audit.audit_logs WHERE org_id = :orgId",
           nativeQuery = true)
    Page<AuditLog> findByOrgId(UUID orgId, Pageable pageable);

    @Query(value = "SELECT * FROM audit.audit_logs WHERE org_id = :orgId AND action = :action ORDER BY occurred_at DESC",
           countQuery = "SELECT count(*) FROM audit.audit_logs WHERE org_id = :orgId AND action = :action",
           nativeQuery = true)
    Page<AuditLog> findByOrgIdAndAction(UUID orgId, String action, Pageable pageable);

    @Query(value = "SELECT * FROM audit.audit_logs WHERE org_id = :orgId AND user_id = :userId ORDER BY occurred_at DESC",
           countQuery = "SELECT count(*) FROM audit.audit_logs WHERE org_id = :orgId AND user_id = :userId",
           nativeQuery = true)
    Page<AuditLog> findByOrgIdAndUserId(UUID orgId, UUID userId, Pageable pageable);
}
