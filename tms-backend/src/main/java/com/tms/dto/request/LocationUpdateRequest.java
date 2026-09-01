package com.tms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class LocationUpdateRequest {
    @NotNull private UUID vehicleId;
    private UUID tripId;
    @NotNull private Double latitude;
    @NotNull private Double longitude;
    private Double speed;
    private Double heading;
}

