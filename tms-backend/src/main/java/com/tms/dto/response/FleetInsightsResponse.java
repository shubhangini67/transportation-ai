package com.tms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FleetInsightsResponse {
    private String engine;
    private double vehicleUtilizationPercent;
    private double maintenanceLoadPercent;
    private double driverAvailabilityPercent;
    private double expensePerTrip;
    private int fleetHealthScore;
    private String riskLevel;
    @Builder.Default
    private List<String> alerts = new ArrayList<>();
    @Builder.Default
    private List<String> recommendations = new ArrayList<>();
}
