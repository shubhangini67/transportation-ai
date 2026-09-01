package com.tms.service;

import com.tms.dto.request.ExpenseRequest;
import com.tms.dto.response.ExpenseResponse;
import com.tms.entity.Expense;
import com.tms.entity.Trip;
import com.tms.entity.Vehicle;
import com.tms.enums.ExpenseCategory;
import com.tms.exception.ResourceNotFoundException;
import com.tms.repository.ExpenseRepository;
import com.tms.repository.TripRepository;
import com.tms.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getAllExpenses(int page, int size, ExpenseCategory category, LocalDate from, LocalDate to) {
        return expenseRepository.findWithFilters(category, from, to,
                PageRequest.of(page, size, Sort.by("expenseDate").descending()))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalInRange(LocalDate from, LocalDate to) {
        return expenseRepository.sumInDateRange(from, to);
    }

    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest req) {
        Trip trip = req.getTripId() != null ? tripRepository.findById(req.getTripId()).orElse(null) : null;
        Vehicle vehicle = req.getVehicleId() != null ? vehicleRepository.findById(req.getVehicleId()).orElse(null) : null;

        Expense expense = Expense.builder()
                .trip(trip).vehicle(vehicle)
                .category(req.getCategory()).amount(req.getAmount())
                .description(req.getDescription()).expenseDate(req.getExpenseDate())
                .build();
        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse updateExpense(UUID id, ExpenseRequest req) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", id));
        expense.setCategory(req.getCategory());
        expense.setAmount(req.getAmount());
        expense.setDescription(req.getDescription());
        expense.setExpenseDate(req.getExpenseDate());
        if (req.getTripId() != null) expense.setTrip(tripRepository.findById(req.getTripId()).orElse(null));
        if (req.getVehicleId() != null) expense.setVehicle(vehicleRepository.findById(req.getVehicleId()).orElse(null));
        return toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void deleteExpense(UUID id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", id));
        expenseRepository.delete(expense);
    }

    private ExpenseResponse toResponse(Expense e) {
        return ExpenseResponse.builder()
                .id(e.getId())
                .tripId(e.getTrip() != null ? e.getTrip().getId() : null)
                .vehicleId(e.getVehicle() != null ? e.getVehicle().getId() : null)
                .vehicleNumber(e.getVehicle() != null ? e.getVehicle().getVehicleNumber() : null)
                .category(e.getCategory()).amount(e.getAmount())
                .description(e.getDescription()).expenseDate(e.getExpenseDate())
                .createdAt(e.getCreatedAt())
                .build();
    }
}

