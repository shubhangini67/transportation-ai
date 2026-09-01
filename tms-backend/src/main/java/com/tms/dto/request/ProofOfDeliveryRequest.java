package com.tms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class ProofOfDeliveryRequest {
    @NotBlank
    private String receiverName;
    private String receiverPhone;
    private String otp;
    private String notes;
    private UUID lrId;
    private Double latitude;
    private Double longitude;
}
