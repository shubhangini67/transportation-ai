package com.tms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class MaintenanceAlertResponse {
    private UUID vehicleId;
    private String vehicleNumber;
    private String vehicleType;
    private String status;
    private Integer odometerKm;
    private Integer nextServiceDueKm;
    private Integer kmRemaining;
    private LocalDate lastServiceDate;
    private String severity;
    private String message;
}
