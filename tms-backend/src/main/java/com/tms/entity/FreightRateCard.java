package com.tms.entity;

import com.tms.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "freight_rate_cards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class FreightRateCard extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String origin;

    @Column(nullable = false, length = 120)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @Column(name = "rate_per_km", nullable = false, precision = 12, scale = 2)
    private BigDecimal ratePerKm;

    @Column(name = "min_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal minCharge;

    @Column(name = "gst_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gstPercent = new BigDecimal("18.00");

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "tenant_id")
    private UUID tenantId;
}
