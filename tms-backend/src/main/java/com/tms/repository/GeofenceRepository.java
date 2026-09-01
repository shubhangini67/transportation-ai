package com.tms.repository;

import com.tms.entity.Geofence;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GeofenceRepository extends JpaRepository<Geofence, UUID> {
    List<Geofence> findByActiveTrue();
    List<Geofence> findByNameContainingIgnoreCase(String name);
    Page<Geofence> findAll(Pageable pageable);
}

