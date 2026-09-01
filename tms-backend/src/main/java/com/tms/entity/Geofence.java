package com.tms.entity;

import com.tms.enums.GeofenceType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "geofences", indexes = {
        @Index(name = "idx_geofence_active", columnList = "active"),
        @Index(name = "idx_geofence_tenant", columnList = "tenant_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Geofence extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Double radiusMeters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private GeofenceType type = GeofenceType.CUSTOM;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "tenant_id")
    private UUID tenantId;
}

