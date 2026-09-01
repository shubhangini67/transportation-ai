package com.tms.controller;

import com.tms.dto.request.GeofenceRequest;
import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.GeofenceEventResponse;
import com.tms.dto.response.GeofenceResponse;
import com.tms.service.GeofenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/geofences")
@RequiredArgsConstructor
@Tag(name = "Geofences", description = "Geofence zone management and events")
public class GeofenceController {

    private final GeofenceService geofenceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get all geofences (paginated)")
    public ResponseEntity<ApiResponse<Page<GeofenceResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(geofenceService.getAll(page, size)));
    }

    @GetMapping("/events/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Recent geofence enter/exit events across India")
    public ResponseEntity<ApiResponse<Page<GeofenceEventResponse>>> getRecentEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(ApiResponse.ok(geofenceService.getRecentEvents(page, size)));
    }

    @GetMapping("/events/vehicle/{vehicleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get geofence events for a vehicle")
    public ResponseEntity<ApiResponse<Page<GeofenceEventResponse>>> getVehicleEvents(
            @PathVariable UUID vehicleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(geofenceService.getEventsForVehicle(vehicleId, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get geofence by ID")
    public ResponseEntity<ApiResponse<GeofenceResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(geofenceService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Create a geofence")
    public ResponseEntity<ApiResponse<GeofenceResponse>> create(@Valid @RequestBody GeofenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(geofenceService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Update a geofence")
    public ResponseEntity<ApiResponse<GeofenceResponse>> update(@PathVariable UUID id, @Valid @RequestBody GeofenceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(geofenceService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a geofence")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        geofenceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get events for a geofence")
    public ResponseEntity<ApiResponse<Page<GeofenceEventResponse>>> getEvents(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(geofenceService.getEventsForGeofence(id, page, size)));
    }
}

