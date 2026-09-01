package com.tms.controller;

import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.OperationsAlertResponse;
import com.tms.service.OperationsService;
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
@RequestMapping("/api/v1/operations")
@RequiredArgsConstructor
@Tag(name = "Operations", description = "Delay and overdue trip exceptions")
public class OperationsController {

    private final OperationsService operationsService;

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Trips that are overdue to start or running past ETA")
    public ResponseEntity<ApiResponse<List<OperationsAlertResponse>>> alerts() {
        return ResponseEntity.ok(ApiResponse.ok(operationsService.getAlerts()));
    }
}
