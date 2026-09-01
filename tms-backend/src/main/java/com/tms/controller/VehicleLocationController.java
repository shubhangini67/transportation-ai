package com.tms.controller;

import com.tms.dto.request.LocationUpdateRequest;
import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.VehicleLocationResponse;
import com.tms.service.VehicleLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Vehicle Location", description = "GPS tracking APIs")
public class VehicleLocationController {

    private final VehicleLocationService locationService;

    @MessageMapping("/location.update")
    public void handleLocationUpdate(LocationUpdateRequest request) {
        locationService.processLocationUpdate(request);
    }

    @PostMapping("/api/v1/vehicles/{vehicleId}/location")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    @Operation(summary = "Submit vehicle location update via REST")
    public ResponseEntity<ApiResponse<VehicleLocationResponse>> submitLocation(
            @PathVariable UUID vehicleId, @RequestBody LocationUpdateRequest request) {
        request.setVehicleId(vehicleId);
        return ResponseEntity.ok(ApiResponse.ok(locationService.processLocationUpdate(request)));
    }

    @GetMapping("/api/v1/trips/{tripId}/route-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CLIENT')")
    @Operation(summary = "Get historical route for a trip")
    public ResponseEntity<ApiResponse<List<VehicleLocationResponse>>> getRouteHistory(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.getRouteHistory(tripId)));
    }

    @GetMapping("/api/v1/vehicles/{vehicleId}/location-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get vehicle location history for date range")
    public ResponseEntity<ApiResponse<List<VehicleLocationResponse>>> getVehicleLocationHistory(
            @PathVariable UUID vehicleId,
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.getVehicleLocationHistory(vehicleId, from, to)));
    }
}

