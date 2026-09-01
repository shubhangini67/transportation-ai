package com.tms.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicle_location_history", indexes = {
        @Index(name = "idx_vlh_vehicle_time", columnList = "vehicle_id,recordedAt"),
        @Index(name = "idx_vlh_trip", columnList = "trip_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class VehicleLocationHistory extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column(name = "trip_id")
    private UUID tripId;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    private Double speed;

    private Double heading;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "tenant_id")
    private UUID tenantId;
}

