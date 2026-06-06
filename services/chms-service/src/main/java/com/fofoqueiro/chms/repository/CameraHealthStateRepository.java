package com.fofoqueiro.chms.repository;

import com.fofoqueiro.chms.domain.entity.CameraHealthState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CameraHealthStateRepository extends JpaRepository<CameraHealthState, UUID> {

    @Query(value = "SELECT * FROM health.camera_health_state WHERE tenant_id = :tenantId",
           nativeQuery = true)
    List<CameraHealthState> findByTenantId(UUID tenantId);
}
