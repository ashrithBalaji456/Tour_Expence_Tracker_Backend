package com.tripexpense.tracker.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripExpenseHistoryItemDto {
    private Long id;
    private String sourceType; // "ACTIVE" or "PRE_TRIP"
    private String title;
    private BigDecimal amount;
    private String category;
    private String paidBy;
    private LocalDateTime expenseDateTime;
    private String notes;
}

