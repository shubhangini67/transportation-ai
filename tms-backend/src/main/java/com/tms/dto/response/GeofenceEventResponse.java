package com.tms.dto.response;

import com.tms.enums.GeofenceEventType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class GeofenceEventResponse {
    private UUID id;
    private UUID geofenceId;
    private String geofenceName;
    private UUID vehicleId;
    private String vehicleNumber;
    private UUID tripId;
    private GeofenceEventType eventType;
    private Double latitude;
    private Double longitude;
    private LocalDateTime eventTime;
}

