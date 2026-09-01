package com.tms.dto.response;

import com.tms.enums.VehicleType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class FreightRateCardResponse {
    private UUID id;
    private String origin;
    private String destination;
    private VehicleType vehicleType;
    private BigDecimal ratePerKm;
    private BigDecimal minCharge;
    private BigDecimal gstPercent;
    private Boolean active;
    private BigDecimal quotedSubtotal;
    private BigDecimal quotedGst;
    private BigDecimal quotedTotal;
    private Double distanceKm;
}
