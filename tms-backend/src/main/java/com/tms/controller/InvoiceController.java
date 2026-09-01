package com.tms.controller;

import com.tms.dto.request.InvoiceRequest;
import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.InvoiceResponse;
import com.tms.enums.InvoiceStatus;
import com.tms.service.InvoicePdfService;
import com.tms.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice management APIs")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get all invoices (paginated)")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getAllInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.getAllInvoices(page, size, status, from, to)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get invoice by ID")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.getInvoiceById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Create an invoice manually")
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(@Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(invoiceService.createInvoice(request)));
    }

    @PostMapping("/generate/{tripId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Auto-generate invoice from trip expenses")
    public ResponseEntity<ApiResponse<InvoiceResponse>> generateFromTrip(@PathVariable UUID tripId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(invoiceService.generateFromTrip(tripId)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Update invoice status")
    public ResponseEntity<ApiResponse<InvoiceResponse>> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        InvoiceStatus status = InvoiceStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.updateInvoiceStatus(id, status)));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Download invoice as PDF")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        com.tms.entity.Invoice invoice = invoiceService.findById(id);
        byte[] pdf = invoicePdfService.generatePdf(invoice);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + invoice.getInvoiceNumber() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an invoice")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }
}

