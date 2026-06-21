package com.fofoqueiro.chms.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(schema = "health", name = "camera_health_state")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CameraHealthState {

    @Id
    @Column(name = "camera_id")
    private UUID cameraId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    @Column(nullable = false)
    private String status;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;

    @Column(name = "recording_confidence_score", precision = 5, scale = 2)
    private BigDecimal recordingConfidenceScore;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
