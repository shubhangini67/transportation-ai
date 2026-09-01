package com.tms.service;

import com.tms.dto.response.MaintenanceAlertResponse;
import com.tms.entity.Vehicle;
import com.tms.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final VehicleRepository vehicleRepository;

    @Transactional(readOnly = true)
    public List<MaintenanceAlertResponse> getAlerts() {
        return vehicleRepository.findAll().stream()
                .map(this::toAlert)
                .filter(a -> !"OK".equals(a.getSeverity()))
                .sorted(Comparator.comparingInt((MaintenanceAlertResponse a) -> "OVERDUE".equals(a.getSeverity()) ? 2 : 1).reversed())
                .toList();
    }

    private MaintenanceAlertResponse toAlert(Vehicle v) {
        Integer odo = v.getOdometerKm();
        Integer due = v.getNextServiceDueKm();
        int remaining = (odo != null && due != null) ? due - odo : Integer.MAX_VALUE;
        long daysSinceService = v.getLastServiceDate() != null
                ? ChronoUnit.DAYS.between(v.getLastServiceDate(), LocalDate.now())
                : 0;

        String severity = "OK";
        String message = "Within service window";
        if (remaining <= 0) {
            severity = "OVERDUE";
            message = "Odometer has passed the service due km.";
        } else if (remaining <= 5000) {
            severity = "DUE_SOON";
            message = remaining + " km remaining before scheduled service.";
        } else if (daysSinceService >= 180) {
            severity = "DUE_SOON";
            message = daysSinceService + " days since last service (policy: 180 days).";
        }

        return MaintenanceAlertResponse.builder()
                .vehicleId(v.getId())
                .vehicleNumber(v.getVehicleNumber())
                .vehicleType(v.getType().name())
                .status(v.getStatus().name())
                .odometerKm(odo)
                .nextServiceDueKm(due)
                .kmRemaining(remaining == Integer.MAX_VALUE ? null : remaining)
                .lastServiceDate(v.getLastServiceDate())
                .severity(severity)
                .message(message)
                .build();
    }
}
