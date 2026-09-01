package com.tms.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "proof_of_delivery")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class ProofOfDelivery extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "lr_id")
    private UUID lrId;

    @Column(nullable = false, length = 150)
    private String receiverName;

    @Column(length = 20)
    private String receiverPhone;

    @Column(length = 6)
    private String otp;

    @Column(length = 500)
    private String notes;

    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private LocalDateTime deliveredAt;

    @Column(name = "tenant_id")
    private UUID tenantId;
}
