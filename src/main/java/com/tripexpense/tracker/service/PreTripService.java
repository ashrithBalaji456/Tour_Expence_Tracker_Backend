package com.tripexpense.tracker.service;

import com.tripexpense.tracker.dto.*;

import java.util.List;

public interface PreTripService {

    // Members
    List<PreTripMemberResponse> getAllMembers();
    PreTripMemberResponse saveMember(PreTripMemberRequest request);
    void deleteMember(Long id);

    // Expenses
    List<PreTripExpenseResponse> getAllExpenses();
    PreTripExpenseResponse createExpense(PreTripExpenseRequest request);
    PreTripExpenseResponse updateExpense(Long id, PreTripExpenseRequest request);
    void deleteExpense(Long id);

    // Summary Split Calculations
    PreTripSummaryDto getSummary();
}
