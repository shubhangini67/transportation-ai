package com.tms.dto.response;

import com.tms.enums.LrStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class LrResponse {
    private UUID id;
    private String lrNumber;
    private String consignor;
    private String consignee;
    private String origin;
    private String destination;
    private String material;
    private Double weight;
    private Integer quantity;
    private LrStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

