package com.tripexpense.tracker.dto;

import com.tripexpense.tracker.entity.PaymentMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FundRequest {

    @NotBlank(message = "Contributor name is required")
    private String contributorName;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    private PaymentMode paymentMode;

    private LocalDate contributionDate;

    private String notes;
}
