package com.tms.service;

import com.tms.dto.request.VehicleRequest;
import com.tms.dto.response.VehicleResponse;
import com.tms.entity.Vehicle;
import com.tms.enums.TripStatus;
import com.tms.enums.VehicleStatus;
import com.tms.enums.VehicleType;
import com.tms.enums.AuditAction;
import com.tms.exception.BadRequestException;
import com.tms.exception.DuplicateResourceException;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.TripRepository;
import com.tms.repository.VehicleRepository;
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
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public Page<VehicleResponse> getAllVehicles(int page, int size, VehicleType type, VehicleStatus status) {
        log.debug("Fetching vehicles page={} size={} type={} status={}", page, size, type, status);
        return vehicleRepository.findWithFilters(type, status,
                PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getAvailableVehicles() {
        return vehicleRepository.findByStatus(VehicleStatus.AVAILABLE).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getFleetPositions() {
        return vehicleRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {
        if (vehicleRepository.existsByVehicleNumber(request.getVehicleNumber())) {
            throw new DuplicateResourceException("Vehicle", "vehicleNumber", request.getVehicleNumber());
        }

        Vehicle vehicle = Vehicle.builder()
                .vehicleNumber(request.getVehicleNumber())
                .type(request.getType())
                .capacity(request.getCapacity())
                .status(request.getStatus() != null ? request.getStatus() : VehicleStatus.AVAILABLE)
                .currentLocation(request.getCurrentLocation())
                .make(request.getMake())
                .model(request.getModel())
                .year(request.getYear())
                .odometerKm(request.getOdometerKm())
                .lastServiceDate(request.getLastServiceDate())
                .nextServiceDueKm(request.getNextServiceDueKm())
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle created: {} ({})", saved.getVehicleNumber(), saved.getId());
        VehicleResponse response = toResponse(saved);
        auditLogService.log("Vehicle", saved.getId().toString(), AuditAction.CREATE, null, response);
        return response;
    }

    @Transactional
    public VehicleResponse updateVehicle(UUID id, VehicleRequest request) {
        Vehicle vehicle = findById(id);

        vehicleRepository.findByVehicleNumber(request.getVehicleNumber())
                .filter(v -> !v.getId().equals(id))
                .ifPresent(v -> {
                    throw new DuplicateResourceException("Vehicle", "vehicleNumber", request.getVehicleNumber());
                });

        vehicle.setVehicleNumber(request.getVehicleNumber());
        vehicle.setType(request.getType());
        vehicle.setCapacity(request.getCapacity());
        if (request.getStatus() != null) vehicle.setStatus(request.getStatus());
        vehicle.setCurrentLocation(request.getCurrentLocation());
        vehicle.setMake(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setYear(request.getYear());
        if (request.getOdometerKm() != null) vehicle.setOdometerKm(request.getOdometerKm());
        if (request.getLastServiceDate() != null) vehicle.setLastServiceDate(request.getLastServiceDate());
        if (request.getNextServiceDueKm() != null) vehicle.setNextServiceDueKm(request.getNextServiceDueKm());

        log.info("Vehicle updated: {}", id);
        return toResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void deleteVehicle(UUID id) {
        Vehicle vehicle = findById(id);

        boolean hasActiveTrip = tripRepository.existsByVehicleIdAndStatusIn(
                id, List.of(TripStatus.PLANNED, TripStatus.IN_PROGRESS));
        if (hasActiveTrip) {
            throw new BadRequestException("Cannot delete vehicle assigned to an active trip");
        }

        vehicleRepository.delete(vehicle);
        log.info("Vehicle deleted: {}", id);
    }

    Vehicle findById(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", id));
    }

    @Transactional
    public VehicleResponse updateLocation(UUID id, Double latitude, Double longitude) {
        Vehicle vehicle = findById(id);
        vehicle.setLatitude(latitude);
        vehicle.setLongitude(longitude);
        return toResponse(vehicleRepository.save(vehicle));
    }

    private VehicleResponse toResponse(Vehicle v) {
        return VehicleResponse.builder()
                .id(v.getId())
                .vehicleNumber(v.getVehicleNumber())
                .type(v.getType())
                .capacity(v.getCapacity())
                .status(v.getStatus())
                .currentLocation(v.getCurrentLocation())
                .make(v.getMake())
                .model(v.getModel())
                .year(v.getYear())
                .latitude(v.getLatitude())
                .longitude(v.getLongitude())
                .odometerKm(v.getOdometerKm())
                .lastServiceDate(v.getLastServiceDate())
                .nextServiceDueKm(v.getNextServiceDueKm())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
