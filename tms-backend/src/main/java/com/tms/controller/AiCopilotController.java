package com.tms.controller;

import com.tms.dto.request.AiAskRequest;
import com.tms.dto.response.AiAskResponse;
import com.tms.dto.response.ApiResponse;
import com.tms.service.AiCopilotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI copilot")
public class AiCopilotController {

    private final AiCopilotService aiCopilotService;

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Which AI engine is active")
    public ResponseEntity<ApiResponse<AiAskResponse>> status() {
        return ResponseEntity.ok(ApiResponse.ok(aiCopilotService.status()));
    }

    @GetMapping("/briefing")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Shift handover from live TMS data (what hi returns)")
    public ResponseEntity<ApiResponse<AiAskResponse>> briefing() {
        return ResponseEntity.ok(ApiResponse.ok(aiCopilotService.briefing("/dashboard")));
    }

    @PostMapping("/ask")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "TMS Bot — Q&A plus conversational create/update/delete")
    public ResponseEntity<ApiResponse<AiAskResponse>> ask(@Valid @RequestBody AiAskRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(aiCopilotService.ask(request)));
    }

    @PostMapping("/reset")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Clear in-progress form filling")
    public ResponseEntity<ApiResponse<AiAskResponse>> reset() {
        return ResponseEntity.ok(ApiResponse.ok(aiCopilotService.resetSession()));
    }
}
