package com.fofoqueiro.chms.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(schema = "health", name = "health_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HealthEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "camera_id", nullable = false)
    private UUID cameraId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String severity;

    @Column(name = "detected_at", insertable = false, updatable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(columnDefinition = "jsonb")
    private String metadata;
}
