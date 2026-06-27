package com.fofoqueiro.tenant.domain.entity;

import com.fofoqueiro.tenant.domain.enums.TenantPlan;
import com.fofoqueiro.tenant.domain.enums.TenantStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(schema = "organizations", name = "organizations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantPlan plan;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "css_override")
    private String cssOverride;

    @Column(name = "max_cameras", nullable = false)
    private int maxCameras;

    @Column(name = "max_users", nullable = false)
    private int maxUsers;

    @Column(name = "retention_days", nullable = false)
    private int retentionDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}
