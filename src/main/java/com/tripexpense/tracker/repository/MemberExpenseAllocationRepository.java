package com.tripexpense.tracker.repository;

import com.tripexpense.tracker.entity.MemberExpenseAllocation;
import com.tripexpense.tracker.entity.PreTripMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberExpenseAllocationRepository extends JpaRepository<MemberExpenseAllocation, Long> {
    List<MemberExpenseAllocation> findByMember(PreTripMember member);
    List<MemberExpenseAllocation> findByMemberIn(List<PreTripMember> members);
    void deleteByMember(PreTripMember member);
    List<MemberExpenseAllocation> findByExpenseIdAndSourceType(Long expenseId, String sourceType);
}

