package com.tripexpense.tracker.dto;

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
public class FundResponse {

    private Long id;
    private String contributorName;
    private BigDecimal amount;
    private PaymentMode paymentMode;
    private LocalDate contributionDate;
    private String notes;
    private LocalDateTime createdAt;
}
