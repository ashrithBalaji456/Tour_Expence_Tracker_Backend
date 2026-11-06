package com.tripexpense.tracker.service.impl;

import com.tripexpense.tracker.dto.DashboardSummaryDto;
import com.tripexpense.tracker.dto.PreTripMemberSummaryDto;
import com.tripexpense.tracker.dto.PreTripTransferDto;
import com.tripexpense.tracker.entity.*;
import com.tripexpense.tracker.repository.*;
import com.tripexpense.tracker.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ExpenseRepository expenseRepository;
    private final FundContributionRepository fundRepository;
    private final PreTripExpenseRepository preTripExpenseRepository;
    private final PreTripMemberRepository preTripMemberRepository;
    private final UserRepository userRepository;
    private final TripGroupRepository tripGroupRepository;
    private final MemberExpenseAllocationRepository allocationRepository;

    private TripGroup getActiveTripGroup() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Try to get group ID from request header
        String headerGroupId = null;
        var attributes = (org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            headerGroupId = attributes.getRequest().getHeader("X-Trip-Group-Id");
        }
        
        if (headerGroupId != null && !headerGroupId.isBlank()) {
            try {
                Long groupId = Long.parseLong(headerGroupId);
                var groupOpt = tripGroupRepository.findById(groupId);
                if (groupOpt.isPresent()) {
                    TripGroup g = groupOpt.get();
                    boolean isCreator = g.getCreator().getUsername().equalsIgnoreCase(username);
                    boolean isMember = g.getMemberUsernames().stream().anyMatch(m -> m.equalsIgnoreCase(username));
                    if (isCreator || isMember) {
                        return g;
                    }
                }
            } catch (Exception e) {
                // fallback
            }
        }

        String email = userRepository.findByUsername(username)
                .map(User::getEmail)
                .orElse("");
        List<TripGroup> groups = tripGroupRepository.findAssociatedGroups(username, email);
        if (groups.isEmpty()) {
            return null; // Return null if user has no groups yet
        }
        return groups.get(0);
    }

    @Override
    @Transactional
    public DashboardSummaryDto getSummary() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        TripGroup activeGroup = getActiveTripGroup();

        // Return empty dashboard details if no active group
        if (activeGroup == null) {
            return DashboardSummaryDto.builder()
                    .totalBudget(BigDecimal.ZERO)
                    .totalSpent(BigDecimal.ZERO)
                    .remainingBalance(BigDecimal.ZERO)
                    .sharePerMember(BigDecimal.ZERO)
                    .memberSummaries(new ArrayList<>())
                    .transfers(new ArrayList<>())
                    .isReadOnly(false)
                    .groupName("")
                    .categoryBreakdown(new EnumMap<>(Category.class))
                    .build();
        }

        boolean isReadOnly = !activeGroup.getCreator().getUsername().equalsIgnoreCase(currentUsername);

        // Retrieve scoped expenses and pre-trip bookings
        List<Expense> activeExpenses = expenseRepository.findByTripGroup(activeGroup);
        List<PreTripExpense> preTripExpenses = preTripExpenseRepository.findByTripGroup(activeGroup);
        List<PreTripMember> members = preTripMemberRepository.findByTripGroup(activeGroup);
        List<MemberExpenseAllocation> allAllocations = allocationRepository.findByMemberIn(members);

        // Calculate Outflows
        BigDecimal totalActiveSpent = activeExpenses.stream()
                .filter(e -> e.getCategory() != Category.SETTLEMENT)
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

        // Dynamic temporal calculation maps
        Map<String, BigDecimal> memberPaidMap = new HashMap<>();
        Map<String, BigDecimal> memberOwedMap = new HashMap<>();
        Map<String, Integer> memberEligibleCountMap = new HashMap<>();
        Map<String, Integer> memberAllocatedCountMap = new HashMap<>();

        for (PreTripMember m : members) {
            String key = m.getName().toLowerCase();
            memberPaidMap.put(key, BigDecimal.ZERO);
            memberOwedMap.put(key, BigDecimal.ZERO);
            memberEligibleCountMap.put(key, 0);
            memberAllocatedCountMap.put(key, 0);
        }

        // Group allocations by source
        Map<Long, List<MemberExpenseAllocation>> preTripAllocMap = allAllocations.stream()
                .filter(a -> "PRE_TRIP".equalsIgnoreCase(a.getSourceType()))
                .collect(Collectors.groupingBy(MemberExpenseAllocation::getExpenseId));

        Map<Long, List<MemberExpenseAllocation>> activeAllocMap = allAllocations.stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getSourceType()))
                .collect(Collectors.groupingBy(MemberExpenseAllocation::getExpenseId));

        // 1. Process Pre-Trip Expenses
        for (PreTripExpense pe : preTripExpenses) {
            LocalDateTime expenseTime = pe.getExpenseDateTime();
            BigDecimal amt = pe.getAmount() != null ? pe.getAmount() : BigDecimal.ZERO;

            List<MemberExpenseAllocation> allocs = preTripAllocMap.getOrDefault(pe.getId(), Collections.emptyList());
            Set<String> explicitlyAllocatedMemberKeys = new HashSet<>();
            BigDecimal totalAllocatedAmt = BigDecimal.ZERO;

            for (MemberExpenseAllocation alloc : allocs) {
                String allocKey = alloc.getMember().getName().toLowerCase();
                memberOwedMap.put(allocKey, memberOwedMap.getOrDefault(allocKey, BigDecimal.ZERO).add(alloc.getAllocatedAmount()));
                memberAllocatedCountMap.put(allocKey, memberAllocatedCountMap.getOrDefault(allocKey, 0) + 1);
                explicitlyAllocatedMemberKeys.add(allocKey);
                totalAllocatedAmt = totalAllocatedAmt.add(alloc.getAllocatedAmount());
            }

            BigDecimal remainder = amt.subtract(totalAllocatedAmt);
            if (remainder.compareTo(BigDecimal.ZERO) < 0) remainder = BigDecimal.ZERO;

            List<PreTripMember> eligible = members.stream()
                    .filter(m -> !explicitlyAllocatedMemberKeys.contains(m.getName().toLowerCase()))
                    .filter(m -> m.getEffectiveFrom() == null || !m.getEffectiveFrom().isAfter(expenseTime))
                    .filter(m -> m.getEffectiveTo() == null || !expenseTime.isAfter(m.getEffectiveTo()))
                    .toList();

            if (eligible.isEmpty()) {
                eligible = members.stream()
                        .filter(m -> !explicitlyAllocatedMemberKeys.contains(m.getName().toLowerCase()))
                        .toList();
            }

            if (!eligible.isEmpty() && remainder.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal splitShare = remainder.divide(BigDecimal.valueOf(eligible.size()), 2, RoundingMode.HALF_UP);
                for (PreTripMember m : eligible) {
                    String key = m.getName().toLowerCase();
                    memberOwedMap.put(key, memberOwedMap.getOrDefault(key, BigDecimal.ZERO).add(splitShare));
                    memberEligibleCountMap.put(key, memberEligibleCountMap.getOrDefault(key, 0) + 1);
                }
            }

            if (pe.getSpentBy() != null) {
                if (pe.getSpentBy().equalsIgnoreCase("Group")) {
                    for (PreTripMember m : members) {
                        String key = m.getName().toLowerCase();
                        memberPaidMap.put(key, memberPaidMap.getOrDefault(key, BigDecimal.ZERO).add(amt.divide(BigDecimal.valueOf(members.size()), 2, RoundingMode.HALF_UP)));
                    }
                } else {
                    String key = pe.getSpentBy().toLowerCase();
                    memberPaidMap.put(key, memberPaidMap.getOrDefault(key, BigDecimal.ZERO).add(amt));
                }
            }
        }

        // 2. Process Active Trip Expenses (excluding settlements)
        for (Expense e : activeExpenses) {
            if (e.getCategory() == Category.SETTLEMENT) continue;

            LocalDateTime expenseTime = e.getExpenseDateTime();
            BigDecimal amt = e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO;

            List<MemberExpenseAllocation> allocs = activeAllocMap.getOrDefault(e.getId(), Collections.emptyList());
            Set<String> explicitlyAllocatedMemberKeys = new HashSet<>();
            BigDecimal totalAllocatedAmt = BigDecimal.ZERO;

            for (MemberExpenseAllocation alloc : allocs) {
                String allocKey = alloc.getMember().getName().toLowerCase();
                memberOwedMap.put(allocKey, memberOwedMap.getOrDefault(allocKey, BigDecimal.ZERO).add(alloc.getAllocatedAmount()));
                memberAllocatedCountMap.put(allocKey, memberAllocatedCountMap.getOrDefault(allocKey, 0) + 1);
                explicitlyAllocatedMemberKeys.add(allocKey);
                totalAllocatedAmt = totalAllocatedAmt.add(alloc.getAllocatedAmount());
            }

            BigDecimal remainder = amt.subtract(totalAllocatedAmt);
            if (remainder.compareTo(BigDecimal.ZERO) < 0) remainder = BigDecimal.ZERO;

            List<PreTripMember> eligible = members.stream()
                    .filter(m -> !explicitlyAllocatedMemberKeys.contains(m.getName().toLowerCase()))
                    .filter(m -> m.getEffectiveFrom() == null || !m.getEffectiveFrom().isAfter(expenseTime))
                    .filter(m -> m.getEffectiveTo() == null || !expenseTime.isAfter(m.getEffectiveTo()))
                    .toList();

            if (eligible.isEmpty()) {
                eligible = members.stream()
                        .filter(m -> !explicitlyAllocatedMemberKeys.contains(m.getName().toLowerCase()))
                        .toList();
            }

            if (!eligible.isEmpty() && remainder.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal splitShare = remainder.divide(BigDecimal.valueOf(eligible.size()), 2, RoundingMode.HALF_UP);
                for (PreTripMember m : eligible) {
                    String key = m.getName().toLowerCase();
                    memberOwedMap.put(key, memberOwedMap.getOrDefault(key, BigDecimal.ZERO).add(splitShare));
                    memberEligibleCountMap.put(key, memberEligibleCountMap.getOrDefault(key, 0) + 1);
                }
            }

            if (e.getPaidBy() != null) {
                if (e.getPaidBy().equalsIgnoreCase("Group") || e.getPaidBy().equalsIgnoreCase("Group Pool")) {
                    for (PreTripMember m : members) {
                        String key = m.getName().toLowerCase();
                        memberPaidMap.put(key, memberPaidMap.getOrDefault(key, BigDecimal.ZERO).add(amt.divide(BigDecimal.valueOf(members.size()), 2, RoundingMode.HALF_UP)));
                    }
                } else {
                    String key = e.getPaidBy().toLowerCase();
                    memberPaidMap.put(key, memberPaidMap.getOrDefault(key, BigDecimal.ZERO).add(amt));
                }
            }
        }

        // 3. Adjust for any recorded settlements from active expenses
        for (Expense e : activeExpenses) {
            if (e.getCategory() == Category.SETTLEMENT && e.getPaidBy() != null) {
                BigDecimal amt = e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO;
                String fromKey = e.getPaidBy().toLowerCase();
                String toKey = null;
                if (e.getNotes() != null && e.getNotes().startsWith("Settle:")) {
                    String[] parts = e.getNotes().split(":");
                    if (parts.length >= 3) {
                        toKey = parts[2].toLowerCase();
                    }
                }
                if (toKey != null) {
                    memberPaidMap.put(fromKey, memberPaidMap.getOrDefault(fromKey, BigDecimal.ZERO).add(amt));
                    memberPaidMap.put(toKey, memberPaidMap.getOrDefault(toKey, BigDecimal.ZERO).subtract(amt));
                }
            }
        }

        List<PreTripMemberSummaryDto> summaries = new ArrayList<>();
        List<MemberBalanceHelper> creditors = new ArrayList<>();
        List<MemberBalanceHelper> debtors = new ArrayList<>();

        for (PreTripMember m : members) {
            String key = m.getName().toLowerCase();
            BigDecimal paid = memberPaidMap.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal owed = memberOwedMap.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal netBalance = paid.subtract(owed);

            boolean isSpentOverBudget = paid.compareTo(m.getBudgetLimit()) > 0;
            boolean isShareOverBudget = owed.compareTo(m.getBudgetLimit()) > 0;

            summaries.add(PreTripMemberSummaryDto.builder()
                    .memberId(m.getId())
                    .memberName(m.getName())
                    .budgetLimit(m.getBudgetLimit())
                    .totalSpent(paid)
                    .share(owed)
                    .netBalance(netBalance)
                    .isSpentOverBudget(isSpentOverBudget)
                    .isShareOverBudget(isShareOverBudget)
                    .effectiveFrom(m.getEffectiveFrom())
                    .effectiveTo(m.getEffectiveTo())
                    .splitMode(m.getSplitMode())
                    .isDeparted(m.getIsDeparted())
                    .settledOnDeparture(m.getSettledOnDeparture())
                    .eligibleExpenseCount(memberEligibleCountMap.getOrDefault(key, 0))
                    .allocatedItemCount(memberAllocatedCountMap.getOrDefault(key, 0))
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

        // Category breakdown (exclude settlements)
        Map<Category, BigDecimal> categoryMap = new EnumMap<>(Category.class);
        for (Category cat : Category.values()) {
            if (cat != Category.SETTLEMENT) {
                categoryMap.put(cat, BigDecimal.ZERO);
            }
        }
        for (Expense exp : activeExpenses) {
            if (exp.getCategory() != Category.SETTLEMENT && exp.getAmount() != null) {
                categoryMap.put(exp.getCategory(), categoryMap.getOrDefault(exp.getCategory(), BigDecimal.ZERO).add(exp.getAmount()));
            }
        }

        long activeCount = activeExpenses.stream()
                .filter(e -> e.getCategory() != Category.SETTLEMENT)
                .count();

        return DashboardSummaryDto.builder()
                .totalBudget(totalBudget)
                .totalSpent(totalSpent)
                .remainingBalance(remainingBalance)
                .sharePerMember(sharePerMember)
                .memberSummaries(summaries)
                .transfers(transfers)
                .totalExpensesSpent(totalActiveSpent)
                .totalPreTripSpent(totalPreTripSpent)
                .totalPreTripBudget(totalBudget)
                .totalExpenseCount((int) activeCount)
                .categoryBreakdown(categoryMap)
                .isReadOnly(isReadOnly)
                .groupName(activeGroup.getName())
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
