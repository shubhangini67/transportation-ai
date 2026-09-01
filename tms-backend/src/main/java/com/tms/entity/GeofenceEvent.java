package com.tms.entity;

import com.tms.enums.GeofenceEventType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "geofence_events", indexes = {
        @Index(name = "idx_gfe_geofence_time", columnList = "geofence_id,eventTime"),
        @Index(name = "idx_gfe_vehicle_time", columnList = "vehicle_id,eventTime")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class GeofenceEvent extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "geofence_id", nullable = false)
    private UUID geofenceId;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "trip_id")
    private UUID tripId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GeofenceEventType eventType;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "tenant_id")
    private UUID tenantId;
}

