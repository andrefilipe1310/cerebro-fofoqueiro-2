package com.fofoqueiro.camera.service;

import com.fofoqueiro.camera.domain.entity.Location;
import com.fofoqueiro.camera.dto.request.CreateLocationRequest;
import com.fofoqueiro.camera.dto.response.LocationResponse;
import com.fofoqueiro.camera.repository.LocationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<LocationResponse> list(UUID tenantId) {
        setRls(tenantId);
        return locationRepository.findByTenantId(tenantId).stream().map(LocationResponse::from).toList();
    }

    @Transactional
    public LocationResponse create(UUID tenantId, CreateLocationRequest req) {
        setRls(tenantId);
        Location location = Location.builder()
                .tenantId(tenantId)
                .name(req.name())
                .address(req.address())
                .lat(req.lat())
                .lng(req.lng())
                .build();
        locationRepository.save(location);
        return LocationResponse.from(location);
    }

    private void setRls(UUID tenantId) {
        if (tenantId != null) {
            em.createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, true)")
              .setParameter("tid", tenantId.toString())
              .getSingleResult();
        }
    }
}
