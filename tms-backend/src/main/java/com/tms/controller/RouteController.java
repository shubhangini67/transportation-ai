package com.tms.controller;

import com.tms.dto.request.RouteRequest;
import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.RouteResponse;
import com.tms.service.RouteService;
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

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
@Tag(name = "Routes", description = "Route management APIs")
public class RouteController {

    private final RouteService routeService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all routes (paginated)")
    public ResponseEntity<ApiResponse<Page<RouteResponse>>> getAllRoutes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(routeService.getAllRoutes(page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get route by ID")
    public ResponseEntity<ApiResponse<RouteResponse>> getRoute(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(routeService.getRouteById(id)));
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get active routes")
    public ResponseEntity<ApiResponse<List<RouteResponse>>> getActiveRoutes() {
        return ResponseEntity.ok(ApiResponse.ok(routeService.getActiveRoutes()));
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search routes by origin/destination")
    public ResponseEntity<ApiResponse<List<RouteResponse>>> searchRoutes(@RequestParam String query) {
        return ResponseEntity.ok(ApiResponse.ok(routeService.searchRoutes(query)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Create a route")
    public ResponseEntity<ApiResponse<RouteResponse>> createRoute(@Valid @RequestBody RouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(routeService.createRoute(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Update a route")
    public ResponseEntity<ApiResponse<RouteResponse>> updateRoute(@PathVariable Long id,
                                                                    @Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(routeService.updateRoute(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a route")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }
}
