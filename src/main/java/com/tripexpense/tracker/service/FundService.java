package com.tripexpense.tracker.service;

import com.tripexpense.tracker.dto.FundRequest;
import com.tripexpense.tracker.dto.FundResponse;

import java.util.List;

public interface FundService {

    FundResponse addFund(FundRequest request);

    List<FundResponse> getAllFunds();

    FundResponse updateFund(Long id, FundRequest request);

    void deleteFund(Long id);
}
