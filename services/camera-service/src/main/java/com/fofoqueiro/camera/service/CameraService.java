package com.fofoqueiro.camera.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fofoqueiro.camera.domain.entity.Camera;
import com.fofoqueiro.camera.domain.entity.OutboxEvent;
import com.fofoqueiro.camera.domain.enums.CameraStatus;
import com.fofoqueiro.camera.dto.request.CreateCameraRequest;
import com.fofoqueiro.camera.dto.request.UpdateCameraRequest;
import com.fofoqueiro.camera.dto.response.CameraResponse;
import com.fofoqueiro.camera.dto.response.StreamUrlResponse;
import com.fofoqueiro.camera.repository.CameraRepository;
import com.fofoqueiro.camera.repository.OutboxEventRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CameraService {

    private final CameraRepository cameraRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @PersistenceContext
    private EntityManager em;

    @Value("${mediamtx.public-url:http://localhost:8889}")
    private String mediamtxPublicUrl;

    @Value("${mediamtx.api-url:http://mediamtx:9997}")
    private String mediamtxApiUrl;

    @Value("${mediamtx.hls-url:http://localhost:8888}")
    private String mediamtxHlsUrl;

    @Transactional(readOnly = true)
    public Page<CameraResponse> list(UUID tenantId, Pageable pageable) {
        setRls(tenantId);
        return cameraRepository.findActiveByTenantId(tenantId, pageable)
                .map(CameraResponse::from);
    }

    @Transactional(readOnly = true)
    public CameraResponse getById(UUID tenantId, UUID cameraId) {
        setRls(tenantId);
        Camera camera = cameraRepository.findByTenantIdAndId(tenantId, cameraId)
                .orElseThrow(() -> new EntityNotFoundException("Câmera não encontrada"));
        return CameraResponse.from(camera);
    }

    @Transactional
    public CameraResponse create(UUID tenantId, CreateCameraRequest req) {
        setRls(tenantId);

        Camera camera = Camera.builder()
                .tenantId(tenantId)
                .locationId(req.locationId())
                .name(req.name())
                .rtspUrlEncrypted(req.rtspUrl() != null ? encryptionService.encrypt(req.rtspUrl()) : null)
                .subStreamUrlEncrypted(req.subStreamUrl() != null ? encryptionService.encrypt(req.subStreamUrl()) : null)
                .lat(req.lat())
                .lng(req.lng())
                .ptzEnabled(req.ptzEnabled())
                .status(CameraStatus.UNKNOWN)
                .build();

        cameraRepository.save(camera);
        publishCameraEvent(camera, "CAMERA_CREATED");
        return CameraResponse.from(camera);
    }

    @Transactional
    public CameraResponse update(UUID tenantId, UUID cameraId, UpdateCameraRequest req) {
        setRls(tenantId);
        Camera camera = cameraRepository.findByTenantIdAndId(tenantId, cameraId)
                .orElseThrow(() -> new EntityNotFoundException("Câmera não encontrada"));

        if (req.name() != null) camera.setName(req.name());
        if (req.locationId() != null) camera.setLocationId(req.locationId());
        if (req.lat() != null) camera.setLat(req.lat());
        if (req.lng() != null) camera.setLng(req.lng());
        if (req.rtspUrl() != null) {
            camera.setRtspUrlEncrypted(encryptionService.encrypt(req.rtspUrl()));
            camera.setStreamToken(null);
        }
        if (req.ptzEnabled() != null) camera.setPtzEnabled(req.ptzEnabled());

        cameraRepository.save(camera);
        return CameraResponse.from(camera);
    }

    @Transactional
    public void delete(UUID tenantId, UUID cameraId) {
        setRls(tenantId);
        Camera camera = cameraRepository.findByTenantIdAndId(tenantId, cameraId)
                .orElseThrow(() -> new EntityNotFoundException("Câmera não encontrada"));
        camera.setStatus(CameraStatus.DELETED);
        cameraRepository.save(camera);
        publishCameraEvent(camera, "CAMERA_DELETED");
    }

    @Transactional
    public StreamUrlResponse getStreamUrl(UUID tenantId, UUID cameraId) {
        setRls(tenantId);
        Camera camera = cameraRepository.findByTenantIdAndId(tenantId, cameraId)
                .orElseThrow(() -> new EntityNotFoundException("Câmera não encontrada"));

        if (camera.getStreamToken() == null || camera.getStreamTokenExpiresAt() == null
                || camera.getStreamTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            String token = UUID.randomUUID().toString().replace("-", "");
            camera.setStreamToken(token);
            camera.setStreamTokenExpiresAt(OffsetDateTime.now().plusHours(1));
            cameraRepository.save(camera);
        }

        String path = String.format("tenant_%s/camera_%s/main", tenantId, cameraId);
        String token = camera.getStreamToken();

        registerRtspSourceInMediaMtx(path, camera);

        return new StreamUrlResponse(
                String.format("%s/%s/whep?token=%s", mediamtxPublicUrl, path, token),
                String.format("%s/%s/index.m3u8?token=%s", mediamtxHlsUrl, path, token),
                null,
                camera.getStreamTokenExpiresAt()
        );
    }

    private void registerRtspSourceInMediaMtx(String path, Camera camera) {
        if (camera.getRtspUrlEncrypted() == null) return;
        try {
            String rtspUrl = encryptionService.decrypt(camera.getRtspUrlEncrypted());
            String url = String.format("%s/v3/config/paths/add/%s", mediamtxApiUrl, path);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of("source", rtspUrl, "sourceOnDemand", true);
            restTemplate.put(url, new HttpEntity<>(body, headers));
            log.debug("MediaMTX path registered: {}", path);
        } catch (Exception e) {
            log.warn("Falha ao registrar path no MediaMTX ({}): {}", path, e.getMessage());
        }
    }

    private void publishCameraEvent(Camera camera, String eventType) {
        try {
            Map<String, Object> payload = Map.of(
                    "cameraId", camera.getId().toString(),
                    "tenantId", camera.getTenantId().toString(),
                    "name", camera.getName(),
                    "status", camera.getStatus().name()
            );
            OutboxEvent event = OutboxEvent.builder()
                    .topic("camera.events")
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .attempts(0)
                    .build();
            outboxEventRepository.save(event);
        } catch (Exception e) {
            log.warn("Falha ao publicar evento camera: {}", e.getMessage());
        }
    }

    private void setRls(UUID tenantId) {
        if (tenantId != null) {
            em.createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, true)")
              .setParameter("tid", tenantId.toString())
              .getSingleResult();
        }
    }
}
