package com.tms.service;

import com.tms.dto.response.FuelAnalyticsResponse;
import com.tms.dto.response.FuelTrendPoint;
import com.tms.dto.response.VehicleFuelBreakdown;
import com.tms.entity.Expense;
import com.tms.entity.Trip;
import com.tms.enums.ExpenseCategory;
import com.tms.repository.ExpenseRepository;
import com.tms.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FuelAnalyticsService {

    private final ExpenseRepository expenseRepo;
    private final TripRepository tripRepo;

    @Transactional(readOnly = true)
    public FuelAnalyticsResponse getAnalytics(LocalDate from, LocalDate to) {
        List<Expense> fuelExpenses = expenseRepo.findByCategoryAndDateRange(ExpenseCategory.FUEL, from, to);
        List<Trip> completedTrips = tripRepo.findCompletedTripsWithRouteInRange(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        BigDecimal totalFuelSpend = fuelExpenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Build per-vehicle distance map from completed trips
        Map<UUID, Double> vehicleDistanceMap = new HashMap<>();
        Map<UUID, Long> vehicleTripCountMap = new HashMap<>();
        for (Trip t : completedTrips) {
            UUID vid = t.getVehicle().getId();
            vehicleDistanceMap.merge(vid, t.getRoute().getDistance(), Double::sum);
            vehicleTripCountMap.merge(vid, 1L, Long::sum);
        }

        double totalDistance = vehicleDistanceMap.values().stream().mapToDouble(Double::doubleValue).sum();
        BigDecimal avgCostPerKm = totalDistance > 0
                ? totalFuelSpend.divide(BigDecimal.valueOf(totalDistance), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Per-vehicle breakdown
        Map<UUID, List<Expense>> expensesByVehicle = fuelExpenses.stream()
                .filter(e -> e.getVehicle() != null)
                .collect(Collectors.groupingBy(e -> e.getVehicle().getId()));

        Set<UUID> allVehicleIds = new HashSet<>(expensesByVehicle.keySet());
        allVehicleIds.addAll(vehicleDistanceMap.keySet());

        List<VehicleFuelBreakdown> breakdowns = allVehicleIds.stream().map(vid -> {
            List<Expense> vExpenses = expensesByVehicle.getOrDefault(vid, List.of());
            BigDecimal vFuelCost = vExpenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            double vDistance = vehicleDistanceMap.getOrDefault(vid, 0.0);
            BigDecimal vCostPerKm = vDistance > 0
                    ? vFuelCost.divide(BigDecimal.valueOf(vDistance), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            String vehicleNumber = vExpenses.isEmpty() ? "Unknown" : vExpenses.get(0).getVehicle().getVehicleNumber();
            if (vehicleNumber.equals("Unknown") && vehicleDistanceMap.containsKey(vid)) {
                vehicleNumber = completedTrips.stream()
                        .filter(t -> t.getVehicle().getId().equals(vid))
                        .findFirst()
                        .map(t -> t.getVehicle().getVehicleNumber())
                        .orElse("Unknown");
            }
            return VehicleFuelBreakdown.builder()
                    .vehicleId(vid)
                    .vehicleNumber(vehicleNumber)
                    .totalFuelCost(vFuelCost)
                    .totalDistanceKm(vDistance)
                    .costPerKm(vCostPerKm)
                    .tripCount(vehicleTripCountMap.getOrDefault(vid, 0L))
                    .fuelExpenseCount(vExpenses.size())
                    .build();
        }).sorted(Comparator.comparing(VehicleFuelBreakdown::getTotalFuelCost).reversed())
                .collect(Collectors.toList());

        // Monthly trend
        List<FuelTrendPoint> monthlyTrend = buildMonthlyTrend(fuelExpenses, completedTrips, from, to);

        return FuelAnalyticsResponse.builder()
                .totalFuelSpend(totalFuelSpend)
                .averageCostPerKm(avgCostPerKm)
                .totalDistanceKm(totalDistance)
                .fuelTransactionCount(fuelExpenses.size())
                .vehicleBreakdowns(breakdowns)
                .monthlyTrend(monthlyTrend)
                .build();
    }

    @Transactional(readOnly = true)
    public VehicleFuelBreakdown getVehicleDetail(UUID vehicleId, LocalDate from, LocalDate to) {
        List<Expense> fuelExpenses = expenseRepo.findByVehicleIdAndCategoryAndExpenseDateBetween(
                vehicleId, ExpenseCategory.FUEL, from, to);
        List<Trip> trips = tripRepo.findCompletedTripsWithRouteByVehicle(
                vehicleId, from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        BigDecimal totalFuelCost = fuelExpenses.stream()
                .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        double totalDistance = trips.stream()
                .mapToDouble(t -> t.getRoute().getDistance()).sum();
        BigDecimal costPerKm = totalDistance > 0
                ? totalFuelCost.divide(BigDecimal.valueOf(totalDistance), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String vehicleNumber = fuelExpenses.isEmpty()
                ? (trips.isEmpty() ? "Unknown" : trips.get(0).getVehicle().getVehicleNumber())
                : fuelExpenses.get(0).getVehicle().getVehicleNumber();

        return VehicleFuelBreakdown.builder()
                .vehicleId(vehicleId)
                .vehicleNumber(vehicleNumber)
                .totalFuelCost(totalFuelCost)
                .totalDistanceKm(totalDistance)
                .costPerKm(costPerKm)
                .tripCount(trips.size())
                .fuelExpenseCount(fuelExpenses.size())
                .build();
    }

    private List<FuelTrendPoint> buildMonthlyTrend(List<Expense> fuelExpenses, List<Trip> trips, LocalDate from, LocalDate to) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, BigDecimal> monthSpend = new TreeMap<>();
        Map<String, Double> monthDistance = new TreeMap<>();

        // Initialize all months in range
        YearMonth start = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        for (YearMonth ym = start; !ym.isAfter(end); ym = ym.plusMonths(1)) {
            String key = ym.format(fmt);
            monthSpend.put(key, BigDecimal.ZERO);
            monthDistance.put(key, 0.0);
        }

        for (Expense e : fuelExpenses) {
            String key = YearMonth.from(e.getExpenseDate()).format(fmt);
            monthSpend.merge(key, e.getAmount(), BigDecimal::add);
        }
        for (Trip t : trips) {
            if (t.getEndTime() != null && t.getRoute() != null) {
                String key = YearMonth.from(t.getEndTime().toLocalDate()).format(fmt);
                monthDistance.merge(key, t.getRoute().getDistance(), Double::sum);
            }
        }

        return monthSpend.entrySet().stream().map(entry -> {
            String month = entry.getKey();
            BigDecimal spend = entry.getValue();
            double dist = monthDistance.getOrDefault(month, 0.0);
            BigDecimal cpk = dist > 0
                    ? spend.divide(BigDecimal.valueOf(dist), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            return FuelTrendPoint.builder()
                    .period(month).totalSpend(spend).totalDistanceKm(dist).costPerKm(cpk).build();
        }).collect(Collectors.toList());
    }
}

