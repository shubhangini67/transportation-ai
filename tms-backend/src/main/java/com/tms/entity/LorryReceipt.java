package com.tms.entity;

import com.tms.enums.LrStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "lorry_receipts", indexes = {
        @Index(name = "idx_lr_number", columnList = "lrNumber", unique = true),
        @Index(name = "idx_lr_status", columnList = "status"),
        @Index(name = "idx_lr_origin", columnList = "origin"),
        @Index(name = "idx_lr_destination", columnList = "destination")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class LorryReceipt extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String lrNumber;

    @Column(nullable = false, length = 200)
    private String consignor;

    @Column(nullable = false, length = 200)
    private String consignee;

    @Column(nullable = false, length = 200)
    private String origin;

    @Column(nullable = false, length = 200)
    private String destination;

    @Column(length = 300)
    private String material;

    @Column(nullable = false)
    private Double weight;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LrStatus status = LrStatus.CREATED;

    @Column(name = "tenant_id")
    private UUID tenantId;
}
