package com.tms.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FleetSnapshotRequest {
    private int totalVehicles;
    private int availableVehicles;
    private int busyVehicles;
    private int maintenanceVehicles;
    private int totalDrivers;
    private int activeDrivers;
    private int inactiveDrivers;
    private int totalTrips;
    private int plannedTrips;
    private int inProgressTrips;
    private int completedTrips;
    private BigDecimal totalExpenses;
    private int totalBookings;
}
