package com.tripexpense.tracker.service.impl;

import com.tripexpense.tracker.dto.*;
import com.tripexpense.tracker.entity.PreTripExpense;
import com.tripexpense.tracker.entity.PreTripMember;
import com.tripexpense.tracker.entity.TripGroup;
import com.tripexpense.tracker.entity.User;
import com.tripexpense.tracker.repository.PreTripExpenseRepository;
import com.tripexpense.tracker.repository.PreTripMemberRepository;
import com.tripexpense.tracker.repository.TripGroupRepository;
import com.tripexpense.tracker.repository.UserRepository;
import com.tripexpense.tracker.service.PreTripService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;
    private final TripGroupRepository tripGroupRepository;

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
            throw new RuntimeException("No active trip group found for user: " + username);
        }
        return groups.get(0);
    }

    private void verifyWriteAccess(TripGroup group) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!group.getCreator().getUsername().equalsIgnoreCase(username)) {
            throw new AccessDeniedException("Only the group creator can perform modifications!");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PreTripMemberResponse> getAllMembers() {
        TripGroup activeGroup = getActiveTripGroup();
        return memberRepository.findByTripGroup(activeGroup).stream()
                .map(this::mapToMemberResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PreTripMemberResponse saveMember(PreTripMemberRequest request) {
        TripGroup activeGroup = getActiveTripGroup();
        verifyWriteAccess(activeGroup);

        Optional<PreTripMember> existing = memberRepository.findByNameIgnoreCaseAndTripGroup(request.getName(), activeGroup);
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
                    .tripGroup(activeGroup)
                    .build();
        }
        PreTripMember saved = memberRepository.save(member);
        return mapToMemberResponse(saved);
    }

    @Override
    @Transactional
    public void deleteMember(Long id) {
        TripGroup activeGroup = getActiveTripGroup();
        verifyWriteAccess(activeGroup);

        PreTripMember member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found with ID: " + id));

        if (!member.getTripGroup().getId().equals(activeGroup.getId())) {
            throw new AccessDeniedException("Access denied to this record");
        }
        memberRepository.delete(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PreTripExpenseResponse> getAllExpenses() {
        TripGroup activeGroup = getActiveTripGroup();
        return expenseRepository.findByTripGroup(activeGroup).stream()
                .map(this::mapToExpenseResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PreTripExpenseResponse createExpense(PreTripExpenseRequest request) {
        TripGroup activeGroup = getActiveTripGroup();
        verifyWriteAccess(activeGroup);

        PreTripExpense expense = PreTripExpense.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .spentBy(request.getSpentBy().trim())
                .expenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : java.time.LocalDate.now())
                .notes(request.getNotes())
                .tripGroup(activeGroup)
                .build();
        PreTripExpense saved = expenseRepository.save(expense);
        return mapToExpenseResponse(saved);
    }

    @Override
    @Transactional
    public PreTripExpenseResponse updateExpense(Long id, PreTripExpenseRequest request) {
        TripGroup activeGroup = getActiveTripGroup();
        verifyWriteAccess(activeGroup);

        PreTripExpense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pre-trip expense not found with ID: " + id));

        if (!expense.getTripGroup().getId().equals(activeGroup.getId())) {
            throw new AccessDeniedException("Access denied to this record");
        }

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
        TripGroup activeGroup = getActiveTripGroup();
        verifyWriteAccess(activeGroup);

        PreTripExpense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pre-trip expense not found with ID: " + id));

        if (!expense.getTripGroup().getId().equals(activeGroup.getId())) {
            throw new AccessDeniedException("Access denied to this record");
        }
        expenseRepository.delete(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public PreTripSummaryDto getSummary() {
        TripGroup activeGroup = getActiveTripGroup();
        List<PreTripMember> members = memberRepository.findByTripGroup(activeGroup);
        List<PreTripExpense> expenses = expenseRepository.findByTripGroup(activeGroup);

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

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isReadOnly = !activeGroup.getCreator().getUsername().equalsIgnoreCase(currentUsername);

        return PreTripSummaryDto.builder()
                .totalSpent(totalSpent)
                .sharePerMember(sharePerMember)
                .memberSummaries(summaries)
                .transfers(transfers)
                .isReadOnly(isReadOnly)
                .groupName(activeGroup.getName())
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
