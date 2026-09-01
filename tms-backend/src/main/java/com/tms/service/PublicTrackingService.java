package com.tms.service;

import com.tms.dto.response.PublicTrackingResponse;
import com.tms.entity.Trip;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicTrackingService {

    private final TripRepository tripRepository;

    @Transactional(readOnly = true)
    public PublicTrackingResponse track(String token) {
        Trip trip = tripRepository.findByTrackingToken(token == null ? "" : token.trim().toUpperCase())
                .orElseGet(() -> tripRepository.findByTrackingToken(token == null ? "" : token.trim())
                        .orElseThrow(() -> new ResourceNotFoundException("Shipment", "token", token)));
        String routeLabel = trip.getRoute() != null
                ? trip.getRoute().getOrigin() + " → " + trip.getRoute().getDestination()
                : "Route assigned on dispatch";
        List<String> hints = trip.getLorryReceipts() == null ? List.of()
                : trip.getLorryReceipts().stream()
                    .map(lr -> lr.getLrNumber() + " · " + lr.getMaterial())
                    .toList();
        return PublicTrackingResponse.builder()
                .token(token)
                .vehicleNumber(trip.getVehicle().getVehicleNumber())
                .driverName(trip.getDriver().getName())
                .routeLabel(routeLabel)
                .status(trip.getStatus().name())
                .latitude(trip.getVehicle().getLatitude())
                .longitude(trip.getVehicle().getLongitude())
                .currentLocation(trip.getVehicle().getCurrentLocation())
                .startTime(trip.getStartTime())
                .lastUpdate(trip.getVehicle().getUpdatedAt())
                .consignmentHints(hints)
                .build();
    }
}
