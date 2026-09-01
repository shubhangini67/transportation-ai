package com.tms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class InvoiceRequest {
    private UUID tripId;
    @NotBlank private String clientName;
    private String clientEmail;
    private BigDecimal taxRate;
    private String notes;
    private LocalDate dueDate;
    private List<InvoiceItemRequest> items;

    @Data
    public static class InvoiceItemRequest {
        @NotBlank private String description;
        private String category;
        @NotNull private Integer quantity;
        @NotNull private BigDecimal unitPrice;
    }
}

