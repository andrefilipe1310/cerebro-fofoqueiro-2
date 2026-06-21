package com.fofoqueiro.camera.repository;

import com.fofoqueiro.camera.domain.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, UUID> {

    @Query(value = "SELECT * FROM cameras.locations WHERE org_id = :orgId ORDER BY name",
           nativeQuery = true)
    List<Location> findByOrgId(UUID orgId);

    @Query(value = "SELECT * FROM cameras.locations WHERE org_id = :orgId AND id = :id LIMIT 1",
           nativeQuery = true)
    Optional<Location> findByOrgIdAndId(UUID orgId, UUID id);
}
