package com.tms.service;

import com.tms.dto.request.TripRequest;
import com.tms.dto.response.TripResponse;
import com.tms.entity.Driver;
import com.tms.entity.LorryReceipt;
import com.tms.entity.Route;
import com.tms.entity.Trip;
import com.tms.entity.Vehicle;
import com.tms.enums.DriverStatus;
import com.tms.enums.TripStatus;
import com.tms.enums.UserRole;
import com.tms.enums.VehicleStatus;
import com.tms.enums.AuditAction;
import com.tms.enums.WebhookEventType;
import com.tms.enums.NotificationType;
import com.tms.exception.BadRequestException;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.LorryReceiptRepository;
import com.tms.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final LorryReceiptRepository lrRepository;
    private final VehicleService vehicleService;
    private final DriverService driverService;
    private final RouteService routeService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;
    private final WebhookDispatchService webhookDispatchService;
    private final NotificationService notificationService;
    private final com.tms.repository.UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<TripResponse> getAllTrips(int page, int size, TripStatus status,
                                          LocalDateTime startFrom, LocalDateTime startTo) {
        log.debug("Fetching trips page={} size={} status={} startFrom={} startTo={}", page, size, status, startFrom, startTo);
        return tripRepository.findWithFilters(status, startFrom, startTo,
                PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TripResponse getTripById(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<TripResponse> getTripsByStatus(TripStatus status) {
        return tripRepository.findByStatus(status).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TripResponse> getRecentTrips() {
        return tripRepository.findRecentTrips().stream().map(this::toResponse).toList();
    }

    @Transactional
    public TripResponse createTrip(TripRequest request) {
        Vehicle vehicle = vehicleService.findById(request.getVehicleId());
        Driver driver = driverService.findById(request.getDriverId());

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new BadRequestException("Vehicle is not available. Current status: " + vehicle.getStatus());
        }
        if (driver.getStatus() != DriverStatus.ACTIVE) {
            throw new BadRequestException("Driver is not active. Current status: " + driver.getStatus());
        }

        // Check driver isn't already on an active trip
        boolean driverBusy = tripRepository.existsByDriverIdAndStatusIn(
                driver.getId(), List.of(TripStatus.PLANNED, TripStatus.IN_PROGRESS));
        if (driverBusy) {
            throw new BadRequestException("Driver is already assigned to an active trip");
        }

        List<LorryReceipt> lrs = resolveLrs(request.getLrIds());

        Route route = request.getRouteId() != null ? routeService.findById(request.getRouteId()) : null;

        Trip trip = Trip.builder()
                .vehicle(vehicle)
                .driver(driver)
                .route(route)
                .status(TripStatus.PLANNED)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .notes(request.getNotes())
                .lorryReceipts(lrs)
                .trackingToken(java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                .build();

        // Mark vehicle as busy
        vehicle.setStatus(VehicleStatus.BUSY);

        Trip saved = tripRepository.save(trip);
        log.info("Trip created: {} vehicle={} driver={}", saved.getId(), vehicle.getVehicleNumber(), driver.getName());
        TripResponse response = toResponse(saved);
        auditLogService.log("Trip", saved.getId().toString(), AuditAction.CREATE, null, response);
        webhookDispatchService.dispatch(WebhookEventType.TRIP_CREATED, response);
        return response;
    }

    @Transactional
    public TripResponse updateTrip(UUID id, TripRequest request) {
        Trip trip = findById(id);

        if (trip.getStatus() == TripStatus.COMPLETED) {
            throw new BadRequestException("Cannot update a completed trip");
        }

        Vehicle newVehicle = vehicleService.findById(request.getVehicleId());
        Driver newDriver = driverService.findById(request.getDriverId());

        // If vehicle changed, release old and validate new
        if (!trip.getVehicle().getId().equals(request.getVehicleId())) {
            if (newVehicle.getStatus() != VehicleStatus.AVAILABLE) {
                throw new BadRequestException("New vehicle is not available");
            }
            trip.getVehicle().setStatus(VehicleStatus.AVAILABLE);
            newVehicle.setStatus(VehicleStatus.BUSY);
        }

        // If driver changed, validate new
        if (!trip.getDriver().getId().equals(request.getDriverId())) {
            if (newDriver.getStatus() != DriverStatus.ACTIVE) {
                throw new BadRequestException("New driver is not active");
            }
            boolean driverBusy = tripRepository.existsByDriverIdAndStatusIn(
                    newDriver.getId(), List.of(TripStatus.PLANNED, TripStatus.IN_PROGRESS));
            if (driverBusy) {
                throw new BadRequestException("New driver is already assigned to an active trip");
            }
        }

        List<LorryReceipt> lrs = resolveLrs(request.getLrIds());

        Route route = request.getRouteId() != null ? routeService.findById(request.getRouteId()) : null;

        trip.setVehicle(newVehicle);
        trip.setDriver(newDriver);
        trip.setRoute(route);
        trip.setStartTime(request.getStartTime());
        trip.setEndTime(request.getEndTime());
        trip.setNotes(request.getNotes());
        trip.setLorryReceipts(lrs);

        return toResponse(tripRepository.save(trip));
    }

    @Transactional
    public TripResponse updateTripStatus(UUID id, TripStatus newStatus) {
        Trip trip = findById(id);
        assertDriverOwnsTrip(trip);
        TripStatus currentStatus = trip.getStatus();

        validateStatusTransition(currentStatus, newStatus);

        trip.setStatus(newStatus);

        switch (newStatus) {
            case IN_PROGRESS -> trip.setStartTime(LocalDateTime.now());
            case COMPLETED -> {
                trip.setEndTime(LocalDateTime.now());
                trip.getVehicle().setStatus(VehicleStatus.AVAILABLE);
            }
            default -> { }
        }

        TripResponse response = toResponse(tripRepository.save(trip));
        auditLogService.log("Trip", id.toString(), AuditAction.UPDATE, currentStatus.name(), newStatus.name());
        emailService.sendTripStatusUpdate(trip, currentStatus.name());
        webhookDispatchService.dispatch(WebhookEventType.TRIP_STATUS_CHANGED, response);

        // Push in-app notification to all dispatchers about the status change
        userRepository.findAll().stream()
                .filter(u -> u.getRole().name().equals("DISPATCHER") || u.getRole().name().equals("ADMIN"))
                .forEach(u -> notificationService.createAndPush(u.getId(), u.getUsername(),
                        "Trip Status Updated",
                        "Trip " + trip.getVehicle().getVehicleNumber() + " changed to " + newStatus.name(),
                        NotificationType.TRIP_UPDATE, "/trips"));

        return response;
    }

    @Transactional
    public void deleteTrip(UUID id) {
        Trip trip = findById(id);
        if (trip.getStatus() == TripStatus.IN_PROGRESS) {
            throw new BadRequestException("Cannot delete an in-progress trip");
        }
        if (trip.getStatus() == TripStatus.PLANNED) {
            trip.getVehicle().setStatus(VehicleStatus.AVAILABLE);
        }
        tripRepository.delete(trip);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTrackingData(UUID id) {
        Trip trip = findById(id);
        Map<String, Object> tracking = new LinkedHashMap<>();
        tracking.put("tripId", trip.getId());
        tracking.put("vehicleId", trip.getVehicle().getId());
        tracking.put("vehicleNumber", trip.getVehicle().getVehicleNumber());
        tracking.put("driverName", trip.getDriver().getName());
        tracking.put("status", trip.getStatus().name());
        tracking.put("vehicleLatitude", trip.getVehicle().getLatitude());
        tracking.put("vehicleLongitude", trip.getVehicle().getLongitude());
        tracking.put("startTime", trip.getStartTime());
        tracking.put("endTime", trip.getEndTime());
        return tracking;
    }

    Trip findById(UUID id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", "id", id));
    }

    private void assertDriverOwnsTrip(Trip trip) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return;
        }
        userRepository.findByUsername(auth.getName()).ifPresent(user -> {
            if (user.getRole() == UserRole.DRIVER) {
                String loginEmail = user.getEmail();
                String driverEmail = trip.getDriver().getEmail();
                if (loginEmail == null || driverEmail == null || !loginEmail.equalsIgnoreCase(driverEmail)) {
                    throw new AccessDeniedException("Drivers can only start or complete their own trips");
                }
            }
        });
    }

    private void validateStatusTransition(TripStatus current, TripStatus next) {
        boolean valid = switch (current) {
            case PLANNED -> next == TripStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == TripStatus.COMPLETED;
            default -> false;
        };
        if (!valid) {
            throw new BadRequestException(
                    String.format("Invalid status transition from %s to %s", current, next));
        }
    }

    private List<LorryReceipt> resolveLrs(List<UUID> lrIds) {
        if (lrIds == null || lrIds.isEmpty()) return new ArrayList<>();
        List<LorryReceipt> lrs = lrRepository.findAllById(lrIds);
        if (lrs.size() != lrIds.size()) {
            throw new BadRequestException("One or more Lorry Receipt IDs are invalid");
        }
        return lrs;
    }

    TripResponse toResponse(Trip t) {
        List<TripResponse.LrSummary> lrSummaries = t.getLorryReceipts() != null
                ? t.getLorryReceipts().stream().map(lr -> TripResponse.LrSummary.builder()
                        .id(lr.getId())
                        .lrNumber(lr.getLrNumber())
                        .origin(lr.getOrigin())
                        .destination(lr.getDestination())
                        .status(lr.getStatus().name())
                        .build()).toList()
                : Collections.emptyList();

        return TripResponse.builder()
                .id(t.getId())
                .vehicleId(t.getVehicle().getId())
                .vehicleNumber(t.getVehicle().getVehicleNumber())
                .vehicleType(t.getVehicle().getType())
                .driverId(t.getDriver().getId())
                .driverName(t.getDriver().getName())
                .routeId(t.getRoute() != null ? t.getRoute().getId() : null)
                .routeOrigin(t.getRoute() != null ? t.getRoute().getOrigin() : null)
                .routeDestination(t.getRoute() != null ? t.getRoute().getDestination() : null)
                .routeDistance(t.getRoute() != null ? t.getRoute().getDistance() : null)
                .routeEstimatedMinutes(t.getRoute() != null ? t.getRoute().getEstimatedTimeMinutes() : null)
                .status(t.getStatus())
                .startTime(t.getStartTime())
                .endTime(t.getEndTime())
                .notes(t.getNotes())
                .trackingToken(t.getTrackingToken())
                .lorryReceipts(lrSummaries)
                .createdAt(t.getCreatedAt())
                .build();
    }
}
