package com.tms.service;

import com.tms.dto.request.FleetSnapshotRequest;
import com.tms.dto.response.FleetInsightsResponse;
import com.tms.enums.DriverStatus;
import com.tms.enums.TripStatus;
import com.tms.enums.VehicleStatus;
import com.tms.repository.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final ExpenseRepository expenseRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final DotNetReportsClient dotNetReportsClient;

    @Data @Builder
    public static class ReportRow {
        private String label;
        private long count;
        private BigDecimal amount;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTripReport(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalTrips", tripRepository.countByCreatedAtBetween(start, end));
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TripStatus s : TripStatus.values()) {
            byStatus.put(s.name(), tripRepository.countByStatus(s));
        }
        report.put("byStatus", byStatus);
        report.put("totalBookings", bookingRepository.countBookingsInDateRange(start, end));
        report.put("totalExpenses", expenseRepository.sumInDateRange(from, to));
        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getVehicleUtilizationReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalVehicles", vehicleRepository.count());
        report.put("availableVehicles", vehicleRepository.countByStatus(com.tms.enums.VehicleStatus.AVAILABLE));
        report.put("busyVehicles", vehicleRepository.countByStatus(com.tms.enums.VehicleStatus.BUSY));
        report.put("maintenanceVehicles", vehicleRepository.countByStatus(com.tms.enums.VehicleStatus.MAINTENANCE));
        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDriverPerformanceReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalDrivers", driverRepository.count());
        report.put("activeDrivers", driverRepository.countByStatus(com.tms.enums.DriverStatus.ACTIVE));
        report.put("inactiveDrivers", driverRepository.countByStatus(com.tms.enums.DriverStatus.INACTIVE));
        return report;
    }

    /**
     * Builds a fleet snapshot in Spring Boot, then asks the ASP.NET Core
     * service to score it. If .NET is down, a simpler local fallback is used.
     */
    @Transactional(readOnly = true)
    public FleetInsightsResponse getFleetInsights() {
        FleetSnapshotRequest snapshot = buildFleetSnapshot();
        if (dotNetReportsClient.isEnabled()) {
            try {
                FleetInsightsResponse fromDotNet = dotNetReportsClient.analyzeFleet(snapshot);
                if (fromDotNet != null) {
                    return fromDotNet;
                }
            } catch (RestClientException ex) {
                log.warn("Using Spring fallback insights: {}", ex.getMessage());
            }
        }
        return fallbackInsights(snapshot);
    }

    private FleetSnapshotRequest buildFleetSnapshot() {
        FleetSnapshotRequest snap = new FleetSnapshotRequest();
        snap.setTotalVehicles((int) vehicleRepository.count());
        snap.setAvailableVehicles((int) vehicleRepository.countByStatus(VehicleStatus.AVAILABLE));
        snap.setBusyVehicles((int) vehicleRepository.countByStatus(VehicleStatus.BUSY));
        snap.setMaintenanceVehicles((int) vehicleRepository.countByStatus(VehicleStatus.MAINTENANCE));
        snap.setTotalDrivers((int) driverRepository.count());
        snap.setActiveDrivers((int) driverRepository.countByStatus(DriverStatus.ACTIVE));
        snap.setInactiveDrivers((int) driverRepository.countByStatus(DriverStatus.INACTIVE));
        snap.setTotalTrips((int) tripRepository.count());
        snap.setPlannedTrips((int) tripRepository.countByStatus(TripStatus.PLANNED));
        snap.setInProgressTrips((int) tripRepository.countByStatus(TripStatus.IN_PROGRESS));
        snap.setCompletedTrips((int) tripRepository.countByStatus(TripStatus.COMPLETED));
        snap.setTotalExpenses(expenseRepository.sumInDateRange(null, null));
        snap.setTotalBookings((int) bookingRepository.count());
        return snap;
    }

    private FleetInsightsResponse fallbackInsights(FleetSnapshotRequest snap) {
        int vehicles = Math.max(snap.getTotalVehicles(), 1);
        double util = 100.0 * snap.getBusyVehicles() / vehicles;
        double maint = 100.0 * snap.getMaintenanceVehicles() / vehicles;
        int drivers = Math.max(snap.getTotalDrivers(), 1);
        double driverAvail = 100.0 * snap.getActiveDrivers() / drivers;
        double expensePerTrip = snap.getTotalTrips() == 0 ? 0
                : snap.getTotalExpenses().divide(BigDecimal.valueOf(snap.getTotalTrips()), 2, RoundingMode.HALF_UP).doubleValue();
        return FleetInsightsResponse.builder()
                .engine("Spring Boot (fallback — start tms-dotnet-reports for ASP.NET insights)")
                .vehicleUtilizationPercent(Math.round(util * 10) / 10.0)
                .maintenanceLoadPercent(Math.round(maint * 10) / 10.0)
                .driverAvailabilityPercent(Math.round(driverAvail * 10) / 10.0)
                .expensePerTrip(expensePerTrip)
                .fleetHealthScore(70)
                .riskLevel("MEDIUM")
                .alerts(List.of("ASP.NET Core reports service is not running. Showing a basic Spring Boot fallback."))
                .recommendations(List.of("Start tms-dotnet-reports (port 5080) to get scored fleet insights from .NET."))
                .build();
    }

    public String exportCsv(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("Key,Value\n");
        data.forEach((key, value) -> {
            if (value instanceof Map) {
                ((Map<?, ?>) value).forEach((k, v) -> sb.append(key).append(".").append(k).append(",").append(v).append("\n"));
            } else {
                sb.append(key).append(",").append(value).append("\n");
            }
        });
        return sb.toString();
    }
}

