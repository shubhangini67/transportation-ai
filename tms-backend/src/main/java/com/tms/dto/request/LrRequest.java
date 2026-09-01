package com.tms.dto.request;

import com.tms.enums.LrStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class LrRequest {

    @NotBlank(message = "LR number is required")
    private String lrNumber;

    @NotBlank(message = "Consignor is required")
    private String consignor;

    @NotBlank(message = "Consignee is required")
    private String consignee;

    @NotBlank(message = "Origin is required")
    private String origin;

    @NotBlank(message = "Destination is required")
    private String destination;

    private String material;

    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be greater than 0")
    private Double weight;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    private Integer quantity;

    private LrStatus status;
}

