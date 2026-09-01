package com.tms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ProofOfDeliveryResponse {
    private UUID id;
    private UUID tripId;
    private UUID lrId;
    private String receiverName;
    private String receiverPhone;
    private String notes;
    private Double latitude;
    private Double longitude;
    private LocalDateTime deliveredAt;
}
