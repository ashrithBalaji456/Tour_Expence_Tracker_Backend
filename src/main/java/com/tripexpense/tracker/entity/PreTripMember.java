package com.tripexpense.tracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pre_trip_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreTripMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal budgetLimit = new BigDecimal("10000.00");

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "is_departed")
    @Builder.Default
    private Boolean isDeparted = false;

    @Column(name = "settled_on_departure")
    @Builder.Default
    private Boolean settledOnDeparture = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_mode")
    @Builder.Default
    private SplitMode splitMode = SplitMode.FROM_START;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_group_id")
    private TripGroup tripGroup;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isDeparted == null) this.isDeparted = false;
        if (this.settledOnDeparture == null) this.settledOnDeparture = false;
    }
}
