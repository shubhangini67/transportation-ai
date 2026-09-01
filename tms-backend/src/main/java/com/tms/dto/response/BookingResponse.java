package com.tms.dto.response;

import com.tms.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BookingResponse {
    private Long id;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private UUID tripId;
    private String vehicleNumber;
    private Integer seatCount;
    private BookingStatus status;
    private String notes;
    private LocalDateTime createdAt;
}
