package com.fofoqueiro.recording.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fofoqueiro.recording.domain.entity.Recording;
import com.fofoqueiro.recording.dto.response.DownloadUrlResponse;
import com.fofoqueiro.recording.dto.response.RecordingResponse;
import com.fofoqueiro.recording.dto.response.TimelineResponse;
import com.fofoqueiro.recording.repository.RecordingRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordingService {

    private final RecordingRepository recordingRepository;
    private final R2StorageService r2StorageService;
    private final EntityManager em;
    private final ObjectMapper objectMapper;

    private void setRls(UUID orgId) {
        if (orgId != null) {
            em.createNativeQuery("SELECT set_config('app.current_org_id', :tid, true)")
              .setParameter("tid", orgId.toString())
              .getSingleResult();
        }
    }

    @Transactional(readOnly = true)
    public Page<RecordingResponse> findByOrg(UUID orgId, Pageable pageable) {
        setRls(orgId);
        return recordingRepository.findByOrgId(orgId, pageable).map(RecordingResponse::from);
    }

    @Transactional(readOnly = true)
    public TimelineResponse findTimeline(UUID cameraId, UUID orgId,
                                         OffsetDateTime from, OffsetDateTime to) {
        setRls(orgId);
        List<Recording> segments = recordingRepository.findTimeline(cameraId, orgId, from, to);
        List<TimelineResponse.GapResponse> gaps = new ArrayList<>();

        OffsetDateTime cursor = from;
        for (Recording seg : segments) {
            if (cursor.isBefore(seg.getStartedAt())) {
                gaps.add(new TimelineResponse.GapResponse(
                        cursor.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        seg.getStartedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
            }
            if (seg.getEndedAt() != null) cursor = seg.getEndedAt();
        }
        if (cursor.isBefore(to)) {
            gaps.add(new TimelineResponse.GapResponse(
                    cursor.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    to.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        }

        return new TimelineResponse(segments.stream().map(RecordingResponse::from).toList(), gaps);
    }

    @Transactional(readOnly = true)
    public DownloadUrlResponse findDownloadUrl(UUID recordingId, UUID orgId) {
        setRls(orgId);
        Recording recording = recordingRepository.findById(recordingId)
                .filter(r -> r.getOrgId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("Recording not found"));
        String url = r2StorageService.generatePresignedDownloadUrl(recording.getR2Key(), Duration.ofHours(1));
        return new DownloadUrlResponse(url, OffsetDateTime.now().plusHours(1));
    }

    @Transactional
    public void registerRecording(String eventPayload) {
        try {
            JsonNode node = objectMapper.readTree(eventPayload);
            JsonNode orgIdNode = node.has("orgId") ? node.get("orgId") : node.get("tenantId");
            UUID orgId = UUID.fromString(orgIdNode.asText());
            setRls(orgId);
            Recording recording = Recording.builder()
                    .orgId(orgId)
                    .cameraId(UUID.fromString(node.get("cameraId").asText()))
                    .r2Key(node.get("r2Key").asText())
                    .startedAt(OffsetDateTime.parse(node.get("startedAt").asText()))
                    .endedAt(node.has("endedAt") ? OffsetDateTime.parse(node.get("endedAt").asText()) : null)
                    .durationSeconds(node.has("durationSeconds") ? node.get("durationSeconds").asInt() : null)
                    .sizeBytes(node.has("sizeBytes") ? node.get("sizeBytes").asLong() : null)
                    .hasMotion(node.has("hasMotion") && node.get("hasMotion").asBoolean())
                    .build();
            recordingRepository.save(recording);
        } catch (Exception e) {
            log.error("Failed to register recording from event: {}", e.getMessage(), e);
        }
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredRecordings() {
        recordingRepository.findAll().stream()
                .map(r -> r.getOrgId())
                .distinct()
                .forEach(orgId -> {
                    setRls(orgId);
                    OffsetDateTime cutoff = OffsetDateTime.now().minusDays(30);
                    List<Recording> expired = recordingRepository.findExpiredRecordings(orgId, cutoff);
                    for (Recording r : expired) {
                        r2StorageService.deleteObject(r.getR2Key());
                        recordingRepository.delete(r);
                    }
                    log.info("Cleaned up {} expired recordings for org {}", expired.size(), orgId);
                });
    }

    @Transactional
    public void delete(UUID recordingId, UUID orgId) {
        setRls(orgId);
        Recording recording = recordingRepository.findById(recordingId)
                .filter(r -> r.getOrgId().equals(orgId))
                .orElseThrow(() -> new RuntimeException("Recording not found"));
        r2StorageService.deleteObject(recording.getR2Key());
        recordingRepository.delete(recording);
    }
}
