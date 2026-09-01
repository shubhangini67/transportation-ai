package com.tms.dto.response;

import com.tms.enums.TripStatus;
import com.tms.enums.VehicleType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TripResponse {
    private UUID id;
    private UUID vehicleId;
    private String vehicleNumber;
    private VehicleType vehicleType;
    private UUID driverId;
    private String driverName;
    private Long routeId;
    private String routeOrigin;
    private String routeDestination;
    private Double routeDistance;
    private Integer routeEstimatedMinutes;
    private TripStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String notes;
    private String trackingToken;
    private List<LrSummary> lorryReceipts;
    private LocalDateTime createdAt;

    @Data
    @Builder
    public static class LrSummary {
        private UUID id;
        private String lrNumber;
        private String origin;
        private String destination;
        private String status;
    }
}
