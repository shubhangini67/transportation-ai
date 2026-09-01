package com.tms.service;

import com.tms.dto.response.DriverScorecardResponse;
import com.tms.entity.Driver;
import com.tms.entity.Expense;
import com.tms.entity.Trip;
import com.tms.enums.ExpenseCategory;
import com.tms.enums.TripStatus;
import com.tms.repository.DriverRepository;
import com.tms.repository.ExpenseRepository;
import com.tms.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverPerformanceService {

    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public List<DriverScorecardResponse> scorecards() {
        return driverRepository.findAll().stream().map(this::score).toList();
    }

    private DriverScorecardResponse score(Driver driver) {
        List<Trip> trips = tripRepository.findByDriverId(driver.getId());
        List<Trip> completed = trips.stream().filter(t -> t.getStatus() == TripStatus.COMPLETED).toList();
        long delayed = completed.stream().filter(this::isDelayed).count();
        int onTime = completed.isEmpty() ? 100
                : (int) Math.round(100.0 * (completed.size() - delayed) / completed.size());

        BigDecimal fuel = completed.stream()
                .flatMap(t -> expenseRepository.findByTripId(t.getId()).stream())
                .filter(e -> e.getCategory() == ExpenseCategory.FUEL)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int score = Math.max(0, Math.min(100, onTime - (int) delayed * 3 + (int) Math.min(completed.size(), 10)));
        String band = score >= 85 ? "A" : score >= 70 ? "B" : score >= 55 ? "C" : "D";

        return DriverScorecardResponse.builder()
                .driverId(driver.getId())
                .driverName(driver.getName())
                .status(driver.getStatus().name())
                .completedTrips(completed.size())
                .delayedTrips(delayed)
                .onTimePercent(onTime)
                .fuelSpend(fuel)
                .score(score)
                .band(band)
                .build();
    }

    private boolean isDelayed(Trip trip) {
        if (trip.getStartTime() == null || trip.getEndTime() == null || trip.getRoute() == null
                || trip.getRoute().getEstimatedTimeMinutes() == null) {
            return false;
        }
        long actual = Duration.between(trip.getStartTime(), trip.getEndTime()).toMinutes();
        return actual > Math.round(trip.getRoute().getEstimatedTimeMinutes() * 1.15);
    }
}
