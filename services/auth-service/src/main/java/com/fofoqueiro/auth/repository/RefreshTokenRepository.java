package com.fofoqueiro.auth.repository;

import com.fofoqueiro.auth.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Query(value = "SELECT * FROM auth.refresh_tokens WHERE token_hash = :hash AND revoked = false AND expires_at > NOW() LIMIT 1",
           nativeQuery = true)
    Optional<RefreshToken> findValidByTokenHash(String hash);

    @Modifying
    @Query(value = "UPDATE auth.refresh_tokens SET revoked = true WHERE user_id = :userId",
           nativeQuery = true)
    void revokeAllByUserId(UUID userId);

    @Modifying
    @Query(value = "UPDATE auth.refresh_tokens SET revoked = true WHERE token_hash = :hash",
           nativeQuery = true)
    void revokeByTokenHash(String hash);

    @Modifying
    @Query(value = "DELETE FROM auth.refresh_tokens WHERE expires_at < :before",
           nativeQuery = true)
    void deleteExpiredBefore(OffsetDateTime before);
}
