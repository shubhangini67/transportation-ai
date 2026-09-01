package com.tms.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "routes")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@SuperBuilder
public class Route extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String origin;

    @Column(nullable = false, length = 200)
    private String destination;

    @Column(nullable = false)
    private Double distance; // in kilometers

    @Column(nullable = false)
    private Integer estimatedTimeMinutes;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "tenant_id")
    private UUID tenantId;
}
