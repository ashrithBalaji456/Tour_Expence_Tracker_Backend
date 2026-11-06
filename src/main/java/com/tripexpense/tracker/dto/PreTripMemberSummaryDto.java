package com.tripexpense.tracker.dto;

import com.tripexpense.tracker.entity.SplitMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreTripMemberSummaryDto {
    private Long memberId;
    private String memberName;
    private BigDecimal budgetLimit;
    private BigDecimal totalSpent;
    private BigDecimal share;
    private BigDecimal netBalance; // positive = owed money, negative = owes money
    private boolean isSpentOverBudget;
    private boolean isShareOverBudget;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private SplitMode splitMode;
    private Boolean isDeparted;
    private Boolean settledOnDeparture;
    private int eligibleExpenseCount;
    private int allocatedItemCount;
}
