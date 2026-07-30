package com.tripexpense.tracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreTripSummaryDto {
    private BigDecimal totalSpent;
    private BigDecimal sharePerMember;
    private List<PreTripMemberSummaryDto> memberSummaries;
    private List<PreTripTransferDto> transfers;
}
