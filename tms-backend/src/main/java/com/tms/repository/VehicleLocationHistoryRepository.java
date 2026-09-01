package com.tms.repository;

import com.tms.entity.VehicleLocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface VehicleLocationHistoryRepository extends JpaRepository<VehicleLocationHistory, UUID> {
    List<VehicleLocationHistory> findByTripIdOrderByRecordedAtAsc(UUID tripId);
    List<VehicleLocationHistory> findByVehicleIdAndRecordedAtBetweenOrderByRecordedAtAsc(UUID vehicleId, LocalDateTime from, LocalDateTime to);
}

