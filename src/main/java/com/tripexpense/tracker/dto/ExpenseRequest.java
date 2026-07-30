package com.tripexpense.tracker.dto;

import com.tripexpense.tracker.entity.Category;
import com.tripexpense.tracker.entity.PaymentMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ExpenseRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Category is required")
    private Category category;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    private String paidBy;

    private LocalDate expenseDate;

    private String notes;
}
