package com.tms.dto.response;

import com.tms.enums.VehicleStatus;
import com.tms.enums.VehicleType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VehicleResponse {
    private UUID id;
    private String vehicleNumber;
    private VehicleType type;
    private Integer capacity;
    private VehicleStatus status;
    private String currentLocation;
    private String make;
    private String model;
    private Integer year;
    private Double latitude;
    private Double longitude;
    private Integer odometerKm;
    private LocalDate lastServiceDate;
    private Integer nextServiceDueKm;
    private LocalDateTime createdAt;
}
