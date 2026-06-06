package com.fofoqueiro.tenant.repository;

import com.fofoqueiro.tenant.domain.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    @Query(value = "SELECT * FROM tenants.tenants WHERE slug = :slug LIMIT 1", nativeQuery = true)
    Optional<Tenant> findBySlug(String slug);

    @Query(value = "SELECT * FROM tenants.tenants WHERE domain = :domain LIMIT 1", nativeQuery = true)
    Optional<Tenant> findByDomain(String domain);
}
