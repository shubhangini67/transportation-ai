package com.tms.service;

import com.tms.dto.request.BookingRequest;
import com.tms.dto.response.BookingResponse;
import com.tms.entity.Booking;
import com.tms.entity.Trip;
import com.tms.enums.BookingStatus;
import com.tms.enums.TripStatus;
import com.tms.enums.AuditAction;
import com.tms.enums.WebhookEventType;
import com.tms.exception.BadRequestException;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TripService tripService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final WebhookDispatchService webhookDispatchService;

    @Transactional(readOnly = true)
    public Page<BookingResponse> getAllBookings(int page, int size, BookingStatus status, String customerName) {
        log.debug("Fetching bookings page={} size={} status={} customerName={}", page, size, status, customerName);
        return bookingRepository.findWithFilters(status, customerName == null ? "" : customerName,
                PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByTrip(UUID tripId) {
        return bookingRepository.findByTripId(tripId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        Trip trip = tripService.findById(request.getTripId());

        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new BadRequestException("Bookings can only be made for PLANNED trips");
        }

        int bookedSeats = bookingRepository.sumSeatsByTripId(trip.getId());
        int availableSeats = trip.getVehicle().getCapacity() - bookedSeats;

        if (request.getSeatCount() > availableSeats) {
            throw new BadRequestException(
                    String.format("Only %d seats available, requested %d", availableSeats, request.getSeatCount()));
        }

        Booking booking = Booking.builder()
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerEmail(request.getCustomerEmail())
                .trip(trip)
                .seatCount(request.getSeatCount())
                .status(BookingStatus.CONFIRMED)
                .notes(request.getNotes())
                .build();

        Booking saved = bookingRepository.save(booking);
        BookingResponse response = toResponse(saved);
        auditLogService.log("Booking", String.valueOf(saved.getId()), AuditAction.CREATE, null, response);
        emailService.sendBookingConfirmation(saved);
        webhookDispatchService.dispatch(WebhookEventType.BOOKING_CREATED, response);
        return response;
    }

    @Transactional
    public BookingResponse updateBooking(Long id, BookingRequest request) {
        Booking booking = findById(id);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Cannot update a cancelled booking");
        }

        Trip trip = tripService.findById(request.getTripId());

        if (!booking.getTrip().getId().equals(request.getTripId()) ||
                !booking.getSeatCount().equals(request.getSeatCount())) {
            int bookedSeats = bookingRepository.sumSeatsByTripId(trip.getId());
            if (booking.getTrip().getId().equals(trip.getId())) {
                bookedSeats -= booking.getSeatCount();
            }
            int availableSeats = trip.getVehicle().getCapacity() - bookedSeats;
            if (request.getSeatCount() > availableSeats) {
                throw new BadRequestException(
                        String.format("Only %d seats available, requested %d", availableSeats, request.getSeatCount()));
            }
        }

        booking.setCustomerName(request.getCustomerName());
        booking.setCustomerPhone(request.getCustomerPhone());
        booking.setCustomerEmail(request.getCustomerEmail());
        booking.setTrip(trip);
        booking.setSeatCount(request.getSeatCount());
        booking.setNotes(request.getNotes());

        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse cancelBooking(Long id) {
        Booking booking = findById(id);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public void deleteBooking(Long id) {
        Booking booking = findById(id);
        bookingRepository.delete(booking);
    }

    private Booking findById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
    }

    private BookingResponse toResponse(Booking b) {
        return BookingResponse.builder()
                .id(b.getId())
                .customerName(b.getCustomerName())
                .customerPhone(b.getCustomerPhone())
                .customerEmail(b.getCustomerEmail())
                .tripId(b.getTrip().getId())
                .vehicleNumber(b.getTrip().getVehicle().getVehicleNumber())
                .seatCount(b.getSeatCount())
                .status(b.getStatus())
                .notes(b.getNotes())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
