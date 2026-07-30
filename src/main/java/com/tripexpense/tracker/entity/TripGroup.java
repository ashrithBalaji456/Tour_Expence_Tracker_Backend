package com.tripexpense.tracker.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "trip_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "creator_id")
    private User creator;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "trip_group_members", joinColumns = @JoinColumn(name = "group_id"))
    @Column(name = "username")
    private List<String> memberUsernames;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
