package com.tms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.UUID;

@Data
public class BookingRequest {
    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer phone is required")
    private String customerPhone;

    private String customerEmail;

    @NotNull(message = "Trip ID is required")
    private UUID tripId;

    @NotNull(message = "Seat count is required")
    @Positive(message = "Seat count must be positive")
    private Integer seatCount;

    private String notes;
}
