package com.tms.controller;

import com.tms.dto.response.ApiResponse;
import com.tms.service.DriverPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Tag(name = "Driver portal", description = "Assigned trips for the logged-in driver")
public class DriverPortalController {

    private final DriverPortalService driverPortalService;

    @GetMapping("/board")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Driver workbench: my trips and counts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> myBoard() {
        return ResponseEntity.ok(ApiResponse.ok(driverPortalService.myBoard()));
    }
}
