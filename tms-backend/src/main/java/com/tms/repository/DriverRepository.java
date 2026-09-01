package com.tms.repository;

import com.tms.entity.Driver;
import com.tms.enums.DriverStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<Driver, UUID> {
    List<Driver> findByStatus(DriverStatus status);
    List<Driver> findByNameContainingIgnoreCase(String name);
    Optional<Driver> findByLicenseNumber(String licenseNumber);
    Optional<Driver> findByEmailIgnoreCase(String email);
    boolean existsByLicenseNumber(String licenseNumber);
    long countByStatus(DriverStatus status);

    @Query("SELECT d FROM Driver d WHERE " +
            "(:status IS NULL OR d.status = :status) AND " +
            "(:name = '' OR LOWER(d.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<Driver> findWithFilters(
            @Param("status") DriverStatus status,
            @Param("name") String name,
            Pageable pageable);
}
