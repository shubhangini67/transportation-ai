package com.tms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class RouteRequest {
    @NotBlank(message = "Origin is required")
    private String origin;

    @NotBlank(message = "Destination is required")
    private String destination;

    @NotNull(message = "Distance is required")
    @Positive(message = "Distance must be positive")
    private Double distance;

    @NotNull(message = "Estimated time is required")
    @Positive(message = "Estimated time must be positive")
    private Integer estimatedTimeMinutes;

    private String description;
    private Boolean active;
}

