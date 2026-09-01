package com.tms.dto.response;

import com.tms.enums.GeofenceType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class GeofenceResponse {
    private UUID id;
    private String name;
    private String description;
    private Double latitude;
    private Double longitude;
    private Double radiusMeters;
    private GeofenceType type;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

