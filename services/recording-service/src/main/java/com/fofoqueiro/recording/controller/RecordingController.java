package com.fofoqueiro.recording.controller;

import com.fofoqueiro.recording.dto.response.DownloadUrlResponse;
import com.fofoqueiro.recording.dto.response.RecordingResponse;
import com.fofoqueiro.recording.dto.response.TimelineResponse;
import com.fofoqueiro.recording.security.TenantContext;
import com.fofoqueiro.recording.service.RecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recordings")
@RequiredArgsConstructor
public class RecordingController {

    private final RecordingService recordingService;

    @GetMapping
    public ResponseEntity<Page<RecordingResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(recordingService.findByTenant(TenantContext.get(), pageable));
    }

    @GetMapping("/timeline")
    public ResponseEntity<TimelineResponse> timeline(
            @RequestParam UUID cameraId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return ResponseEntity.ok(recordingService.findTimeline(cameraId, TenantContext.get(), from, to));
    }

    @GetMapping("/{id}/download-url")
    public ResponseEntity<DownloadUrlResponse> downloadUrl(@PathVariable UUID id) {
        return ResponseEntity.ok(recordingService.findDownloadUrl(id, TenantContext.get()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        recordingService.delete(id, TenantContext.get());
        return ResponseEntity.noContent().build();
    }
}
