package com.tms.controller;

import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.DashboardResponse;
import com.tms.dto.response.DashboardTrendResponse;
import com.tms.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard & analytics APIs")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CLIENT')")
    @Operation(summary = "Get dashboard statistics")
    public ResponseEntity<ApiResponse<DashboardResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboardStats()));
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CLIENT')")
    @Operation(summary = "Get dashboard metrics")
    public ResponseEntity<ApiResponse<DashboardResponse>> getMetrics() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboardStats()));
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CLIENT')")
    @Operation(summary = "Get dashboard trend data for charts")
    public ResponseEntity<ApiResponse<DashboardTrendResponse>> getTrends() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboardTrends()));
    }
}
