package com.tms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class DriverScorecardResponse {
    private UUID driverId;
    private String driverName;
    private String status;
    private long completedTrips;
    private long delayedTrips;
    private int onTimePercent;
    private BigDecimal fuelSpend;
    private int score;
    private String band;
}
