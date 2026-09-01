package com.tms.repository;

import com.tms.entity.Trip;
import com.tms.enums.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TripRepository extends JpaRepository<Trip, UUID> {
    List<Trip> findByStatus(TripStatus status);
    @Query("SELECT DISTINCT t FROM Trip t JOIN FETCH t.vehicle LEFT JOIN FETCH t.route WHERE t.status = :status")
    List<Trip> findLiveTripsForGps(@Param("status") TripStatus status);
    List<Trip> findByDriverId(UUID driverId);
    List<Trip> findByDriverIdOrderByStartTimeDesc(UUID driverId);
    Optional<Trip> findByTrackingToken(String trackingToken);
    List<Trip> findByVehicleId(UUID vehicleId);
    long countByStatus(TripStatus status);

    @Query("SELECT t FROM Trip t ORDER BY t.createdAt DESC LIMIT 10")
    List<Trip> findRecentTrips();

    boolean existsByVehicleIdAndStatusIn(UUID vehicleId, List<TripStatus> statuses);
    boolean existsByDriverIdAndStatusIn(UUID driverId, List<TripStatus> statuses);
    boolean existsByLorryReceiptsId(UUID lrId);

    @Query("SELECT t FROM Trip t WHERE " +
            "(:status IS NULL OR t.status = :status) AND " +
            "t.startTime >= COALESCE(:startFrom, t.startTime) AND " +
            "t.startTime <= COALESCE(:startTo, t.startTime)")
    Page<Trip> findWithFilters(
            @Param("status") TripStatus status,
            @Param("startFrom") LocalDateTime startFrom,
            @Param("startTo") LocalDateTime startTo,
            Pageable pageable);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT t FROM Trip t JOIN FETCH t.route WHERE t.vehicle.id = :vehicleId AND t.status = 'COMPLETED' AND t.route IS NOT NULL AND t.endTime BETWEEN :from AND :to")
    List<Trip> findCompletedTripsWithRouteByVehicle(@Param("vehicleId") UUID vehicleId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT t FROM Trip t JOIN FETCH t.route WHERE t.status = 'COMPLETED' AND t.route IS NOT NULL AND t.endTime BETWEEN :from AND :to")
    List<Trip> findCompletedTripsWithRouteInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
