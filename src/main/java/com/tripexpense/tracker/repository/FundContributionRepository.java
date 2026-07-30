package com.tripexpense.tracker.repository;

import com.tripexpense.tracker.entity.FundContribution;
import com.tripexpense.tracker.entity.TripGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface FundContributionRepository extends JpaRepository<FundContribution, Long> {

    List<FundContribution> findByTripGroup(TripGroup tripGroup);

    @Query("SELECT SUM(f.amount) FROM FundContribution f WHERE f.tripGroup = :tripGroup")
    BigDecimal getTotalFundsSum(@Param("tripGroup") TripGroup tripGroup);
}
