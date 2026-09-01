package com.tms.repository;

import com.tms.entity.Expense;
import com.tms.enums.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findByTripId(UUID tripId);

    @Query("SELECT e FROM Expense e WHERE " +
            "(:category IS NULL OR e.category = :category) AND " +
            "e.expenseDate >= COALESCE(:from, e.expenseDate) AND " +
            "e.expenseDate <= COALESCE(:to, e.expenseDate)")
    Page<Expense> findWithFilters(ExpenseCategory category, LocalDate from, LocalDate to, Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.trip.id = :tripId")
    BigDecimal sumByTripId(UUID tripId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.expenseDate BETWEEN :from AND :to")
    BigDecimal sumInDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<Expense> findByVehicleIdAndCategoryAndExpenseDateBetween(UUID vehicleId, ExpenseCategory category, LocalDate from, LocalDate to);

    @Query("SELECT e FROM Expense e WHERE e.category = :category AND e.expenseDate BETWEEN :from AND :to")
    List<Expense> findByCategoryAndDateRange(@Param("category") ExpenseCategory category, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.category = 'FUEL' AND e.expenseDate BETWEEN :from AND :to")
    BigDecimal sumFuelInDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
