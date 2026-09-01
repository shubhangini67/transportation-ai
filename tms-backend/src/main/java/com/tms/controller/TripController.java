package com.tms.controller;

import com.tms.dto.request.TripRequest;
import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.TripResponse;
import com.tms.enums.TripStatus;
import com.tms.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
@Tag(name = "Trips", description = "Trip management APIs")
public class TripController {

    private final TripService tripService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CLIENT')")
    @Operation(summary = "Get all trips (paginated)")
    public ResponseEntity<ApiResponse<Page<TripResponse>>> getAllTrips(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TripStatus status,
            @RequestParam(required = false) LocalDateTime startFrom,
            @RequestParam(required = false) LocalDateTime startTo) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.getAllTrips(page, size, status, startFrom, startTo)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CLIENT')")
    @Operation(summary = "Get trip by ID")
    public ResponseEntity<ApiResponse<TripResponse>> getTrip(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.getTripById(id)));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get trips by status")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getTripsByStatus(@PathVariable TripStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.getTripsByStatus(status)));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get recent trips")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getRecentTrips() {
        return ResponseEntity.ok(ApiResponse.ok(tripService.getRecentTrips()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Create a trip")
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(@Valid @RequestBody TripRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(tripService.createTrip(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Update a trip")
    public ResponseEntity<ApiResponse<TripResponse>> updateTrip(@PathVariable UUID id,
                                                                 @Valid @RequestBody TripRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.updateTrip(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    @Operation(summary = "Update trip status")
    public ResponseEntity<ApiResponse<TripResponse>> updateTripStatus(@PathVariable UUID id,
                                                                       @RequestBody Map<String, String> body) {
        TripStatus status = TripStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(ApiResponse.ok(tripService.updateTripStatus(id, status)));
    }

    @GetMapping("/{id}/tracking")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CLIENT')")
    @Operation(summary = "Get trip tracking data (vehicle GPS location)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTracking(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(tripService.getTrackingData(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a trip")
    public ResponseEntity<Void> deleteTrip(@PathVariable UUID id) {
        tripService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }
}
