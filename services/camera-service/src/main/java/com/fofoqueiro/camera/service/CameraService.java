package com.fofoqueiro.camera.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fofoqueiro.camera.domain.entity.Camera;
import com.fofoqueiro.camera.domain.entity.OutboxEvent;
import com.fofoqueiro.camera.domain.enums.CameraStatus;
import com.fofoqueiro.camera.dto.request.CreateCameraRequest;
import com.fofoqueiro.camera.dto.request.UpdateCameraRequest;
import com.fofoqueiro.camera.dto.response.CameraResponse;
import com.fofoqueiro.camera.dto.response.StreamUrlResponse;
import com.fofoqueiro.camera.dto.response.TestConnectionResponse;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
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
    public Page<CameraResponse> list(UUID orgId, Pageable pageable) {
        setRls(orgId);
        return cameraRepository.findActiveByOrgId(orgId, pageable)
                .map(CameraResponse::from);
    }

    @Transactional(readOnly = true)
    public CameraResponse getById(UUID orgId, UUID cameraId) {
        setRls(orgId);
        Camera camera = cameraRepository.findByOrgIdAndId(orgId, cameraId)
                .orElseThrow(() -> new EntityNotFoundException("Câmera não encontrada"));
        return CameraResponse.from(camera);
    }

    @Transactional
    public CameraResponse create(UUID orgId, CreateCameraRequest req) {
        setRls(orgId);

        Camera camera = Camera.builder()
                .orgId(orgId)
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

        String path = String.format("org_%s/camera_%s/main", orgId, camera.getId());
        registerRtspSourceInMediaMtx(path, camera);

        return CameraResponse.from(camera);
    }

    @Transactional
    public CameraResponse update(UUID orgId, UUID cameraId, UpdateCameraRequest req) {
        setRls(orgId);
        Camera camera = cameraRepository.findByOrgIdAndId(orgId, cameraId)
                .orElseThrow(() -> new EntityNotFoundException("Câmera não encontrada"));

        if (req.name() != null) camera.setName(req.name());
        if (req.locationId() != null) camera.setLocationId(req.locationId());
        if (req.lat() != null) camera.setLat(req.lat());
        if (req.lng() != null) camera.setLng(req.lng());
        boolean rtspChanged = false;
        if (req.rtspUrl() != null) {
            camera.setRtspUrlEncrypted(encryptionService.encrypt(req.rtspUrl()));
            camera.setStreamToken(null);
            rtspChanged = true;
        }
        if (req.ptzEnabled() != null) camera.setPtzEnabled(req.ptzEnabled());

        cameraRepository.save(camera);

        if (rtspChanged) {
            String path = String.format("org_%s/camera_%s/main", orgId, cameraId);
            registerRtspSourceInMediaMtx(path, camera);
        }

        return CameraResponse.from(camera);
    }

    @Transactional
    public void delete(UUID orgId, UUID cameraId) {
        setRls(orgId);
        Camera camera = cameraRepository.findByOrgIdAndId(orgId, cameraId)
                .orElseThrow(() -> new EntityNotFoundException("Câmera não encontrada"));
        camera.setStatus(CameraStatus.DELETED);
        cameraRepository.save(camera);
        publishCameraEvent(camera, "CAMERA_DELETED");
    }

    @Transactional
    public StreamUrlResponse getStreamUrl(UUID orgId, UUID cameraId) {
        setRls(orgId);
        Camera camera = cameraRepository.findByOrgIdAndId(orgId, cameraId)
                .orElseThrow(() -> new EntityNotFoundException("Câmera não encontrada"));

        if (camera.getStreamToken() == null || camera.getStreamTokenExpiresAt() == null
                || camera.getStreamTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            String token = UUID.randomUUID().toString().replace("-", "");
            camera.setStreamToken(token);
            camera.setStreamTokenExpiresAt(OffsetDateTime.now().plusHours(1));
            cameraRepository.save(camera);
        }

        String path = String.format("org_%s/camera_%s/main", orgId, cameraId);
        String token = camera.getStreamToken();

        registerRtspSourceInMediaMtx(path, camera);

        return new StreamUrlResponse(
                String.format("%s/%s/whep?token=%s", mediamtxPublicUrl, path, token),
                String.format("%s/%s/index.m3u8?token=%s", mediamtxHlsUrl, path, token),
                null,
                camera.getStreamTokenExpiresAt()
        );
    }

    public TestConnectionResponse testConnection(String rtspUrl) {
        try {
            URI uri = URI.create(rtspUrl);
            int port = uri.getPort() == -1 ? 554 : uri.getPort();
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(uri.getHost(), port), 4000);
            }
            return new TestConnectionResponse(true, null);
        } catch (Exception e) {
            return new TestConnectionResponse(false, e.getMessage());
        }
    }

    public void registerRtspSourceInMediaMtx(String path, Camera camera) {
        if (camera.getRtspUrlEncrypted() == null) return;
        try {
            String rtspUrl = encryptionService.decrypt(camera.getRtspUrlEncrypted());
            String encodedPath = path.replace("/", "%2F");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of("source", rtspUrl, "sourceOnDemand", false);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // Remove path if it already exists (idempotent registration)
            try {
                URI deleteUri = URI.create(String.format("%s/v3/config/paths/delete/%s", mediamtxApiUrl, encodedPath));
                restTemplate.exchange(deleteUri, HttpMethod.DELETE, null, String.class);
            } catch (Exception ignored) {}

            URI addUri = URI.create(String.format("%s/v3/config/paths/add/%s", mediamtxApiUrl, encodedPath));
            restTemplate.exchange(addUri, HttpMethod.POST, entity, String.class);
            log.info("MediaMTX path registered: {}", path);
        } catch (Exception e) {
            log.warn("Falha ao registrar path no MediaMTX ({}): {}", path, e.getMessage());
        }
    }

    private void publishCameraEvent(Camera camera, String eventType) {
        try {
            Map<String, Object> payload = Map.of(
                    "cameraId", camera.getId().toString(),
                    "orgId", camera.getOrgId().toString(),
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

    private void setRls(UUID orgId) {
        if (orgId != null) {
            em.createNativeQuery("SELECT set_config('app.current_org_id', :tid, true)")
              .setParameter("tid", orgId.toString())
              .getSingleResult();
        }
    }
}
