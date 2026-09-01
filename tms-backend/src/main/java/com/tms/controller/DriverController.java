package com.tms.controller;

import com.tms.dto.request.DriverRequest;
import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.DriverResponse;
import com.tms.enums.DriverStatus;
import com.tms.service.DriverService;
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
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
@Tag(name = "Drivers", description = "Driver management APIs")
public class DriverController {

    private final DriverService driverService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get all drivers (paginated)")
    public ResponseEntity<ApiResponse<Page<DriverResponse>>> getAllDrivers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) DriverStatus status,
            @RequestParam(required = false) String name) {
        return ResponseEntity.ok(ApiResponse.ok(driverService.getAllDrivers(page, size, status, name)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get driver by ID")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriver(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(driverService.getDriverById(id)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get active drivers")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getActiveDrivers() {
        return ResponseEntity.ok(ApiResponse.ok(driverService.getActiveDrivers()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Create a driver")
    public ResponseEntity<ApiResponse<DriverResponse>> createDriver(@Valid @RequestBody DriverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(driverService.createDriver(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Update a driver")
    public ResponseEntity<ApiResponse<DriverResponse>> updateDriver(@PathVariable UUID id,
                                                                     @Valid @RequestBody DriverRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(driverService.updateDriver(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a driver")
    public ResponseEntity<Void> deleteDriver(@PathVariable UUID id) {
        driverService.deleteDriver(id);
        return ResponseEntity.noContent().build();
    }
}
