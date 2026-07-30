package com.tripexpense.tracker.service.impl;

import com.tripexpense.tracker.dto.DashboardSummaryDto;
import com.tripexpense.tracker.dto.PreTripMemberSummaryDto;
import com.tripexpense.tracker.dto.PreTripTransferDto;
import com.tripexpense.tracker.entity.Category;
import com.tripexpense.tracker.entity.Expense;
import com.tripexpense.tracker.entity.PreTripExpense;
import com.tripexpense.tracker.entity.PreTripMember;
import com.tripexpense.tracker.repository.ExpenseRepository;
import com.tripexpense.tracker.repository.FundContributionRepository;
import com.tripexpense.tracker.repository.PreTripExpenseRepository;
import com.tripexpense.tracker.repository.PreTripMemberRepository;
import com.tripexpense.tracker.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ExpenseRepository expenseRepository;
    private final FundContributionRepository fundRepository;
    private final PreTripExpenseRepository preTripExpenseRepository;
    private final PreTripMemberRepository preTripMemberRepository;

    @Override
    @Transactional
    public DashboardSummaryDto getSummary() {
        // Retrieve all current expenses and pre-trip bookings
        List<Expense> activeExpenses = expenseRepository.findAll();
        List<PreTripExpense> preTripExpenses = preTripExpenseRepository.findAll();
        List<PreTripMember> members = preTripMemberRepository.findAll();

        // 1. Auto-discover members from logged expenses if no members are in database
        if (members.isEmpty()) {
            Set<String> discoveredNames = new HashSet<>();
            for (Expense e : activeExpenses) {
                if (e.getPaidBy() != null && !e.getPaidBy().isBlank() && !e.getPaidBy().equalsIgnoreCase("Trip Pool")) {
                    discoveredNames.add(e.getPaidBy().trim());
                }
            }
            for (PreTripExpense pe : preTripExpenses) {
                if (pe.getSpentBy() != null && !pe.getSpentBy().isBlank()) {
                    discoveredNames.add(pe.getSpentBy().trim());
                }
            }
            // Auto-save discovered members with a default ₹10,000 budget limit
            for (String name : discoveredNames) {
                PreTripMember newMember = PreTripMember.builder()
                        .name(name)
                        .budgetLimit(new BigDecimal("10000.00"))
                        .build();
                preTripMemberRepository.save(newMember);
            }
            members = preTripMemberRepository.findAll();
        }

        // Calculate Outflows
        BigDecimal totalActiveSpent = activeExpenses.stream()
                .map(Expense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPreTripSpent = preTripExpenses.stream()
                .map(PreTripExpense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = totalActiveSpent.add(totalPreTripSpent);

        // Calculate Budgets
        BigDecimal totalBudget = members.stream()
                .map(PreTripMember::getBudgetLimit)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingBalance = totalBudget.subtract(totalSpent);

        // Share per person
        int memberCount = members.size();
        BigDecimal sharePerMember = BigDecimal.ZERO;
        if (memberCount > 0) {
            sharePerMember = totalSpent.divide(BigDecimal.valueOf(memberCount), 2, RoundingMode.HALF_UP);
        }

        // Compute spending per member
        Map<String, BigDecimal> memberSpendMap = new HashMap<>();
        for (PreTripMember m : members) {
            memberSpendMap.put(m.getName().toLowerCase(), BigDecimal.ZERO);
        }

        BigDecimal countMembers = BigDecimal.valueOf(members.isEmpty() ? 1 : members.size());

        // Add pre-trip spends
        for (PreTripExpense pe : preTripExpenses) {
            if (pe.getSpentBy() != null) {
                BigDecimal amt = pe.getAmount() != null ? pe.getAmount() : BigDecimal.ZERO;
                if (pe.getSpentBy().equalsIgnoreCase("Group")) {
                    BigDecimal splitShare = amt.divide(countMembers, 2, RoundingMode.HALF_UP);
                    for (PreTripMember m : members) {
                        String key = m.getName().toLowerCase();
                        memberSpendMap.put(key, memberSpendMap.getOrDefault(key, BigDecimal.ZERO).add(splitShare));
                    }
                } else {
                    String key = pe.getSpentBy().toLowerCase();
                    memberSpendMap.put(key, memberSpendMap.getOrDefault(key, BigDecimal.ZERO).add(amt));
                }
            }
        }

        // Add active trip spends
        for (Expense e : activeExpenses) {
            if (e.getPaidBy() != null) {
                BigDecimal amt = e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO;
                if (e.getPaidBy().equalsIgnoreCase("Group")) {
                    BigDecimal splitShare = amt.divide(countMembers, 2, RoundingMode.HALF_UP);
                    for (PreTripMember m : members) {
                        String key = m.getName().toLowerCase();
                        memberSpendMap.put(key, memberSpendMap.getOrDefault(key, BigDecimal.ZERO).add(splitShare));
                    }
                } else {
                    String key = e.getPaidBy().toLowerCase();
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

        // Settlements logic
        List<PreTripTransferDto> transfers = new ArrayList<>();
        creditors.sort((c1, c2) -> c2.balance.compareTo(c1.balance));
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

        // Category breakdown
        Map<Category, BigDecimal> categoryMap = new EnumMap<>(Category.class);
        for (Category cat : Category.values()) {
            categoryMap.put(cat, BigDecimal.ZERO);
        }

        List<Object[]> rawBreakdown = expenseRepository.getCategoryExpenseBreakdown();
        for (Object[] row : rawBreakdown) {
            Category cat = (Category) row[0];
            BigDecimal sum = (BigDecimal) row[1];
            if (cat != null && sum != null) {
                categoryMap.put(cat, sum);
            }
        }

        // Fallbacks for backwards compatibility
        BigDecimal totalPoolCollected = fundRepository.getTotalFundsSum();
        if (totalPoolCollected == null) totalPoolCollected = BigDecimal.ZERO;

        return DashboardSummaryDto.builder()
                .totalBudget(totalBudget)
                .totalSpent(totalSpent)
                .remainingBalance(remainingBalance)
                .sharePerMember(sharePerMember)
                .memberSummaries(summaries)
                .transfers(transfers)
                .totalFundsCollected(totalPoolCollected)
                .totalExpensesSpent(totalActiveSpent)
                .totalPreTripSpent(totalPreTripSpent)
                .totalPreTripBudget(totalBudget)
                .totalExpenseCount(activeExpenses.size())
                .categoryBreakdown(categoryMap)
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
