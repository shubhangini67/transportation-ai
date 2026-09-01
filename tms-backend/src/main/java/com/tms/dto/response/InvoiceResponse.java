package com.tms.dto.response;

import com.tms.enums.InvoiceStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class InvoiceResponse {
    private UUID id;
    private String invoiceNumber;
    private UUID tripId;
    private String tripVehicleNumber;
    private String tripDriverName;
    private String clientName;
    private String clientEmail;
    private BigDecimal subtotal;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private InvoiceStatus status;
    private String notes;
    private LocalDate issuedDate;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private List<InvoiceItemResponse> items;

    @Data @Builder
    public static class InvoiceItemResponse {
        private UUID id;
        private String description;
        private String category;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private UUID expenseId;
    }
}

