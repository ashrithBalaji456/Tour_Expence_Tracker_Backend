package com.tripexpense.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreTripTransferDto {
    private String fromMember;
    private String toMember;
    private BigDecimal amount;
}
