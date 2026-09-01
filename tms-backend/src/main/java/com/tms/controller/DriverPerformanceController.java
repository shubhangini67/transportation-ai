package com.tms.controller;

import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.DriverScorecardResponse;
import com.tms.service.DriverPerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/performance/drivers")
@RequiredArgsConstructor
@Tag(name = "Driver performance")
public class DriverPerformanceController {

    private final DriverPerformanceService driverPerformanceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "On-time %, fuel spend and score band per driver")
    public ResponseEntity<ApiResponse<List<DriverScorecardResponse>>> scorecards() {
        return ResponseEntity.ok(ApiResponse.ok(driverPerformanceService.scorecards()));
    }
}
