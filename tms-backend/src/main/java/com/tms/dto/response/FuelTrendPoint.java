package com.tms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FuelTrendPoint {
    private String period; // e.g., "2026-01"
    private BigDecimal totalSpend;
    private Double totalDistanceKm;
    private BigDecimal costPerKm;
}

