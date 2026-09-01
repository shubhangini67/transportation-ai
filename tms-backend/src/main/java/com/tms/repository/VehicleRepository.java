package com.tms.repository;

import com.tms.entity.Vehicle;
import com.tms.enums.VehicleStatus;
import com.tms.enums.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    List<Vehicle> findByStatus(VehicleStatus status);
    Optional<Vehicle> findByVehicleNumber(String vehicleNumber);
    Optional<Vehicle> findByVehicleNumberIgnoreCase(String vehicleNumber);
    List<Vehicle> findByVehicleNumberContainingIgnoreCase(String vehicleNumber);
    boolean existsByVehicleNumber(String vehicleNumber);
    long countByStatus(VehicleStatus status);

    @Query("SELECT v FROM Vehicle v WHERE " +
            "(:type IS NULL OR v.type = :type) AND " +
            "(:status IS NULL OR v.status = :status)")
    Page<Vehicle> findWithFilters(
            @Param("type") VehicleType type,
            @Param("status") VehicleStatus status,
            Pageable pageable);
}
