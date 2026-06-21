package com.fofoqueiro.camera.domain.entity;

import com.fofoqueiro.camera.domain.enums.CameraStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(schema = "cameras", name = "cameras")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Camera {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(nullable = false)
    private String name;

    @Column(name = "rtsp_url_encrypted")
    private String rtspUrlEncrypted;

    @Column(name = "sub_stream_url_encrypted")
    private String subStreamUrlEncrypted;

    @Column(precision = 10, scale = 8)
    private BigDecimal lat;

    @Column(precision = 11, scale = 8)
    private BigDecimal lng;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CameraStatus status;

    @Column(name = "ptz_enabled", nullable = false)
    private boolean ptzEnabled;

    @Column(name = "stream_token")
    private String streamToken;

    @Column(name = "stream_token_expires_at")
    private OffsetDateTime streamTokenExpiresAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
