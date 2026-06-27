package com.fofoqueiro.recording.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(schema = "recordings", name = "recordings")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Recording {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "camera_id", nullable = false)
    private UUID cameraId;

    @Column(name = "r2_key", nullable = false)
    private String r2Key;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "has_motion", nullable = false)
    @Builder.Default
    private boolean hasMotion = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
