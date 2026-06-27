package com.fofoqueiro.auth.security;

import com.fofoqueiro.auth.domain.entity.User;
import com.fofoqueiro.auth.domain.entity.UserMembership;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:900000}") long accessExpirationMs,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(User user, UUID orgId, String role) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer("fofoqueiro")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .claims(Map.of(
                        "orgId", orgId.toString(),
                        "role", role,
                        "email", user.getEmail()
                ))
                .signWith(signingKey)
                .compact();
    }

    /** Temp token usado no fluxo 2FA e na seleção de org. Carrega apenas userId. */
    public String generateTempToken(UUID userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .issuer("fofoqueiro")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 300_000))
                .claims(Map.of("type", "pending"))
                .signWith(signingKey)
                .compact();
    }

    public Claims validateAndExtractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer("fofoqueiro")
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateAndExtractClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public long getRemainingMs(Claims claims) {
        long expMs = claims.getExpiration().toInstant().toEpochMilli();
        long nowMs = Instant.now().toEpochMilli();
        return Math.max(0, expMs - nowMs);
    }
}
