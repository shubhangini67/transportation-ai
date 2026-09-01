package com.tms.dto.request;

import com.tms.enums.GeofenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class GeofenceRequest {
    @NotBlank(message = "Name is required")
    private String name;
    private String description;
    @NotNull(message = "Latitude is required")
    private Double latitude;
    @NotNull(message = "Longitude is required")
    private Double longitude;
    @NotNull(message = "Radius is required")
    @Positive(message = "Radius must be positive")
    private Double radiusMeters;
    private GeofenceType type;
    private Boolean active;
}

