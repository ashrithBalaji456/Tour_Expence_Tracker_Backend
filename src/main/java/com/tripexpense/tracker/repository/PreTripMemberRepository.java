package com.tripexpense.tracker.repository;

import com.tripexpense.tracker.entity.PreTripMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PreTripMemberRepository extends JpaRepository<PreTripMember, Long> {
    Optional<PreTripMember> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
