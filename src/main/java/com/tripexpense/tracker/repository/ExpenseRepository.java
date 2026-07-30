package com.tripexpense.tracker.repository;

import com.tripexpense.tracker.entity.Category;
import com.tripexpense.tracker.entity.Expense;
import com.tripexpense.tracker.entity.TripGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByTripGroup(TripGroup tripGroup);

    List<Expense> findByCategoryAndTripGroup(Category category, TripGroup tripGroup);

    List<Expense> findByExpenseDateAndTripGroup(LocalDate expenseDate, TripGroup tripGroup);

    List<Expense> findByExpenseDateBetweenAndTripGroup(LocalDate startDate, LocalDate endDate, TripGroup tripGroup);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.tripGroup = :tripGroup")
    BigDecimal getTotalExpensesSum(@Param("tripGroup") TripGroup tripGroup);

    @Query("SELECT e.category, SUM(e.amount) FROM Expense e WHERE e.tripGroup = :tripGroup GROUP BY e.category")
    List<Object[]> getCategoryExpenseBreakdown(@Param("tripGroup") TripGroup tripGroup);
}
