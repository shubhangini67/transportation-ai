package com.tms.service;

import com.tms.dto.response.OperationsAlertResponse;
import com.tms.entity.Trip;
import com.tms.enums.TripStatus;
import com.tms.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationsService {

    private final TripRepository tripRepository;

    @Transactional(readOnly = true)
    public List<OperationsAlertResponse> getAlerts() {
        LocalDateTime now = LocalDateTime.now();
        List<OperationsAlertResponse> alerts = new ArrayList<>();

        for (Trip trip : tripRepository.findAll()) {
            if (trip.getStatus() == TripStatus.PLANNED && trip.getStartTime() != null && trip.getStartTime().isBefore(now)) {
                alerts.add(alert(trip, "HIGH", "OVERDUE_START",
                        "Planned start time has passed — trip has not started.",
                        Duration.between(trip.getStartTime(), now).toMinutes()));
            }
            if (trip.getStatus() == TripStatus.IN_PROGRESS && trip.getStartTime() != null && trip.getRoute() != null
                    && trip.getRoute().getEstimatedTimeMinutes() != null) {
                long elapsed = Duration.between(trip.getStartTime(), now).toMinutes();
                int eta = trip.getRoute().getEstimatedTimeMinutes();
                if (elapsed > Math.round(eta * 1.15)) {
                    alerts.add(alert(trip, elapsed > eta * 1.5 ? "HIGH" : "MEDIUM", "DELAYED",
                            "In-progress trip is beyond 115% of the route ETA.",
                            elapsed - eta));
                }
            }
        }

        alerts.sort(Comparator
                .comparingInt((OperationsAlertResponse a) -> severityRank(a.getSeverity()))
                .thenComparingLong(OperationsAlertResponse::getMinutesOverdue)
                .reversed());
        return alerts;
    }

    private static int severityRank(String severity) {
        if ("HIGH".equals(severity)) return 2;
        if ("MEDIUM".equals(severity)) return 1;
        return 0;
    }

    private OperationsAlertResponse alert(Trip trip, String severity, String code, String message, long minutesOverdue) {
        String routeLabel = trip.getRoute() != null
                ? trip.getRoute().getOrigin() + " → " + trip.getRoute().getDestination()
                : "No route";
        return OperationsAlertResponse.builder()
                .tripId(trip.getId())
                .vehicleNumber(trip.getVehicle().getVehicleNumber())
                .driverName(trip.getDriver().getName())
                .routeLabel(routeLabel)
                .tripStatus(trip.getStatus())
                .severity(severity)
                .code(code)
                .message(message)
                .startTime(trip.getStartTime())
                .estimatedMinutes(trip.getRoute() != null ? trip.getRoute().getEstimatedTimeMinutes() : null)
                .minutesOverdue(Math.max(minutesOverdue, 0))
                .build();
    }
}
