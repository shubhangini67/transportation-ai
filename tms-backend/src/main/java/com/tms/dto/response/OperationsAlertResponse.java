package com.tms.dto.response;

import com.tms.enums.TripStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OperationsAlertResponse {
    private UUID tripId;
    private String vehicleNumber;
    private String driverName;
    private String routeLabel;
    private TripStatus tripStatus;
    private String severity;
    private String code;
    private String message;
    private LocalDateTime startTime;
    private Integer estimatedMinutes;
    private long minutesOverdue;
}
