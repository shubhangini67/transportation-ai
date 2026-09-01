package com.tms.config;

import com.tms.dto.request.LocationUpdateRequest;
import com.tms.entity.Route;
import com.tms.entity.Trip;
import com.tms.entity.Vehicle;
import com.tms.enums.TripStatus;
import com.tms.repository.TripRepository;
import com.tms.service.VehicleLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Demo GPS feeder: walks in-progress trucks toward their route destination
 * and broadcasts over WebSocket so Live tracking looks real.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.gps.simulator", havingValue = "true", matchIfMissing = true)
public class GpsSimulator {

    private static final Map<String, double[]> CITIES = Map.ofEntries(
            Map.entry("delhi", new double[]{28.6139, 77.2090}),
            Map.entry("jaipur", new double[]{26.9124, 75.7873}),
            Map.entry("mumbai", new double[]{19.0760, 72.8777}),
            Map.entry("pune", new double[]{18.5204, 73.8567}),
            Map.entry("bengaluru", new double[]{12.9716, 77.5946}),
            Map.entry("bangalore", new double[]{12.9716, 77.5946}),
            Map.entry("chennai", new double[]{13.0827, 80.2707}),
            Map.entry("hyderabad", new double[]{17.3850, 78.4867}),
            Map.entry("vijayawada", new double[]{16.5062, 80.6480}),
            Map.entry("ahmedabad", new double[]{23.0225, 72.5714}),
            Map.entry("surat", new double[]{21.1702, 72.8311}),
            Map.entry("udaipur", new double[]{24.5854, 73.7125}),
            Map.entry("madurai", new double[]{9.9252, 78.1198}),
            Map.entry("kolkata", new double[]{22.5726, 88.3639}),
            Map.entry("bhubaneswar", new double[]{20.2961, 85.8245}),
            Map.entry("agra", new double[]{27.1767, 78.0081}),
            Map.entry("gurugram", new double[]{28.4595, 77.0266})
    );

    private final TripRepository tripRepository;
    private final VehicleLocationService vehicleLocationService;
    private final ConcurrentHashMap<UUID, Boolean> towardDestination = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 7000)
    @Transactional
    public void tick() {
        List<Trip> live = tripRepository.findLiveTripsForGps(TripStatus.IN_PROGRESS);
        for (Trip trip : live) {
            try {
                moveTrip(trip);
            } catch (Exception ex) {
                log.debug("GPS sim skip for trip {}: {}", trip.getId(), ex.getMessage());
            }
        }
    }

    private void moveTrip(Trip trip) {
        Vehicle vehicle = trip.getVehicle();
        Route route = trip.getRoute();
        if (vehicle == null || route == null) return;

        double[] origin = coords(route.getOrigin());
        double[] dest = coords(route.getDestination());
        if (origin == null || dest == null) return;

        boolean goingOut = towardDestination.getOrDefault(trip.getId(), true);
        double[] target = goingOut ? dest : origin;
        double lat = vehicle.getLatitude() != null ? vehicle.getLatitude() : origin[0];
        double lng = vehicle.getLongitude() != null ? vehicle.getLongitude() : origin[1];

        double dLat = target[0] - lat;
        double dLng = target[1] - lng;
        double dist = Math.hypot(dLat, dLng);
        if (dist < 0.05) {
            goingOut = !goingOut;
            towardDestination.put(trip.getId(), goingOut);
            target = goingOut ? dest : origin;
            dLat = target[0] - lat;
            dLng = target[1] - lng;
            dist = Math.hypot(dLat, dLng);
            if (dist < 0.001) return;
        }

        double step = Math.min(0.018, dist);
        double nLat = lat + (dLat / dist) * step;
        double nLng = lng + (dLng / dist) * step;
        double heading = (Math.toDegrees(Math.atan2(dLng, dLat)) + 360) % 360;
        double speed = 38 + (Math.abs(nLat * 100) % 22);

        vehicle.setCurrentLocation("En route " + route.getOrigin() + " → " + route.getDestination());

        LocationUpdateRequest req = new LocationUpdateRequest();
        req.setVehicleId(vehicle.getId());
        req.setTripId(trip.getId());
        req.setLatitude(nLat);
        req.setLongitude(nLng);
        req.setSpeed(speed);
        req.setHeading(heading);
        vehicleLocationService.processLocationUpdate(req);
    }

    private static double[] coords(String city) {
        if (city == null) return null;
        return CITIES.get(city.trim().toLowerCase());
    }
}
