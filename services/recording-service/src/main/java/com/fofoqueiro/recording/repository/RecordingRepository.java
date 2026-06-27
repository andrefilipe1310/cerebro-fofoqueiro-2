package com.fofoqueiro.recording.repository;

import com.fofoqueiro.recording.domain.entity.Recording;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecordingRepository extends JpaRepository<Recording, UUID> {

    @Query(value = "SELECT * FROM recordings.recordings WHERE org_id = :orgId ORDER BY started_at DESC",
           countQuery = "SELECT count(*) FROM recordings.recordings WHERE org_id = :orgId",
           nativeQuery = true)
    Page<Recording> findByOrgId(UUID orgId, Pageable pageable);

    @Query(value = "SELECT * FROM recordings.recordings WHERE camera_id = :cameraId AND org_id = :orgId AND started_at >= :from AND (ended_at IS NULL OR ended_at <= :to) ORDER BY started_at",
           nativeQuery = true)
    List<Recording> findTimeline(UUID cameraId, UUID orgId, OffsetDateTime from, OffsetDateTime to);

    @Query(value = "SELECT * FROM recordings.recordings WHERE org_id = :orgId AND ended_at IS NOT NULL AND ended_at < :cutoff",
           nativeQuery = true)
    List<Recording> findExpiredRecordings(UUID orgId, OffsetDateTime cutoff);
}
