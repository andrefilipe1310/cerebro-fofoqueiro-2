package com.fofoqueiro.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fofoqueiro.auth.domain.entity.OutboxEvent;
import com.fofoqueiro.auth.domain.entity.RefreshToken;
import com.fofoqueiro.auth.domain.entity.User;
import com.fofoqueiro.auth.domain.entity.UserMembership;
import com.fofoqueiro.auth.dto.request.LoginRequest;
import com.fofoqueiro.auth.dto.request.RefreshRequest;
import com.fofoqueiro.auth.dto.request.SelectOrgRequest;
import com.fofoqueiro.auth.dto.request.TotpVerifyRequest;
import com.fofoqueiro.auth.dto.response.AuthResponse;
import com.fofoqueiro.auth.dto.response.OrgOption;
import com.fofoqueiro.auth.dto.response.TotpSetupResponse;
import com.fofoqueiro.auth.dto.response.UserResponse;
import com.fofoqueiro.auth.exception.AuthException;
import com.fofoqueiro.auth.repository.OutboxEventRepository;
import com.fofoqueiro.auth.repository.RefreshTokenRepository;
import com.fofoqueiro.auth.repository.UserMembershipRepository;
import com.fofoqueiro.auth.repository.UserRepository;
import com.fofoqueiro.auth.security.JwtService;
import com.fofoqueiro.auth.security.OrgContext;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMembershipRepository membershipRepository;
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
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new AuthException("Credenciais inválidas"));

        if (!user.isActive()) throw new AuthException("Conta desativada");

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new AuthException("Conta bloqueada temporariamente. Tente novamente mais tarde.");
        }

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            userRepository.recordFailedAttempt(user.getId());
            throw new AuthException("Credenciais inválidas");
        }

        userRepository.recordSuccessfulLogin(user.getId(), OffsetDateTime.now());

        List<UserMembership> memberships = membershipRepository.findActiveByUserId(user.getId());
        if (memberships.isEmpty()) throw new AuthException("Sem acesso a nenhuma organização");

        if (user.isTotpEnabled()) {
            // Guarda lista de memberships no Redis para retomar após TOTP (TTL 5min)
            storePendingMemberships(user.getId(), memberships);
            return AuthResponse.pending2fa(jwtService.generateTempToken(user.getId()), user.getId());
        }

        return resolveOrgsResponse(user, memberships);
    }

    @Transactional
    public AuthResponse verifyTotp(TotpVerifyRequest req) {
        Claims claims;
        try {
            claims = jwtService.validateAndExtractClaims(req.tempToken());
        } catch (Exception e) {
            throw new AuthException("Token temporário inválido ou expirado");
        }

        if (!"pending".equals(claims.get("type", String.class))) {
            throw new AuthException("Token inválido");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Usuário não encontrado"));

        if (!totpService.verify(user.getTotpSecret(), req.totpCode())) {
            throw new AuthException("Código TOTP inválido");
        }

        List<UserMembership> memberships = membershipRepository.findActiveByUserId(userId);
        if (memberships.isEmpty()) throw new AuthException("Sem acesso a nenhuma organização");

        return resolveOrgsResponse(user, memberships);
    }

    /** Chamado quando usuário tem múltiplas orgs e seleciona uma no picker */
    @Transactional
    public AuthResponse selectOrg(SelectOrgRequest req) {
        Claims claims;
        try {
            claims = jwtService.validateAndExtractClaims(req.tempToken());
        } catch (Exception e) {
            throw new AuthException("Token temporário inválido ou expirado");
        }

        if (!"pending".equals(claims.get("type", String.class))) {
            throw new AuthException("Token inválido para seleção de organização");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Usuário não encontrado"));

        UserMembership membership = membershipRepository.findActiveByUserIdAndOrgId(userId, req.orgId())
                .orElseThrow(() -> new AuthException("Organização inválida ou sem permissão"));

        return issueTokens(user, membership);
    }

    @Transactional
    public TotpSetupResponse setupTotp(UUID userId, UUID orgId) {
        setRls(orgId);
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

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new AuthException("Usuário não encontrado"));

        refreshTokenRepository.revokeByTokenHash(tokenHash);

        // orgId persistido no refresh token para saber qual org re-emitir
        if (stored.getOrgId() != null) {
            UserMembership membership = membershipRepository.findActiveByUserIdAndOrgId(user.getId(), stored.getOrgId())
                    .orElseThrow(() -> new AuthException("Acesso à organização revogado"));
            return issueTokens(user, membership);
        }

        // Fallback: única membership ativa
        List<UserMembership> memberships = membershipRepository.findActiveByUserId(user.getId());
        if (memberships.isEmpty()) throw new AuthException("Sem acesso a nenhuma organização");
        if (memberships.size() == 1) return issueTokens(user, memberships.get(0));
        throw new AuthException("Múltiplas organizações — faça login novamente");
    }

    @Transactional
    public void logout(String accessToken, UUID orgId) {
        try {
            Claims claims = jwtService.validateAndExtractClaims(accessToken);
            long remainingMs = jwtService.getRemainingMs(claims);
            if (remainingMs > 0) {
                redisTemplate.opsForValue().set("blacklist:" + accessToken, "1",
                        remainingMs, TimeUnit.MILLISECONDS);
            }
            refreshTokenRepository.revokeAllByUserId(UUID.fromString(claims.getSubject()));
        } catch (Exception e) {
            log.warn("Logout com token inválido: {}", e.getMessage());
        }
    }

    public UserResponse getMe(UUID userId, UUID orgId) {
        setRls(orgId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Usuário não encontrado"));
        UserMembership membership = membershipRepository.findActiveByUserIdAndOrgId(userId, orgId)
                .orElse(null);
        return UserResponse.from(user, membership);
    }

    // ─── Privados ──────────────────────────────────────────────────────────────

    private AuthResponse resolveOrgsResponse(User user, List<UserMembership> memberships) {
        if (memberships.size() == 1) {
            return issueTokens(user, memberships.get(0));
        }
        // Múltiplas orgs: busca dados das orgs para o picker (cross-schema read grant aplicado em V3)
        List<OrgOption> orgs = loadOrgOptions(memberships);
        return AuthResponse.pendingOrgSelection(jwtService.generateTempToken(user.getId()), user.getId(), orgs);
    }

    @SuppressWarnings("unchecked")
    private List<OrgOption> loadOrgOptions(List<UserMembership> memberships) {
        UUID[] ids = memberships.stream().map(UserMembership::getOrgId).toArray(UUID[]::new);
        List<Object[]> rows = em.createNativeQuery(
                "SELECT id::text, slug, name, logo_url FROM organizations.organizations WHERE id = ANY(:ids)")
                .setParameter("ids", ids)
                .getResultList();

        Map<UUID, Object[]> byId = new HashMap<>();
        for (Object[] row : rows) {
            byId.put(UUID.fromString((String) row[0]), row);
        }

        return memberships.stream().map(m -> {
            Object[] row = byId.get(m.getOrgId());
            String name    = row != null ? (String) row[2] : m.getOrgId().toString();
            String slug    = row != null ? (String) row[1] : "";
            String logoUrl = row != null ? (String) row[3] : null;
            return new OrgOption(m.getOrgId(), slug, name, logoUrl, m.getRole().name());
        }).toList();
    }

    private AuthResponse issueTokens(User user, UserMembership membership) {
        setRls(membership.getOrgId());

        String accessToken = jwtService.generateAccessToken(user, membership.getOrgId(), membership.getRole().name());
        String rawRefresh  = UUID.randomUUID().toString();
        String tokenHash   = hashToken(rawRefresh);

        RefreshToken rt = RefreshToken.builder()
                .userId(user.getId())
                .orgId(membership.getOrgId())
                .tokenHash(tokenHash)
                .expiresAt(OffsetDateTime.now().plusSeconds(jwtService.getRefreshExpirationMs() / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(rt);

        publishAuthEvent(user, membership, "USER_LOGGED_IN");

        return AuthResponse.authenticated(
                accessToken, rawRefresh,
                jwtService.getAccessExpirationMs() / 1000,
                user.getId(), membership.getOrgId(), membership.getRole().name()
        );
    }

    private void storePendingMemberships(UUID userId, List<UserMembership> memberships) {
        // Apenas armazena flag no Redis — memberships são carregadas fresh do DB no verifyTotp
        // (já que o DB é fonte da verdade e o TTL garante expiração)
        redisTemplate.opsForValue().set("pending_2fa:" + userId, "1", 5, TimeUnit.MINUTES);
    }

    private void publishAuthEvent(User user, UserMembership membership, String eventType) {
        try {
            Map<String, Object> payload = Map.of(
                    "userId", user.getId().toString(),
                    "orgId", membership.getOrgId().toString(),
                    "email", user.getEmail(),
                    "role", membership.getRole().name()
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

    private void setRls(UUID orgId) {
        if (orgId != null) {
            em.createNativeQuery("SELECT set_config('app.current_org_id', :oid, true)")
              .setParameter("oid", orgId.toString())
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
