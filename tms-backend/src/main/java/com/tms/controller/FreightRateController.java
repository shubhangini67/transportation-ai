package com.tms.controller;

import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.FreightRateCardResponse;
import com.tms.enums.VehicleType;
import com.tms.service.FreightRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rates")
@RequiredArgsConstructor
@Tag(name = "Freight rates")
public class FreightRateController {

    private final FreightRateService freightRateService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<List<FreightRateCardResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(freightRateService.list()));
    }

    @GetMapping("/quote")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Quote a lane with GST 18% from the rate card")
    public ResponseEntity<ApiResponse<FreightRateCardResponse>> quote(
            @RequestParam Long routeId,
            @RequestParam VehicleType vehicleType) {
        return ResponseEntity.ok(ApiResponse.ok(freightRateService.quote(routeId, vehicleType)));
    }
}
