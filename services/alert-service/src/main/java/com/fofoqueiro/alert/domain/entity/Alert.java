package com.fofoqueiro.alert.domain.entity;

import com.fofoqueiro.alert.domain.enums.AlertSeverity;
import com.fofoqueiro.alert.domain.enums.AlertStatus;
import com.fofoqueiro.alert.domain.enums.AlertType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(schema = "alerts", name = "alerts")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "camera_id", nullable = false)
    private UUID cameraId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType type;

    @Column(nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AlertSeverity severity = AlertSeverity.WARNING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AlertStatus status = AlertStatus.TRIGGERED;

    @CreationTimestamp
    @Column(name = "triggered_at", nullable = false, updatable = false)
    private OffsetDateTime triggeredAt;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "kafka_event_id", unique = true)
    private String kafkaEventId;
}
