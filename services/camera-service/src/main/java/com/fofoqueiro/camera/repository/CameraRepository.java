package com.fofoqueiro.camera.repository;

import com.fofoqueiro.camera.domain.entity.Camera;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CameraRepository extends JpaRepository<Camera, UUID> {

    @Query(value = "SELECT * FROM cameras.cameras WHERE tenant_id = :tenantId AND status != 'DELETED' ORDER BY created_at DESC",
           countQuery = "SELECT count(*) FROM cameras.cameras WHERE tenant_id = :tenantId AND status != 'DELETED'",
           nativeQuery = true)
    Page<Camera> findActiveByTenantId(UUID tenantId, Pageable pageable);

    @Query(value = "SELECT * FROM cameras.cameras WHERE tenant_id = :tenantId AND id = :id AND status != 'DELETED' LIMIT 1",
           nativeQuery = true)
    Optional<Camera> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query(value = "SELECT count(*) FROM cameras.cameras WHERE tenant_id = :tenantId AND status != 'DELETED'",
           nativeQuery = true)
    long countActiveByTenantId(UUID tenantId);
}
