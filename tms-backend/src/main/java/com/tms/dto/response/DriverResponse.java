package com.tms.dto.response;

import com.tms.enums.DriverStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DriverResponse {
    private UUID id;
    private String name;
    private String phone;
    private String licenseNumber;
    private String email;
    private DriverStatus status;
    private LocalDateTime createdAt;
}
