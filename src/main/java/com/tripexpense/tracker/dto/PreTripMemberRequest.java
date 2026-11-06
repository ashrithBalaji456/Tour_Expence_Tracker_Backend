package com.tripexpense.tracker.dto;

import com.tripexpense.tracker.entity.SplitMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreTripMemberRequest {
    private String name;
    private BigDecimal budgetLimit;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private SplitMode splitMode;
    private Boolean isDeparted;
    private Boolean settledOnDeparture;
    private List<MemberExpenseAllocationDto> allocations;
}
