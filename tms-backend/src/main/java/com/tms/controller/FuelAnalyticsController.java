package com.tms.controller;

import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.FuelAnalyticsResponse;
import com.tms.dto.response.VehicleFuelBreakdown;
import com.tms.service.FuelAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics/fuel")
@RequiredArgsConstructor
@Tag(name = "Fuel Analytics", description = "Fuel efficiency and cost-per-km analytics")
public class FuelAnalyticsController {

    private final FuelAnalyticsService fuelAnalyticsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get fuel analytics overview with cost-per-km breakdowns")
    public ResponseEntity<ApiResponse<FuelAnalyticsResponse>> getAnalytics(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        if (from == null) from = LocalDate.now().minusMonths(6);
        if (to == null) to = LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(fuelAnalyticsService.getAnalytics(from, to)));
    }

    @GetMapping("/vehicle/{vehicleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get fuel analytics for a specific vehicle")
    public ResponseEntity<ApiResponse<VehicleFuelBreakdown>> getVehicleDetail(
            @PathVariable UUID vehicleId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        if (from == null) from = LocalDate.now().minusMonths(6);
        if (to == null) to = LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(fuelAnalyticsService.getVehicleDetail(vehicleId, from, to)));
    }
}

