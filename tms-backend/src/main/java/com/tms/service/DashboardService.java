package com.tms.service;

import com.tms.dto.response.DashboardResponse;
import com.tms.dto.response.DashboardTrendResponse;
import com.tms.enums.DriverStatus;
import com.tms.enums.TripStatus;
import com.tms.enums.VehicleStatus;
import com.tms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final LorryReceiptRepository lrRepository;
    private final TripService tripService;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        return DashboardResponse.builder()
                .totalTrips(tripRepository.count())
                .activeTrips(tripRepository.countByStatus(TripStatus.IN_PROGRESS))
                .completedTrips(tripRepository.countByStatus(TripStatus.COMPLETED))
                .availableVehicles(vehicleRepository.countByStatus(VehicleStatus.AVAILABLE))
                .totalVehicles(vehicleRepository.count())
                .totalDrivers(driverRepository.count())
                .activeDrivers(driverRepository.countByStatus(DriverStatus.ACTIVE))
                .totalLrs(lrRepository.count())
                .todayBookings(bookingRepository.countBookingsInDateRange(startOfDay, endOfDay))
                .totalBookings(bookingRepository.count())
                .recentTrips(tripService.getRecentTrips())
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardTrendResponse getDashboardTrends() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");
        LocalDate today = LocalDate.now();

        List<DashboardTrendResponse.TrendPoint> tripTrend = new ArrayList<>();
        List<DashboardTrendResponse.TrendPoint> bookingTrend = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);
            String label = date.format(fmt);

            long tripCount = tripRepository.countByCreatedAtBetween(dayStart, dayEnd);
            long bookingCount = bookingRepository.countBookingsInDateRange(dayStart, dayEnd);

            tripTrend.add(new DashboardTrendResponse.TrendPoint(label, tripCount));
            bookingTrend.add(new DashboardTrendResponse.TrendPoint(label, bookingCount));
        }

        Map<String, Long> tripsByStatus = new LinkedHashMap<>();
        for (TripStatus s : TripStatus.values()) {
            tripsByStatus.put(s.name(), tripRepository.countByStatus(s));
        }

        Map<String, Long> vehiclesByStatus = new LinkedHashMap<>();
        for (VehicleStatus s : VehicleStatus.values()) {
            vehiclesByStatus.put(s.name(), vehicleRepository.countByStatus(s));
        }

        return DashboardTrendResponse.builder()
                .tripTrend(tripTrend)
                .bookingTrend(bookingTrend)
                .tripsByStatus(tripsByStatus)
                .vehiclesByStatus(vehiclesByStatus)
                .build();
    }
}
