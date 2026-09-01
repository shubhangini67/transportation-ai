package com.tms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class VehicleLocationResponse {
    private UUID id;
    private UUID vehicleId;
    private UUID tripId;
    private Double latitude;
    private Double longitude;
    private Double speed;
    private Double heading;
    private LocalDateTime recordedAt;
}

