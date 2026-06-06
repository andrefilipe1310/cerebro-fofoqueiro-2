package com.fofoqueiro.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fofoqueiro.auth.domain.entity.OutboxEvent;
import com.fofoqueiro.auth.domain.entity.User;
import com.fofoqueiro.auth.dto.request.CreateUserRequest;
import com.fofoqueiro.auth.dto.response.UserResponse;
import com.fofoqueiro.auth.repository.OutboxEventRepository;
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
    private final OutboxEventRepository outboxEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    private void setRls(UUID tenantId) {
        if (tenantId != null) {
            em.createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, true)")
              .setParameter("tid", tenantId.toString())
              .getSingleResult();
        }
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(UUID tenantId) {
        setRls(tenantId);
        return userRepository.findByTenantId(tenantId)
                .stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse createUser(UUID tenantId, CreateUserRequest req) {
        setRls(tenantId);
        User user = User.builder()
                .tenantId(tenantId)
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(req.role())
                .active(true)
                .totpEnabled(false)
                .failedAttempts(0)
                .build();
        userRepository.save(user);
        publishUserCreatedEvent(user, req.email());
        return UserResponse.from(user);
    }

    @Transactional
    public void deactivateUser(UUID tenantId, UUID userId) {
        setRls(tenantId);
        userRepository.findById(userId)
                .filter(u -> u.getTenantId().equals(tenantId))
                .ifPresent(u -> {
                    u.setActive(false);
                    userRepository.save(u);
                });
    }

    private void publishUserCreatedEvent(User user, String email) {
        try {
            Map<String, Object> payload = Map.of(
                    "userId", user.getId().toString(),
                    "tenantId", user.getTenantId().toString(),
                    "email", email,
                    "eventType", "USER_CREATED"
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
