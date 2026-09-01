package com.tms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class VehicleFuelBreakdown {
    private UUID vehicleId;
    private String vehicleNumber;
    private BigDecimal totalFuelCost;
    private Double totalDistanceKm;
    private BigDecimal costPerKm;
    private long tripCount;
    private long fuelExpenseCount;
}

