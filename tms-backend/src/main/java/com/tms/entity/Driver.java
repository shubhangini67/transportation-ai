package com.tms.entity;

import com.tms.enums.DriverStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "drivers", indexes = {
        @Index(name = "idx_driver_license", columnList = "licenseNumber", unique = true)
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class Driver extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, unique = true, length = 30)
    private String licenseNumber;

    @Column(length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DriverStatus status = DriverStatus.ACTIVE;

    @Column(name = "tenant_id")
    private UUID tenantId;
}
