package com.tripexpense.tracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_expense_allocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberExpenseAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private PreTripMember member;

    @Column(name = "expense_id")
    private Long expenseId;

    @Column(name = "source_type") // "ACTIVE" or "PRE_TRIP"
    private String sourceType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "original_amount", precision = 10, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "allocated_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal allocatedAmount;

    @Column(name = "days_count")
    private Integer daysCount;

    @Column(name = "total_days")
    private Integer totalDays;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}

