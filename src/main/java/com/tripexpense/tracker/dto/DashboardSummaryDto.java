package com.tripexpense.tracker.dto;

import com.tripexpense.tracker.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDto {

    // Unified Budget Metrics
    private BigDecimal totalBudget;
    private BigDecimal totalSpent;
    private BigDecimal remainingBalance;
    private BigDecimal sharePerMember;

    // Splits & Settlements
    private List<PreTripMemberSummaryDto> memberSummaries;
    private List<PreTripTransferDto> transfers;

    // Original fields for details breakdown
    private BigDecimal totalFundsCollected;
    private BigDecimal totalExpensesSpent;
    private BigDecimal totalPreTripSpent;
    private BigDecimal totalPreTripBudget;

    private int totalExpenseCount;
    private Map<Category, BigDecimal> categoryBreakdown;
}
