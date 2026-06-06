package com.fofoqueiro.alert.repository;

import com.fofoqueiro.alert.domain.entity.Alert;
import com.fofoqueiro.alert.domain.enums.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    @Query(value = "SELECT * FROM alerts.alerts WHERE tenant_id = :tenantId ORDER BY triggered_at DESC",
           countQuery = "SELECT count(*) FROM alerts.alerts WHERE tenant_id = :tenantId",
           nativeQuery = true)
    Page<Alert> findByTenantId(UUID tenantId, Pageable pageable);

    @Query(value = "SELECT * FROM alerts.alerts WHERE tenant_id = :tenantId AND status = :#{#status.name()} ORDER BY triggered_at DESC",
           countQuery = "SELECT count(*) FROM alerts.alerts WHERE tenant_id = :tenantId AND status = :#{#status.name()}",
           nativeQuery = true)
    Page<Alert> findByTenantIdAndStatus(UUID tenantId, AlertStatus status, Pageable pageable);

    Optional<Alert> findByKafkaEventId(String kafkaEventId);
}
