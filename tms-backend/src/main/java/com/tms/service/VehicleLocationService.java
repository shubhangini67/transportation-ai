package com.tms.service;

import com.tms.dto.request.LocationUpdateRequest;
import com.tms.dto.response.VehicleLocationResponse;
import com.tms.entity.Vehicle;
import com.tms.entity.VehicleLocationHistory;
import com.tms.repository.VehicleLocationHistoryRepository;
import com.tms.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleLocationService {

    private final VehicleLocationHistoryRepository locationRepo;
    private final VehicleRepository vehicleRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final GeofenceService geofenceService;

    @Transactional
    public VehicleLocationResponse processLocationUpdate(LocationUpdateRequest req) {
        VehicleLocationHistory loc = VehicleLocationHistory.builder()
                .vehicleId(req.getVehicleId())
                .tripId(req.getTripId())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .speed(req.getSpeed())
                .heading(req.getHeading())
                .recordedAt(LocalDateTime.now())
                .build();
        locationRepo.save(loc);

        // Update vehicle's current GPS position
        vehicleRepo.findById(req.getVehicleId()).ifPresent(v -> {
            v.setLatitude(req.getLatitude());
            v.setLongitude(req.getLongitude());
            vehicleRepo.save(v);
        });

        // Check geofence boundaries
        try {
            geofenceService.checkGeofences(req.getVehicleId(), req.getTripId(), req.getLatitude(), req.getLongitude());
        } catch (Exception e) {
            log.warn("Geofence check failed for vehicle {}: {}", req.getVehicleId(), e.getMessage());
        }

        VehicleLocationResponse response = toResponse(loc);
        messagingTemplate.convertAndSend("/topic/vehicle/" + req.getVehicleId() + "/location", response);
        log.debug("Location broadcast for vehicle {}", req.getVehicleId());
        return response;
    }

    @Transactional(readOnly = true)
    public List<VehicleLocationResponse> getRouteHistory(UUID tripId) {
        return locationRepo.findByTripIdOrderByRecordedAtAsc(tripId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VehicleLocationResponse> getVehicleLocationHistory(UUID vehicleId, LocalDateTime from, LocalDateTime to) {
        return locationRepo.findByVehicleIdAndRecordedAtBetweenOrderByRecordedAtAsc(vehicleId, from, to)
                .stream().map(this::toResponse).toList();
    }

    private VehicleLocationResponse toResponse(VehicleLocationHistory l) {
        return VehicleLocationResponse.builder()
                .id(l.getId()).vehicleId(l.getVehicleId()).tripId(l.getTripId())
                .latitude(l.getLatitude()).longitude(l.getLongitude())
                .speed(l.getSpeed()).heading(l.getHeading())
                .recordedAt(l.getRecordedAt()).build();
    }
}

