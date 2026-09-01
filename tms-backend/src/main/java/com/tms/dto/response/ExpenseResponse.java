package com.tms.dto.response;

import com.tms.enums.ExpenseCategory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
public class ExpenseResponse {
    private UUID id;
    private UUID tripId;
    private UUID vehicleId;
    private String vehicleNumber;
    private ExpenseCategory category;
    private BigDecimal amount;
    private String description;
    private LocalDate expenseDate;
    private LocalDateTime createdAt;
}

