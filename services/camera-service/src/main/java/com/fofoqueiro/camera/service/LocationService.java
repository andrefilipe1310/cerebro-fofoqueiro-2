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
    public List<LocationResponse> list(UUID orgId) {
        setRls(orgId);
        return locationRepository.findByOrgId(orgId).stream().map(LocationResponse::from).toList();
    }

    @Transactional
    public LocationResponse create(UUID orgId, CreateLocationRequest req) {
        setRls(orgId);
        Location location = Location.builder()
                .orgId(orgId)
                .name(req.name())
                .address(req.address())
                .lat(req.lat())
                .lng(req.lng())
                .build();
        locationRepository.save(location);
        return LocationResponse.from(location);
    }

    private void setRls(UUID orgId) {
        if (orgId != null) {
            em.createNativeQuery("SELECT set_config('app.current_org_id', :oid, true)")
              .setParameter("oid", orgId.toString())
              .getSingleResult();
        }
    }
}
