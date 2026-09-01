package com.tms.repository;

import com.tms.entity.Booking;
import com.tms.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByTripId(UUID tripId);
    List<Booking> findByStatus(BookingStatus status);
    long countByStatus(BookingStatus status);

    @Query("SELECT COALESCE(SUM(b.seatCount), 0) FROM Booking b WHERE b.trip.id = :tripId AND b.status = 'CONFIRMED'")
    int sumSeatsByTripId(@Param("tripId") UUID tripId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.createdAt BETWEEN :start AND :end")
    long countBookingsInDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT b FROM Booking b WHERE " +
            "(:status IS NULL OR b.status = :status) AND " +
            "(:customerName = '' OR LOWER(b.customerName) LIKE LOWER(CONCAT('%', :customerName, '%')))")
    Page<Booking> findWithFilters(
            @Param("status") BookingStatus status,
            @Param("customerName") String customerName,
            Pageable pageable);
}
