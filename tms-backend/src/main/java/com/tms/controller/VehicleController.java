package com.tms.controller;

import com.tms.dto.request.VehicleRequest;
import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.VehicleResponse;
import com.tms.enums.VehicleStatus;
import com.tms.enums.VehicleType;
import com.tms.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "Fleet management APIs")
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    @Operation(summary = "Get all vehicles (paginated)")
    public ResponseEntity<ApiResponse<Page<VehicleResponse>>> getAllVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) VehicleType type,
            @RequestParam(required = false) VehicleStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.getAllVehicles(page, size, type, status)));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get available vehicles")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getAvailableVehicles() {
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.getAvailableVehicles()));
    }

    @GetMapping("/fleet")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    @Operation(summary = "All vehicles with last known GPS for the India fleet map")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getFleetPositions() {
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.getFleetPositions()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    @Operation(summary = "Get vehicle by ID")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicle(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.getVehicleById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Create a vehicle")
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(@Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(vehicleService.createVehicle(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Update a vehicle")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(@PathVariable UUID id,
                                                                       @Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.updateVehicle(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a vehicle")
    public ResponseEntity<Void> deleteVehicle(@PathVariable UUID id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('ADMIN', 'DRIVER')")
    @Operation(summary = "Update vehicle GPS location")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateLocation(
            @PathVariable UUID id, @RequestBody java.util.Map<String, Double> body) {
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.updateLocation(id, body.get("latitude"), body.get("longitude"))));
    }
}
