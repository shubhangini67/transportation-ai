package com.tms.controller;

import com.tms.dto.request.ProofOfDeliveryRequest;
import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.ProofOfDeliveryResponse;
import com.tms.service.ProofOfDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/pod")
@RequiredArgsConstructor
@Tag(name = "Proof of Delivery")
public class ProofOfDeliveryController {

    private final ProofOfDeliveryService proofOfDeliveryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    public ResponseEntity<ApiResponse<List<ProofOfDeliveryResponse>>> list(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok(proofOfDeliveryService.forTrip(tripId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER')")
    @Operation(summary = "Capture proof of delivery with GPS and receiver OTP")
    public ResponseEntity<ApiResponse<ProofOfDeliveryResponse>> submit(
            @PathVariable UUID tripId,
            @Valid @RequestBody ProofOfDeliveryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(proofOfDeliveryService.submit(tripId, request)));
    }
}
