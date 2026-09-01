package com.tms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PublicTrackingResponse {
    private String token;
    private String vehicleNumber;
    private String driverName;
    private String routeLabel;
    private String status;
    private Double latitude;
    private Double longitude;
    private String currentLocation;
    private LocalDateTime startTime;
    private LocalDateTime lastUpdate;
    private List<String> consignmentHints;
}
