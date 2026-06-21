package com.fofoqueiro.auth.repository;

import com.fofoqueiro.auth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // auth_service_user é dona da tabela — RLS não se aplica, findByEmail funciona globalmente
    @Query(value = "SELECT * FROM auth.users WHERE email = :email AND active = TRUE LIMIT 1",
           nativeQuery = true)
    Optional<User> findByEmail(String email);

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
