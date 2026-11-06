package com.tripexpense.tracker.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberExpenseAllocationDto {
    private Long id;
    private Long expenseId;
    private String sourceType; // "ACTIVE" or "PRE_TRIP"
    private String title;
    private BigDecimal originalAmount;
    private BigDecimal allocatedAmount;
    private Integer daysCount;
    private Integer totalDays;
    private String notes;
}

