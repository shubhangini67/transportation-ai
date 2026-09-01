package com.tms.controller;

import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.MaintenanceAlertResponse;
import com.tms.service.MaintenanceService;
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
@RequestMapping("/api/v1/maintenance")
@RequiredArgsConstructor
@Tag(name = "Maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Vehicles overdue or approaching service km")
    public ResponseEntity<ApiResponse<List<MaintenanceAlertResponse>>> alerts() {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.getAlerts()));
    }
}
