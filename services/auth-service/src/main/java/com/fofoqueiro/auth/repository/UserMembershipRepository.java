package com.fofoqueiro.auth.repository;

import com.fofoqueiro.auth.domain.entity.UserMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserMembershipRepository extends JpaRepository<UserMembership, UUID> {

    @Query("SELECT m FROM UserMembership m WHERE m.user.id = :userId AND m.active = true")
    List<UserMembership> findActiveByUserId(UUID userId);

    @Query("SELECT m FROM UserMembership m WHERE m.user.id = :userId AND m.orgId = :orgId AND m.active = true")
    Optional<UserMembership> findActiveByUserIdAndOrgId(UUID userId, UUID orgId);

    @Query("SELECT m FROM UserMembership m WHERE m.orgId = :orgId AND m.active = true")
    List<UserMembership> findActiveByOrgId(UUID orgId);
}
