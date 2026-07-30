package com.tripexpense.tracker.service;

import com.tripexpense.tracker.dto.ExpenseRequest;
import com.tripexpense.tracker.dto.ExpenseResponse;
import com.tripexpense.tracker.entity.Category;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseService {

    ExpenseResponse createExpense(ExpenseRequest request);

    List<ExpenseResponse> getAllExpenses();

    ExpenseResponse getExpenseById(Long id);

    List<ExpenseResponse> getExpensesByCategory(Category category);

    List<ExpenseResponse> getExpensesByDate(LocalDate date);

    ExpenseResponse updateExpense(Long id, ExpenseRequest request);

    void deleteExpense(Long id);
}
