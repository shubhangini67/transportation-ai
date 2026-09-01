package com.tms.entity;

import com.tms.enums.VehicleStatus;
import com.tms.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "vehicles", indexes = {
        @Index(name = "idx_vehicle_number", columnList = "vehicleNumber", unique = true),
        @Index(name = "idx_vehicle_status", columnList = "status")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class Vehicle extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleType type;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @Column(length = 200)
    private String currentLocation;

    @Column(length = 100)
    private String make;

    @Column(length = 100)
    private String model;

    @Column(name = "manufacture_year")
    private Integer year;

    private Double latitude;
    private Double longitude;

    @Column(name = "odometer_km")
    private Integer odometerKm;

    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;

    @Column(name = "next_service_due_km")
    private Integer nextServiceDueKm;

    @Column(name = "tenant_id")
    private UUID tenantId;
}
