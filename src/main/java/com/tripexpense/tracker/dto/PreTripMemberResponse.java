package com.tripexpense.tracker.dto;

import com.tripexpense.tracker.entity.SplitMode;
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
public class PreTripMemberResponse {
    private Long id;
    private String name;
    private BigDecimal budgetLimit;
    private LocalDateTime effectiveFrom;
    private SplitMode splitMode;
    private LocalDateTime createdAt;
}
