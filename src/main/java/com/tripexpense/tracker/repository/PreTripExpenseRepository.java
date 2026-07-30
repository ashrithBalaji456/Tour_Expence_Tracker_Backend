package com.tripexpense.tracker.repository;

import com.tripexpense.tracker.entity.PreTripExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreTripExpenseRepository extends JpaRepository<PreTripExpense, Long> {
    List<PreTripExpense> findBySpentByIgnoreCase(String spentBy);
}
