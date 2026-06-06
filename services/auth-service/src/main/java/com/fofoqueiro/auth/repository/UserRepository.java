package com.fofoqueiro.auth.repository;

import com.fofoqueiro.auth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query(value = "SELECT * FROM auth.users WHERE tenant_id = :tenantId ORDER BY email",
           nativeQuery = true)
    List<User> findByTenantId(UUID tenantId);

    @Query(value = "SELECT * FROM auth.users WHERE tenant_id = :tenantId AND email = :email LIMIT 1",
           nativeQuery = true)
    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

    @Modifying
    @Query(value = "UPDATE auth.users SET last_login = :now, failed_attempts = 0, locked_until = NULL WHERE id = :id",
           nativeQuery = true)
    void recordSuccessfulLogin(UUID id, OffsetDateTime now);

    @Modifying
    @Query(value = "UPDATE auth.users SET failed_attempts = failed_attempts + 1, locked_until = CASE WHEN failed_attempts + 1 >= 5 THEN NOW() + INTERVAL '15 minutes' ELSE NULL END WHERE id = :id",
           nativeQuery = true)
    void recordFailedAttempt(UUID id);

    @Modifying
    @Query(value = "UPDATE auth.users SET totp_secret = :secret, totp_enabled = true WHERE id = :id",
           nativeQuery = true)
    void enableTotp(UUID id, String secret);
}
