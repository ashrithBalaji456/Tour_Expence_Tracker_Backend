package com.tripexpense.tracker.dto;

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
public class ExitBillItemDto {
    private Long id;
    private String title;
    private String category;
    private BigDecimal totalAmount;
    private int activeMemberCount;
    private BigDecimal memberShare;
    private LocalDateTime expenseDateTime;
    private String paidBy;
}
