package com.fofoqueiro.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fofoqueiro.auth.domain.entity.OutboxEvent;
import com.fofoqueiro.auth.domain.entity.User;
import com.fofoqueiro.auth.domain.entity.UserMembership;
import com.fofoqueiro.auth.dto.request.CreateUserRequest;
import com.fofoqueiro.auth.dto.response.UserResponse;
import com.fofoqueiro.auth.repository.OutboxEventRepository;
import com.fofoqueiro.auth.repository.UserMembershipRepository;
import com.fofoqueiro.auth.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final UserMembershipRepository membershipRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    private void setRls(UUID orgId) {
        if (orgId != null) {
            em.createNativeQuery("SELECT set_config('app.current_org_id', :oid, true)")
              .setParameter("oid", orgId.toString())
              .getSingleResult();
        }
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(UUID orgId) {
        return membershipRepository.findActiveByOrgId(orgId)
                .stream()
                .map(m -> UserResponse.from(m.getUser(), m))
                .toList();
    }

    @Transactional
    public UserResponse createUser(UUID orgId, CreateUserRequest req) {
        // Verifica se email já tem conta global
        User user = userRepository.findByEmail(req.email()).orElseGet(() -> {
            User novo = User.builder()
                    .email(req.email())
                    .passwordHash(passwordEncoder.encode(req.password()))
                    .active(true)
                    .totpEnabled(false)
                    .failedAttempts(0)
                    .build();
            return userRepository.save(novo);
        });

        // Verifica se já tem membership nesta org
        membershipRepository.findActiveByUserIdAndOrgId(user.getId(), orgId).ifPresent(m -> {
            throw new IllegalStateException("Usuário já pertence a esta organização");
        });

        UserMembership membership = UserMembership.builder()
                .user(user)
                .orgId(orgId)
                .role(req.role())
                .active(true)
                .build();
        membershipRepository.save(membership);

        publishUserCreatedEvent(user, orgId);
        return UserResponse.from(user, membership);
    }

    @Transactional
    public void removeMembership(UUID orgId, UUID userId) {
        membershipRepository.findActiveByUserIdAndOrgId(userId, orgId)
                .ifPresent(m -> {
                    m.setActive(false);
                    membershipRepository.save(m);
                });
    }

    private void publishUserCreatedEvent(User user, UUID orgId) {
        try {
            Map<String, Object> payload = Map.of(
                    "userId", user.getId().toString(),
                    "orgId", orgId.toString(),
                    "email", user.getEmail()
            );
            OutboxEvent event = OutboxEvent.builder()
                    .topic("auth.events")
                    .eventType("USER_CREATED")
                    .payload(objectMapper.writeValueAsString(payload))
                    .attempts(0)
                    .build();
            outboxEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Failed to publish user created event: {}", e.getMessage());
        }
    }
}
