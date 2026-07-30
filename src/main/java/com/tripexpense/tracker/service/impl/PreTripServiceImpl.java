package com.tripexpense.tracker.service.impl;

import com.tripexpense.tracker.dto.*;
import com.tripexpense.tracker.entity.PreTripExpense;
import com.tripexpense.tracker.entity.PreTripMember;
import com.tripexpense.tracker.repository.PreTripExpenseRepository;
import com.tripexpense.tracker.repository.PreTripMemberRepository;
import com.tripexpense.tracker.service.PreTripService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PreTripServiceImpl implements PreTripService {

    private final PreTripMemberRepository memberRepository;
    private final PreTripExpenseRepository expenseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PreTripMemberResponse> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(this::mapToMemberResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PreTripMemberResponse saveMember(PreTripMemberRequest request) {
        Optional<PreTripMember> existing = memberRepository.findByNameIgnoreCase(request.getName());
        PreTripMember member;
        if (existing.isPresent()) {
            member = existing.get();
            if (request.getBudgetLimit() != null) {
                member.setBudgetLimit(request.getBudgetLimit());
            }
        } else {
            member = PreTripMember.builder()
                    .name(request.getName().trim())
                    .budgetLimit(request.getBudgetLimit() != null ? request.getBudgetLimit() : new BigDecimal("10000.00"))
                    .build();
        }
        PreTripMember saved = memberRepository.save(member);
        return mapToMemberResponse(saved);
    }

    @Override
    @Transactional
    public void deleteMember(Long id) {
        if (!memberRepository.existsById(id)) {
            throw new RuntimeException("Member not found with ID: " + id);
        }
        memberRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PreTripExpenseResponse> getAllExpenses() {
        return expenseRepository.findAll().stream()
                .map(this::mapToExpenseResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PreTripExpenseResponse createExpense(PreTripExpenseRequest request) {
        PreTripExpense expense = PreTripExpense.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .spentBy(request.getSpentBy().trim())
                .expenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : java.time.LocalDate.now())
                .notes(request.getNotes())
                .build();
        PreTripExpense saved = expenseRepository.save(expense);
        return mapToExpenseResponse(saved);
    }

    @Override
    @Transactional
    public PreTripExpenseResponse updateExpense(Long id, PreTripExpenseRequest request) {
        PreTripExpense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pre-trip expense not found with ID: " + id));

        if (request.getTitle() != null) expense.setTitle(request.getTitle());
        if (request.getAmount() != null) expense.setAmount(request.getAmount());
        if (request.getSpentBy() != null) expense.setSpentBy(request.getSpentBy().trim());
        if (request.getExpenseDate() != null) expense.setExpenseDate(request.getExpenseDate());
        if (request.getNotes() != null) expense.setNotes(request.getNotes());

        PreTripExpense saved = expenseRepository.save(expense);
        return mapToExpenseResponse(saved);
    }

    @Override
    @Transactional
    public void deleteExpense(Long id) {
        if (!expenseRepository.existsById(id)) {
            throw new RuntimeException("Pre-trip expense not found with ID: " + id);
        }
        expenseRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PreTripSummaryDto getSummary() {
        List<PreTripMember> members = memberRepository.findAll();
        List<PreTripExpense> expenses = expenseRepository.findAll();

        BigDecimal totalSpent = expenses.stream()
                .map(PreTripExpense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (members.isEmpty()) {
            return PreTripSummaryDto.builder()
                    .totalSpent(totalSpent)
                    .sharePerMember(BigDecimal.ZERO)
                    .memberSummaries(new ArrayList<>())
                    .transfers(new ArrayList<>())
                    .build();
        }

        int memberCount = members.size();
        BigDecimal sharePerMember = totalSpent.divide(BigDecimal.valueOf(memberCount), 2, RoundingMode.HALF_UP);

        // Group expenses by spentBy (case-insensitive key)
        Map<String, BigDecimal> memberSpendMap = new HashMap<>();
        for (PreTripMember m : members) {
            memberSpendMap.put(m.getName().toLowerCase(), BigDecimal.ZERO);
        }

        BigDecimal countMembers = BigDecimal.valueOf(members.isEmpty() ? 1 : members.size());

        for (PreTripExpense e : expenses) {
            if (e.getSpentBy() != null) {
                BigDecimal amt = e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO;
                if (e.getSpentBy().equalsIgnoreCase("Group")) {
                    BigDecimal splitShare = amt.divide(countMembers, 2, RoundingMode.HALF_UP);
                    for (PreTripMember m : members) {
                        String key = m.getName().toLowerCase();
                        memberSpendMap.put(key, memberSpendMap.getOrDefault(key, BigDecimal.ZERO).add(splitShare));
                    }
                } else {
                    String key = e.getSpentBy().toLowerCase();
                    memberSpendMap.put(key, memberSpendMap.getOrDefault(key, BigDecimal.ZERO).add(amt));
                }
            }
        }

        List<PreTripMemberSummaryDto> summaries = new ArrayList<>();
        List<MemberBalanceHelper> creditors = new ArrayList<>();
        List<MemberBalanceHelper> debtors = new ArrayList<>();

        for (PreTripMember m : members) {
            BigDecimal spent = memberSpendMap.getOrDefault(m.getName().toLowerCase(), BigDecimal.ZERO);
            BigDecimal netBalance = spent.subtract(sharePerMember);

            boolean isSpentOverBudget = spent.compareTo(m.getBudgetLimit()) > 0;
            boolean isShareOverBudget = sharePerMember.compareTo(m.getBudgetLimit()) > 0;

            summaries.add(PreTripMemberSummaryDto.builder()
                    .memberName(m.getName())
                    .budgetLimit(m.getBudgetLimit())
                    .totalSpent(spent)
                    .share(sharePerMember)
                    .netBalance(netBalance)
                    .isSpentOverBudget(isSpentOverBudget)
                    .isShareOverBudget(isShareOverBudget)
                    .build());

            if (netBalance.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new MemberBalanceHelper(m.getName(), netBalance));
            } else if (netBalance.compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(new MemberBalanceHelper(m.getName(), netBalance.abs()));
            }
        }

        // Settlement matching algorithm
        List<PreTripTransferDto> transfers = new ArrayList<>();
        
        // Sort creditors desc (largest surplus first)
        creditors.sort((c1, c2) -> c2.balance.compareTo(c1.balance));
        // Sort debtors desc (largest debt first)
        debtors.sort((d1, d2) -> d2.balance.compareTo(d1.balance));

        int cIdx = 0;
        int dIdx = 0;

        while (cIdx < creditors.size() && dIdx < debtors.size()) {
            MemberBalanceHelper creditor = creditors.get(cIdx);
            MemberBalanceHelper debtor = debtors.get(dIdx);

            BigDecimal transferAmount = creditor.balance.min(debtor.balance);

            if (transferAmount.compareTo(BigDecimal.ZERO) > 0) {
                transfers.add(PreTripTransferDto.builder()
                        .fromMember(debtor.name)
                        .toMember(creditor.name)
                        .amount(transferAmount.setScale(2, RoundingMode.HALF_UP))
                        .build());
            }

            creditor.balance = creditor.balance.subtract(transferAmount);
            debtor.balance = debtor.balance.subtract(transferAmount);

            if (creditor.balance.compareTo(new BigDecimal("0.01")) < 0) {
                cIdx++;
            }
            if (debtor.balance.compareTo(new BigDecimal("0.01")) < 0) {
                dIdx++;
            }
        }

        return PreTripSummaryDto.builder()
                .totalSpent(totalSpent)
                .sharePerMember(sharePerMember)
                .memberSummaries(summaries)
                .transfers(transfers)
                .build();
    }

    private PreTripMemberResponse mapToMemberResponse(PreTripMember member) {
        return PreTripMemberResponse.builder()
                .id(member.getId())
                .name(member.getName())
                .budgetLimit(member.getBudgetLimit())
                .createdAt(member.getCreatedAt())
                .build();
    }

    private PreTripExpenseResponse mapToExpenseResponse(PreTripExpense expense) {
        return PreTripExpenseResponse.builder()
                .id(expense.getId())
                .title(expense.getTitle())
                .amount(expense.getAmount())
                .spentBy(expense.getSpentBy())
                .expenseDate(expense.getExpenseDate())
                .notes(expense.getNotes())
                .createdAt(expense.getCreatedAt())
                .build();
    }

    private static class MemberBalanceHelper {
        String name;
        BigDecimal balance;

        MemberBalanceHelper(String name, BigDecimal balance) {
            this.name = name;
            this.balance = balance;
        }
    }
}
