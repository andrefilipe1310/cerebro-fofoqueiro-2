package com.fofoqueiro.tenant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fofoqueiro.tenant.domain.entity.OutboxEvent;
import com.fofoqueiro.tenant.domain.entity.Tenant;
import com.fofoqueiro.tenant.domain.enums.TenantPlan;
import com.fofoqueiro.tenant.domain.enums.TenantStatus;
import com.fofoqueiro.tenant.dto.request.CreateTenantRequest;
import com.fofoqueiro.tenant.dto.request.UpdateTenantRequest;
import com.fofoqueiro.tenant.dto.response.TenantResponse;
import com.fofoqueiro.tenant.repository.OutboxEventRepository;
import com.fofoqueiro.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY = "org:";
    private static final long CACHE_TTL_MINUTES = 5;

    @Transactional(readOnly = true)
    public TenantResponse findById(UUID tenantId) {
        String cached = redisTemplate.opsForValue().get(CACHE_KEY + tenantId);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, TenantResponse.class);
            } catch (Exception e) {
                log.warn("Cache deserialize error for tenant {}", tenantId);
            }
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Tenant não encontrado"));

        TenantResponse response = TenantResponse.from(tenant);
        cacheResponse(CACHE_KEY + tenantId, response);
        return response;
    }

    @Transactional(readOnly = true)
    public TenantResponse findByDomain(String domain) {
        String key = "org:domain:" + domain;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, TenantResponse.class);
            } catch (Exception e) {
                log.warn("Cache deserialize error for domain {}", domain);
            }
        }

        Tenant tenant = tenantRepository.findByDomain(domain)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Tenant não encontrado para domínio: " + domain));

        TenantResponse response = TenantResponse.from(tenant);
        cacheResponse(key, response);
        return response;
    }

    @Transactional(readOnly = true)
    public TenantResponse findBySlug(String slug) {
        String key = "org:slug:" + slug;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, TenantResponse.class);
            } catch (Exception e) {
                log.warn("Cache deserialize error for slug {}", slug);
            }
        }

        Tenant tenant = tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Tenant não encontrado para slug: " + slug));

        TenantResponse response = TenantResponse.from(tenant);
        cacheResponse(key, response);
        return response;
    }

    @Transactional
    public TenantResponse update(UUID tenantId, UpdateTenantRequest req) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Tenant não encontrado"));

        if (req.name() != null) tenant.setName(req.name());
        if (req.domain() != null) tenant.setDomain(req.domain());
        if (req.logoUrl() != null) tenant.setLogoUrl(req.logoUrl());
        if (req.cssOverride() != null) tenant.setCssOverride(req.cssOverride());

        tenantRepository.save(tenant);
        invalidateCache(tenantId);
        return TenantResponse.from(tenant);
    }

    @Transactional
    public TenantResponse create(CreateTenantRequest req) {
        Tenant tenant = Tenant.builder()
                .slug(req.slug())
                .name(req.name())
                .domain(req.domain())
                .plan(req.plan())
                .maxCameras(req.maxCameras() > 0 ? req.maxCameras() : defaultMaxCameras(req.plan()))
                .maxUsers(req.maxUsers() > 0 ? req.maxUsers() : 5)
                .retentionDays(req.retentionDays() > 0 ? req.retentionDays() : 30)
                .status(TenantStatus.ACTIVE)
                .build();

        tenantRepository.save(tenant);

        publishTenantEvent(tenant, "TENANT_CREATED");
        return TenantResponse.from(tenant);
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> listAll() {
        return tenantRepository.findAll().stream().map(TenantResponse::from).toList();
    }

    private void invalidateCache(UUID tenantId) {
        redisTemplate.delete(CACHE_KEY + tenantId);
    }

    private void cacheResponse(String key, TenantResponse response) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(response),
                    CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to cache tenant response: {}", e.getMessage());
        }
    }

    private void publishTenantEvent(Tenant tenant, String eventType) {
        try {
            Map<String, Object> payload = Map.of("orgId", tenant.getId().toString(), "slug", tenant.getSlug());
            OutboxEvent event = OutboxEvent.builder()
                    .topic("organization.events")
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .attempts(0)
                    .build();
            outboxEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Falha ao publicar evento tenant: {}", e.getMessage());
        }
    }

    private int defaultMaxCameras(TenantPlan plan) {
        return switch (plan) {
            case FREE -> 2;
            case STARTER -> 10;
            case PRO -> 50;
            case ENTERPRISE -> 500;
        };
    }
}
