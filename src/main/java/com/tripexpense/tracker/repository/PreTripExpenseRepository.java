package com.tripexpense.tracker.repository;

import com.tripexpense.tracker.entity.PreTripExpense;
import com.tripexpense.tracker.entity.TripGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreTripExpenseRepository extends JpaRepository<PreTripExpense, Long> {
    List<PreTripExpense> findByTripGroup(TripGroup tripGroup);
    List<PreTripExpense> findBySpentByIgnoreCaseAndTripGroup(String spentBy, TripGroup tripGroup);
}
