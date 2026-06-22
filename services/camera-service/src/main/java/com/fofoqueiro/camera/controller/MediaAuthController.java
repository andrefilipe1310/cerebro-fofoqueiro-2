package com.fofoqueiro.camera.controller;

import com.fofoqueiro.camera.repository.CameraRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/internal/media")
@RequiredArgsConstructor
public class MediaAuthController {

    private final CameraRepository cameraRepository;

    @PostMapping("/auth")
    public ResponseEntity<Void> authenticate(@RequestBody Map<String, Object> body) {
        try {
            String path = (String) body.get("path");
            String token = extractToken(body);

            if (path == null || token == null) {
                return ResponseEntity.status(403).build();
            }

            // Path format: org_{orgId}/camera_{cameraId}/main
            String[] parts = path.split("/");
            if (parts.length < 2) return ResponseEntity.status(403).build();

            String cameraIdStr = parts[1].replace("camera_", "");
            java.util.UUID cameraId = java.util.UUID.fromString(cameraIdStr);

            boolean valid = cameraRepository.findById(cameraId)
                    .map(cam -> token.equals(cam.getStreamToken())
                            && cam.getStreamTokenExpiresAt() != null
                            && cam.getStreamTokenExpiresAt().isAfter(OffsetDateTime.now()))
                    .orElse(false);

            return valid ? ResponseEntity.ok().build() : ResponseEntity.status(403).build();

        } catch (Exception e) {
            log.warn("MediaMTX auth error: {}", e.getMessage());
            return ResponseEntity.status(403).build();
        }
    }

    private String extractToken(Map<String, Object> body) {
        Object query = body.get("query");
        if (query instanceof String q) {
            for (String param : q.split("&")) {
                if (param.startsWith("token=")) return param.substring(6);
            }
        }
        return null;
    }
}
