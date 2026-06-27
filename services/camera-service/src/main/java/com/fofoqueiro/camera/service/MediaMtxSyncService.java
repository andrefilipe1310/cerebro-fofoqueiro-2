package com.fofoqueiro.camera.service;

import com.fofoqueiro.camera.domain.entity.Camera;
import com.fofoqueiro.camera.repository.CameraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaMtxSyncService {

    private final CameraRepository cameraRepository;
    private final CameraService cameraService;

    @EventListener(ApplicationReadyEvent.class)
    public void syncAllCamerasToMediaMtx() {
        List<Camera> cameras = cameraRepository.findAllActive();
        log.info("Sincronizando {} câmeras no MediaMTX após startup", cameras.size());
        for (Camera camera : cameras) {
            String path = String.format("tenant_%s/camera_%s/main",
                    camera.getTenantId(), camera.getId());
            cameraService.registerRtspSourceInMediaMtx(path, camera);
        }
    }
}
