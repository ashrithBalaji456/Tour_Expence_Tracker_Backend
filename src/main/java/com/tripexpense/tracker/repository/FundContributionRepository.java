package com.tripexpense.tracker.repository;

import com.tripexpense.tracker.entity.FundContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface FundContributionRepository extends JpaRepository<FundContribution, Long> {

    @Query("SELECT SUM(f.amount) FROM FundContribution f")
    BigDecimal getTotalFundsSum();
}
