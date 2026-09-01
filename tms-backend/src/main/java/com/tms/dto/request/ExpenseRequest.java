package com.tms.dto.request;

import com.tms.enums.ExpenseCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class ExpenseRequest {
    private UUID tripId;
    private UUID vehicleId;
    @NotNull private ExpenseCategory category;
    @NotNull @Positive private BigDecimal amount;
    private String description;
    @NotNull private LocalDate expenseDate;
}

