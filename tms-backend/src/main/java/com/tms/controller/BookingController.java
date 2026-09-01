package com.tms.controller;

import com.tms.dto.request.BookingRequest;
import com.tms.dto.response.ApiResponse;
import com.tms.dto.response.BookingResponse;
import com.tms.enums.BookingStatus;
import com.tms.service.BookingService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management APIs")
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'CLIENT')")
    @Operation(summary = "Get all bookings (paginated)")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String customerName) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getAllBookings(page, size, status, customerName)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'CLIENT')")
    @Operation(summary = "Get booking by ID")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getBookingById(id)));
    }

    @GetMapping("/trip/{tripId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Get bookings by trip")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByTrip(@PathVariable UUID tripId) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getBookingsByTrip(tripId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'CLIENT')")
    @Operation(summary = "Create a booking")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(bookingService.createBooking(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    @Operation(summary = "Update a booking")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBooking(@PathVariable Long id,
                                                                       @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.updateBooking(id, request)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'CLIENT')")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.cancelBooking(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a booking")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }
}
