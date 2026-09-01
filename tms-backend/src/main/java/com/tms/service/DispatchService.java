package com.tms.service;

import com.tms.dto.request.DispatchSnapshotRequest;
import com.tms.dto.response.DispatchPlanResponse;
import com.tms.entity.Driver;
import com.tms.entity.Route;
import com.tms.entity.Vehicle;
import com.tms.enums.DriverStatus;
import com.tms.enums.TripStatus;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.DriverRepository;
import com.tms.repository.RouteRepository;
import com.tms.repository.TripRepository;
import com.tms.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchService {

    private static final List<TripStatus> BUSY_STATUSES = List.of(TripStatus.PLANNED, TripStatus.IN_PROGRESS);

    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final DotNetReportsClient dotNetReportsClient;

    @Transactional(readOnly = true)
    public DispatchPlanResponse recommend(Long routeId, int requiredCapacity) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", routeId));

        DispatchSnapshotRequest snapshot = new DispatchSnapshotRequest();
        snapshot.setOrigin(route.getOrigin());
        snapshot.setDestination(route.getDestination());
        snapshot.setDistanceKm(route.getDistance() != null ? route.getDistance() : 0);
        snapshot.setEstimatedMinutes(route.getEstimatedTimeMinutes() != null ? route.getEstimatedTimeMinutes() : 0);
        snapshot.setRequiredCapacity(Math.max(requiredCapacity, 1));

        snapshot.setVehicles(vehicleRepository.findAll().stream().map(v -> toVehicle(v)).toList());
        snapshot.setDrivers(driverRepository.findAll().stream().map(d -> toDriver(d)).toList());

        if (dotNetReportsClient.isEnabled()) {
            try {
                DispatchPlanResponse fromDotNet = dotNetReportsClient.recommendDispatch(snapshot);
                if (fromDotNet != null) {
                    return fromDotNet;
                }
            } catch (RestClientException ex) {
                log.warn("Using Spring fallback dispatch: {}", ex.getMessage());
            }
        }
        return fallback(snapshot);
    }

    private DispatchSnapshotRequest.DispatchVehicle toVehicle(Vehicle v) {
        DispatchSnapshotRequest.DispatchVehicle dto = new DispatchSnapshotRequest.DispatchVehicle();
        dto.setId(v.getId().toString());
        dto.setNumber(v.getVehicleNumber());
        dto.setType(v.getType().name());
        dto.setCapacity(v.getCapacity());
        dto.setStatus(v.getStatus().name());
        dto.setCurrentLocation(v.getCurrentLocation());
        dto.setBusyOnTrip(tripRepository.existsByVehicleIdAndStatusIn(v.getId(), BUSY_STATUSES));
        return dto;
    }

    private DispatchSnapshotRequest.DispatchDriver toDriver(Driver d) {
        DispatchSnapshotRequest.DispatchDriver dto = new DispatchSnapshotRequest.DispatchDriver();
        dto.setId(d.getId().toString());
        dto.setName(d.getName());
        dto.setStatus(d.getStatus().name());
        dto.setBusyOnTrip(tripRepository.existsByDriverIdAndStatusIn(d.getId(), BUSY_STATUSES));
        return dto;
    }

    private DispatchPlanResponse fallback(DispatchSnapshotRequest snapshot) {
        DispatchPlanResponse response = new DispatchPlanResponse();
        response.setEngine("Spring Boot (fallback — start tms-dotnet-reports for ASP.NET dispatch)");
        response.setRouteLabel(snapshot.getOrigin() + " → " + snapshot.getDestination());
        snapshot.getVehicles().stream()
                .filter(v -> !"MAINTENANCE".equals(v.getStatus()) && !v.isBusyOnTrip())
                .filter(v -> v.getCapacity() >= snapshot.getRequiredCapacity())
                .findFirst()
                .ifPresent(v -> snapshot.getDrivers().stream()
                        .filter(d -> DriverStatus.ACTIVE.name().equals(d.getStatus()) && !d.isBusyOnTrip())
                        .findFirst()
                        .ifPresent(d -> {
                            DispatchPlanResponse.DispatchRecommendation rec = new DispatchPlanResponse.DispatchRecommendation();
                            rec.setRank(1);
                            rec.setScore(50);
                            rec.setVehicleId(v.getId());
                            rec.setVehicleNumber(v.getNumber());
                            rec.setVehicleType(v.getType());
                            rec.setDriverId(d.getId());
                            rec.setDriverName(d.getName());
                            rec.setReason("Local fallback pairing. Start the .NET service for scored recommendations.");
                            response.getRecommendations().add(rec);
                        }));
        return response;
    }
}
