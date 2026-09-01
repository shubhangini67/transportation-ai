package com.tms.dto.request;

import com.tms.enums.VehicleStatus;
import com.tms.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class VehicleRequest {
    @NotBlank(message = "Vehicle number is required")
    private String vehicleNumber;

    @NotNull(message = "Vehicle type is required")
    private VehicleType type;

    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be positive")
    private Integer capacity;

    private VehicleStatus status;
    private String currentLocation;
    private String make;
    private String model;
    private Integer year;
    private Integer odometerKm;
    private java.time.LocalDate lastServiceDate;
    private Integer nextServiceDueKm;
}
