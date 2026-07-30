package com.tripexpense.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreTripMemberSummaryDto {
    private String memberName;
    private BigDecimal budgetLimit;
    private BigDecimal totalSpent;
    private BigDecimal share;
    private BigDecimal netBalance; // positive = owed money, negative = owes money
    private boolean isSpentOverBudget;
    private boolean isShareOverBudget;
}
