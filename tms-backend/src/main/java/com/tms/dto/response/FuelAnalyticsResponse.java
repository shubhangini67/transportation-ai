package com.tms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class FuelAnalyticsResponse {
    private BigDecimal totalFuelSpend;
    private BigDecimal averageCostPerKm;
    private Double totalDistanceKm;
    private long fuelTransactionCount;
    private List<VehicleFuelBreakdown> vehicleBreakdowns;
    private List<FuelTrendPoint> monthlyTrend;
}

