package com.tms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private long totalTrips;
    private long activeTrips;
    private long completedTrips;
    private long availableVehicles;
    private long totalVehicles;
    private long totalDrivers;
    private long activeDrivers;
    private long totalLrs;
    private long todayBookings;
    private long totalBookings;
    private List<TripResponse> recentTrips;
}
