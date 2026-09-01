package com.tms.controller;

import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.FleetInsightsResponse;
import com.tms.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Reports & analytics APIs")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/trips")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTripReport(
            @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getTripReport(from, to)));
    }

    @GetMapping("/trips/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<byte[]> getTripReportCsv(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        String csv = reportService.exportCsv(reportService.getTripReport(from, to));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trip-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @GetMapping("/vehicles")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVehicleReport() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getVehicleUtilizationReport()));
    }

    @GetMapping("/drivers")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDriverReport() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getDriverPerformanceReport()));
    }

    @GetMapping("/insights")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Fleet insights scored by the ASP.NET Core reporting service")
    public ResponseEntity<ApiResponse<FleetInsightsResponse>> getFleetInsights() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getFleetInsights()));
    }
}

