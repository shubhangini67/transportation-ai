package com.tms.controller;

import com.tms.dto.request.LrRequest;
import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.LrResponse;
import com.tms.enums.LrStatus;
import com.tms.service.LorryReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lrs")
@RequiredArgsConstructor
@Tag(name = "Lorry Receipts", description = "LR management APIs")
public class LorryReceiptController {

    private final LorryReceiptService lrService;
    private final com.tms.service.LrPdfService lrPdfService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CLIENT')")
    @Operation(summary = "Get all LRs (paginated + filterable)")
    public ResponseEntity<ApiResponse<Page<LrResponse>>> getAllLrs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) LrStatus status,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination) {
        return ResponseEntity.ok(ApiResponse.ok(lrService.getAllLrs(page, size, sortBy, sortDir, status, origin, destination)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CLIENT')")
    @Operation(summary = "Get LR by ID")
    public ResponseEntity<ApiResponse<LrResponse>> getLr(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(lrService.getLrById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Create a lorry receipt")
    public ResponseEntity<ApiResponse<LrResponse>> createLr(@Valid @RequestBody LrRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(lrService.createLr(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Update a lorry receipt")
    public ResponseEntity<ApiResponse<LrResponse>> updateLr(@PathVariable UUID id,
                                                              @Valid @RequestBody LrRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(lrService.updateLr(id, request)));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'DRIVER', 'CLIENT')")
    @Operation(summary = "Download LR as PDF")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        com.tms.entity.LorryReceipt lr = lrService.findById(id);
        byte[] pdf = lrPdfService.generatePdf(lr);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=LR-" + lr.getLrNumber() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a lorry receipt")
    public ResponseEntity<Void> deleteLr(@PathVariable UUID id) {
        lrService.deleteLr(id);
        return ResponseEntity.noContent().build();
    }
}
