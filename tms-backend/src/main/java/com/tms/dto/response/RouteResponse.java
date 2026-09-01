package com.tms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RouteResponse {
    private Long id;
    private String origin;
    private String destination;
    private Double distance;
    private Integer estimatedTimeMinutes;
    private String description;
    private Boolean active;
    private LocalDateTime createdAt;
}

