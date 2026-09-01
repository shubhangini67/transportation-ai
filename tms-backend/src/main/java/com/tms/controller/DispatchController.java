package com.tms.controller;

import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.DispatchPlanResponse;
import com.tms.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dispatch")
@RequiredArgsConstructor
@Tag(name = "Dispatch", description = "Smart vehicle/driver assignment via ASP.NET Core")
public class DispatchController {

    private final DispatchService dispatchService;

    @GetMapping("/recommend")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Rank vehicle–driver pairs for a route using the ASP.NET Core optimizer")
    public ResponseEntity<ApiResponse<DispatchPlanResponse>> recommend(
            @RequestParam Long routeId,
            @RequestParam(defaultValue = "1") int requiredCapacity) {
        return ResponseEntity.ok(ApiResponse.ok(dispatchService.recommend(routeId, requiredCapacity)));
    }
}
