package com.tms.service;

import com.tms.dto.request.DriverRequest;
import com.tms.dto.response.DriverResponse;
import com.tms.entity.Driver;
import com.tms.enums.DriverStatus;
import com.tms.enums.TripStatus;
import com.tms.enums.AuditAction;
import com.tms.exception.BadRequestException;
import com.tms.exception.DuplicateResourceException;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.DriverRepository;
import com.tms.repository.TripRepository;
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
public class DriverService {

    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<DriverResponse> getAllDrivers(int page, int size, DriverStatus status, String name) {
        log.debug("Fetching drivers page={} size={} status={} name={}", page, size, status, name);
        return driverRepository.findWithFilters(status, name == null ? "" : name,
                PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DriverResponse getDriverById(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<DriverResponse> getActiveDrivers() {
        return driverRepository.findByStatus(DriverStatus.ACTIVE).stream().map(this::toResponse).toList();
    }

    @Transactional
    public DriverResponse createDriver(DriverRequest request) {
        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateResourceException("Driver", "licenseNumber", request.getLicenseNumber());
        }

        Driver driver = Driver.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .licenseNumber(request.getLicenseNumber())
                .email(request.getEmail())
                .status(request.getStatus() != null ? request.getStatus() : DriverStatus.ACTIVE)
                .build();

        Driver saved = driverRepository.save(driver);
        log.info("Driver created: {} ({})", saved.getName(), saved.getId());
        DriverResponse response = toResponse(saved);
        auditLogService.log("Driver", saved.getId().toString(), AuditAction.CREATE, null, response);
        return response;
    }

    @Transactional
    public DriverResponse updateDriver(UUID id, DriverRequest request) {
        Driver driver = findById(id);

        driverRepository.findByLicenseNumber(request.getLicenseNumber())
                .filter(d -> !d.getId().equals(id))
                .ifPresent(d -> {
                    throw new DuplicateResourceException("Driver", "licenseNumber", request.getLicenseNumber());
                });

        driver.setName(request.getName());
        driver.setPhone(request.getPhone());
        driver.setLicenseNumber(request.getLicenseNumber());
        driver.setEmail(request.getEmail());
        if (request.getStatus() != null) driver.setStatus(request.getStatus());

        log.info("Driver updated: {}", id);
        return toResponse(driverRepository.save(driver));
    }

    @Transactional
    public void deleteDriver(UUID id) {
        Driver driver = findById(id);

        boolean hasActiveTrip = tripRepository.existsByDriverIdAndStatusIn(
                id, List.of(TripStatus.PLANNED, TripStatus.IN_PROGRESS));
        if (hasActiveTrip) {
            throw new BadRequestException("Cannot delete driver assigned to an active trip");
        }

        driverRepository.delete(driver);
        log.info("Driver deleted: {}", id);
    }

    Driver findById(UUID id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", id));
    }

    private DriverResponse toResponse(Driver d) {
        return DriverResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .phone(d.getPhone())
                .licenseNumber(d.getLicenseNumber())
                .email(d.getEmail())
                .status(d.getStatus())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
