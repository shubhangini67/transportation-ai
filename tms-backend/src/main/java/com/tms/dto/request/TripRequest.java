package com.tms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class TripRequest {
    @NotNull(message = "Vehicle ID is required")
    private UUID vehicleId;

    @NotNull(message = "Driver ID is required")
    private UUID driverId;

    private Long routeId;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String notes;
    private List<UUID> lrIds;
}
