package com.tripexpense.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberExitBillDto {
    private Long memberId;
    private String memberName;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private Boolean isDeparted;
    private Boolean settledOnDeparture;
    private List<MemberExpenseAllocationDto> historyAllocations;
    private List<ExitBillItemDto> stayExpenses;
    private BigDecimal totalAllocatedFromHistory;
    private BigDecimal totalStayExpensesShare;
    private BigDecimal totalOwed;
    private BigDecimal totalPaid;
    private BigDecimal netDue; // positive = owes group/host, negative = owed refund
    private String suggestedPayee;
}

