package com.tripexpense.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreTripExpenseRequest {
    private String title;
    private BigDecimal amount;
    private String spentBy;
    private LocalDate expenseDate;
    private String notes;
}
