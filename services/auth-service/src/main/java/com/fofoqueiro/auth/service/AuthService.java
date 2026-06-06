package com.fofoqueiro.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fofoqueiro.auth.domain.entity.OutboxEvent;
import com.fofoqueiro.auth.domain.entity.RefreshToken;
import com.fofoqueiro.auth.domain.entity.User;
import com.fofoqueiro.auth.dto.request.LoginRequest;
import com.fofoqueiro.auth.dto.request.RefreshRequest;
import com.fofoqueiro.auth.dto.request.TotpVerifyRequest;
import com.fofoqueiro.auth.dto.response.AuthResponse;
import com.fofoqueiro.auth.dto.response.TotpSetupResponse;
import com.fofoqueiro.auth.dto.response.UserResponse;
import com.fofoqueiro.auth.exception.AuthException;
import com.fofoqueiro.auth.repository.OutboxEventRepository;
import com.fofoqueiro.auth.repository.RefreshTokenRepository;
import com.fofoqueiro.auth.repository.UserRepository;
import com.fofoqueiro.auth.security.JwtService;
import com.fofoqueiro.auth.security.TotpService;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JwtService jwtService;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public AuthResponse login(LoginRequest req) {
        setRls(req.tenantId());

        User user = userRepository.findByTenantIdAndEmail(req.tenantId(), req.email())
                .orElseThrow(() -> new AuthException("Credenciais inválidas"));

        if (!user.isActive()) {
            throw new AuthException("Conta desativada");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new AuthException("Conta bloqueada temporariamente. Tente novamente mais tarde.");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            userRepository.recordFailedAttempt(user.getId());
            throw new AuthException("Credenciais inválidas");
        }

        if (user.isTotpEnabled()) {
            String tempToken = jwtService.generateTempToken(user.getId(), user.getTenantId());
            return AuthResponse.pending2fa(tempToken, user.getId(), user.getTenantId());
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse verifyTotp(TotpVerifyRequest req) {
        Claims claims;
        try {
            claims = jwtService.validateAndExtractClaims(req.tempToken());
        } catch (Exception e) {
            throw new AuthException("Token temporário inválido ou expirado");
        }

        if (!"2fa_pending".equals(claims.get("type", String.class))) {
            throw new AuthException("Token inválido para verificação 2FA");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        UUID tenantId = UUID.fromString(claims.get("tenantId", String.class));

        setRls(tenantId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Usuário não encontrado"));

        if (!totpService.verify(user.getTotpSecret(), req.totpCode())) {
            throw new AuthException("Código TOTP inválido");
        }

        return issueTokens(user);
    }

    @Transactional
    public TotpSetupResponse setupTotp(UUID userId, UUID tenantId) {
        setRls(tenantId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Usuário não encontrado"));

        String secret = totpService.generateSecret();
        userRepository.enableTotp(userId, secret);

        return new TotpSetupResponse(secret, totpService.buildQrCodeUri(secret, user.getEmail()));
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest req) {
        String tokenHash = hashToken(req.refreshToken());

        RefreshToken stored = refreshTokenRepository.findValidByTokenHash(tokenHash)
                .orElseThrow(() -> new AuthException("Refresh token inválido ou expirado"));

        setRls(null);

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new AuthException("Usuário não encontrado"));

        refreshTokenRepository.revokeByTokenHash(tokenHash);

        return issueTokens(user);
    }

    @Transactional
    public void logout(String accessToken, UUID tenantId) {
        try {
            Claims claims = jwtService.validateAndExtractClaims(accessToken);
            long remainingMs = jwtService.getRemainingMs(claims);
            if (remainingMs > 0) {
                redisTemplate.opsForValue().set("blacklist:" + accessToken, "1",
                        remainingMs, TimeUnit.MILLISECONDS);
            }
            String userId = claims.getSubject();
            refreshTokenRepository.revokeAllByUserId(UUID.fromString(userId));
        } catch (Exception e) {
            log.warn("Logout com token inválido: {}", e.getMessage());
        }
    }

    public UserResponse getMe(UUID userId, UUID tenantId) {
        setRls(tenantId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Usuário não encontrado"));
        return UserResponse.from(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefresh = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawRefresh);

        RefreshToken rt = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(OffsetDateTime.now().plusSeconds(jwtService.getRefreshExpirationMs() / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(rt);

        userRepository.recordSuccessfulLogin(user.getId(), OffsetDateTime.now());

        publishAuthEvent(user, "USER_LOGGED_IN");

        return AuthResponse.authenticated(
                accessToken, rawRefresh,
                jwtService.getAccessExpirationMs() / 1000,
                user.getId(), user.getTenantId(), user.getRole().name()
        );
    }

    private void publishAuthEvent(User user, String eventType) {
        try {
            Map<String, Object> payload = Map.of(
                    "userId", user.getId().toString(),
                    "email", user.getEmail(),
                    "role", user.getRole().name()
            );
            OutboxEvent event = OutboxEvent.builder()
                    .topic("auth.events")
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .attempts(0)
                    .build();
            outboxEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Falha ao publicar evento auth: {}", e.getMessage());
        }
    }

    private void setRls(UUID tenantId) {
        if (tenantId != null) {
            em.createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, true)")
              .setParameter("tid", tenantId.toString())
              .getSingleResult();
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer hash do token", e);
        }
    }
}
