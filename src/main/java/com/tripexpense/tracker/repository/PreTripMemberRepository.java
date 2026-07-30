package com.tripexpense.tracker.repository;

import com.tripexpense.tracker.entity.PreTripMember;
import com.tripexpense.tracker.entity.TripGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PreTripMemberRepository extends JpaRepository<PreTripMember, Long> {
    List<PreTripMember> findByTripGroup(TripGroup tripGroup);
    Optional<PreTripMember> findByNameIgnoreCaseAndTripGroup(String name, TripGroup tripGroup);
    boolean existsByNameIgnoreCaseAndTripGroup(String name, TripGroup tripGroup);
}
