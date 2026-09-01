package com.tms.service;

import com.tms.dto.request.GeofenceRequest;
import com.tms.dto.response.GeofenceEventResponse;
import com.tms.dto.response.GeofenceResponse;
import com.tms.entity.Geofence;
import com.tms.entity.GeofenceEvent;
import com.tms.entity.User;
import com.tms.enums.GeofenceEventType;
import com.tms.enums.GeofenceType;
import com.tms.enums.NotificationType;
import com.tms.enums.UserRole;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.GeofenceEventRepository;
import com.tms.repository.GeofenceRepository;
import com.tms.repository.UserRepository;
import com.tms.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeofenceService {

    private final GeofenceRepository geofenceRepo;
    private final GeofenceEventRepository eventRepo;
    private final VehicleRepository vehicleRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    // ───── CRUD ─────

    @Transactional(readOnly = true)
    public Page<GeofenceResponse> getAll(int page, int size) {
        return geofenceRepo.findAll(PageRequest.of(page, size)).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public GeofenceResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional
    public GeofenceResponse create(GeofenceRequest req) {
        Geofence g = Geofence.builder()
                .name(req.getName())
                .description(req.getDescription())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .radiusMeters(req.getRadiusMeters())
                .type(req.getType() != null ? req.getType() : GeofenceType.CUSTOM)
                .active(req.getActive() != null ? req.getActive() : true)
                .build();
        return toResponse(geofenceRepo.save(g));
    }

    @Transactional
    public GeofenceResponse update(UUID id, GeofenceRequest req) {
        Geofence g = findById(id);
        g.setName(req.getName());
        g.setDescription(req.getDescription());
        g.setLatitude(req.getLatitude());
        g.setLongitude(req.getLongitude());
        g.setRadiusMeters(req.getRadiusMeters());
        if (req.getType() != null) g.setType(req.getType());
        if (req.getActive() != null) g.setActive(req.getActive());
        return toResponse(geofenceRepo.save(g));
    }

    @Transactional
    public void delete(UUID id) {
        Geofence g = findById(id);
        geofenceRepo.delete(g);
    }

    @Transactional(readOnly = true)
    public Page<GeofenceEventResponse> getEventsForGeofence(UUID geofenceId, int page, int size) {
        return eventRepo.findByGeofenceIdOrderByEventTimeDesc(geofenceId, PageRequest.of(page, size))
                .map(this::toEventResponse);
    }

    @Transactional(readOnly = true)
    public Page<GeofenceEventResponse> getEventsForVehicle(UUID vehicleId, int page, int size) {
        return eventRepo.findByVehicleIdOrderByEventTimeDesc(vehicleId, PageRequest.of(page, size))
                .map(this::toEventResponse);
    }

    @Transactional(readOnly = true)
    public Page<GeofenceEventResponse> getRecentEvents(int page, int size) {
        return eventRepo.findAllByOrderByEventTimeDesc(PageRequest.of(page, size)).map(this::toEventResponse);
    }

    // ───── Geofence checking (called from VehicleLocationService) ─────

    @Transactional
    public void checkGeofences(UUID vehicleId, UUID tripId, double lat, double lng) {
        List<Geofence> activeGeofences = geofenceRepo.findByActiveTrue();
        for (Geofence fence : activeGeofences) {
            double distance = haversine(lat, lng, fence.getLatitude(), fence.getLongitude());
            boolean isInsideNow = distance <= fence.getRadiusMeters();

            Optional<GeofenceEvent> lastEvent = eventRepo.findFirstByVehicleIdAndGeofenceIdOrderByEventTimeDesc(vehicleId, fence.getId());
            boolean wasInside = lastEvent.map(e -> e.getEventType() == GeofenceEventType.ENTER).orElse(false);

            if (isInsideNow && !wasInside) {
                // Vehicle entered geofence
                createEventAndNotify(fence, vehicleId, tripId, lat, lng, GeofenceEventType.ENTER);
            } else if (!isInsideNow && wasInside) {
                // Vehicle exited geofence
                createEventAndNotify(fence, vehicleId, tripId, lat, lng, GeofenceEventType.EXIT);
            }
        }
    }

    private void createEventAndNotify(Geofence fence, UUID vehicleId, UUID tripId, double lat, double lng, GeofenceEventType eventType) {
        GeofenceEvent event = GeofenceEvent.builder()
                .geofenceId(fence.getId())
                .vehicleId(vehicleId)
                .tripId(tripId)
                .eventType(eventType)
                .latitude(lat)
                .longitude(lng)
                .eventTime(LocalDateTime.now())
                .build();
        eventRepo.save(event);

        String vehicleNumber = vehicleRepo.findById(vehicleId)
                .map(v -> v.getVehicleNumber()).orElse("Unknown");
        String action = eventType == GeofenceEventType.ENTER ? "entered" : "exited";
        String title = "Geofence " + action + ": " + fence.getName();
        String message = "Vehicle " + vehicleNumber + " has " + action + " geofence '" + fence.getName() + "'";

        // Notify all admins and dispatchers
        List<User> adminsAndDispatchers = userRepo.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ADMIN || u.getRole() == UserRole.DISPATCHER)
                .filter(User::getActive)
                .toList();

        for (User u : adminsAndDispatchers) {
            notificationService.createAndPush(u.getId(), u.getUsername(), title, message,
                    NotificationType.GEOFENCE_ALERT, "/geofences");
        }

        log.info("Geofence event: vehicle {} {} '{}'", vehicleNumber, action, fence.getName());
    }

    // ───── Haversine formula ─────

    private double haversine(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    // ───── Helpers ─────

    private Geofence findById(UUID id) {
        return geofenceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Geofence", "id", id));
    }

    private GeofenceResponse toResponse(Geofence g) {
        return GeofenceResponse.builder()
                .id(g.getId()).name(g.getName()).description(g.getDescription())
                .latitude(g.getLatitude()).longitude(g.getLongitude())
                .radiusMeters(g.getRadiusMeters()).type(g.getType()).active(g.getActive())
                .createdAt(g.getCreatedAt()).updatedAt(g.getUpdatedAt())
                .build();
    }

    private GeofenceEventResponse toEventResponse(GeofenceEvent e) {
        String geofenceName = geofenceRepo.findById(e.getGeofenceId())
                .map(Geofence::getName).orElse("Unknown");
        String vehicleNumber = vehicleRepo.findById(e.getVehicleId())
                .map(v -> v.getVehicleNumber()).orElse("—");
        return GeofenceEventResponse.builder()
                .id(e.getId()).geofenceId(e.getGeofenceId()).geofenceName(geofenceName)
                .vehicleId(e.getVehicleId()).vehicleNumber(vehicleNumber).tripId(e.getTripId())
                .eventType(e.getEventType()).latitude(e.getLatitude()).longitude(e.getLongitude())
                .eventTime(e.getEventTime())
                .build();
    }
}

