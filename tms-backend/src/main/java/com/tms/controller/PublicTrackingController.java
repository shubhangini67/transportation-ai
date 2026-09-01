package com.tms.controller;

import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.PublicTrackingResponse;
import com.tms.service.PublicTrackingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "Public tracking")
public class PublicTrackingController {

    private final PublicTrackingService publicTrackingService;

    @GetMapping("/track/{token}")
    public ResponseEntity<ApiResponse<PublicTrackingResponse>> track(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok(publicTrackingService.track(token)));
    }
}
