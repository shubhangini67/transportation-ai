package com.tms.repository;

import com.tms.entity.GeofenceEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GeofenceEventRepository extends JpaRepository<GeofenceEvent, UUID> {
    Page<GeofenceEvent> findByGeofenceIdOrderByEventTimeDesc(UUID geofenceId, Pageable pageable);
    Page<GeofenceEvent> findByVehicleIdOrderByEventTimeDesc(UUID vehicleId, Pageable pageable);
    Page<GeofenceEvent> findAllByOrderByEventTimeDesc(Pageable pageable);
    Optional<GeofenceEvent> findFirstByVehicleIdAndGeofenceIdOrderByEventTimeDesc(UUID vehicleId, UUID geofenceId);
}

