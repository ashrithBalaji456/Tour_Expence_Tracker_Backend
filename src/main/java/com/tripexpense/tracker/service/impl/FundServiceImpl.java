package com.tripexpense.tracker.service.impl;

import com.tripexpense.tracker.dto.FundRequest;
import com.tripexpense.tracker.dto.FundResponse;
import com.tripexpense.tracker.entity.FundContribution;
import com.tripexpense.tracker.repository.FundContributionRepository;
import com.tripexpense.tracker.service.FundService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FundServiceImpl implements FundService {

    private final FundContributionRepository fundRepository;

    @Override
    @Transactional
    public FundResponse addFund(FundRequest request) {
        FundContribution fund = FundContribution.builder()
                .contributorName(request.getContributorName())
                .amount(request.getAmount())
                .paymentMode(request.getPaymentMode() != null ? request.getPaymentMode() : com.tripexpense.tracker.entity.PaymentMode.CASH)
                .contributionDate(request.getContributionDate() != null ? request.getContributionDate() : LocalDate.now())
                .notes(request.getNotes())
                .build();

        FundContribution saved = fundRepository.save(fund);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FundResponse> getAllFunds() {
        return fundRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public FundResponse updateFund(Long id, FundRequest request) {
        FundContribution fund = fundRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fund contribution not found with ID: " + id));

        if (request.getContributorName() != null && !request.getContributorName().isBlank()) {
            fund.setContributorName(request.getContributorName());
        }
        if (request.getAmount() != null) {
            fund.setAmount(request.getAmount());
        }
        if (request.getPaymentMode() != null) {
            fund.setPaymentMode(request.getPaymentMode());
        }
        if (request.getContributionDate() != null) {
            fund.setContributionDate(request.getContributionDate());
        }
        if (request.getNotes() != null) {
            fund.setNotes(request.getNotes());
        }

        FundContribution updated = fundRepository.save(fund);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteFund(Long id) {
        if (!fundRepository.existsById(id)) {
            throw new RuntimeException("Fund contribution not found with ID: " + id);
        }
        fundRepository.deleteById(id);
    }

    private FundResponse mapToResponse(FundContribution fund) {
        return FundResponse.builder()
                .id(fund.getId())
                .contributorName(fund.getContributorName())
                .amount(fund.getAmount())
                .paymentMode(fund.getPaymentMode() != null ? fund.getPaymentMode() : com.tripexpense.tracker.entity.PaymentMode.CASH)
                .contributionDate(fund.getContributionDate())
                .notes(fund.getNotes())
                .createdAt(fund.getCreatedAt())
                .build();
    }
}
