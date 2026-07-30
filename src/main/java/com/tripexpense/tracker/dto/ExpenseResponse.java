package com.tripexpense.tracker.dto;

import com.tripexpense.tracker.entity.Category;
import com.tripexpense.tracker.entity.PaymentMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponse {

    private Long id;
    private String title;
    private BigDecimal amount;
    private Category category;
    private PaymentMode paymentMode;
    private String paidBy;
    private LocalDate expenseDate;
    private String notes;
    private LocalDateTime createdAt;
}
