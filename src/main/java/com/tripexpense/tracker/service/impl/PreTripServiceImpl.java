package com.tripexpense.tracker.service.impl;

import com.tripexpense.tracker.dto.*;
import com.tripexpense.tracker.entity.*;
import com.tripexpense.tracker.repository.*;
import com.tripexpense.tracker.service.PreTripService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
public class PreTripServiceImpl implements PreTripService {

    private final PreTripMemberRepository memberRepository;
    private final PreTripExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final TripGroupRepository tripGroupRepository;
    private final ExpenseRepository activeExpenseRepository;
    private final MemberExpenseAllocationRepository allocationRepository;

    private TripGroup getActiveTripGroup() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

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

    private void verifyWriteAccess(TripGroup activeGroup) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!activeGroup.getCreator().getUsername().equalsIgnoreCase(currentUsername)) {
            throw new AccessDeniedException("Only the group creator can perform modifications!");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PreTripMemberResponse> getAllMembers() {
        TripGroup activeGroup = getActiveTripGroup();
        List<PreTripMember> members = memberRepository.findByTripGroup(activeGroup);
        List<MemberExpenseAllocation> allAllocations = allocationRepository.findByMemberIn(members);

        Map<Long, List<MemberExpenseAllocation>> allocMap = allAllocations.stream()
                .collect(Collectors.groupingBy(a -> a.getMember().getId()));

        return members.stream()
                .map(m -> mapToMemberResponse(m, allocMap.getOrDefault(m.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PreTripMemberResponse saveMember(PreTripMemberRequest request) {
        TripGroup activeGroup = getActiveTripGroup();
        verifyWriteAccess(activeGroup);

        SplitMode mode = request.getSplitMode() != null ? request.getSplitMode() : SplitMode.FROM_START;
        LocalDateTime effective = null;
        if (mode == SplitMode.FROM_NOW) {
            effective = LocalDateTime.now();
        } else if (mode == SplitMode.CUSTOM_DATE) {
            effective = request.getEffectiveFrom() != null ? request.getEffectiveFrom() : LocalDateTime.now();
        }

        Optional<PreTripMember> existing = memberRepository.findByNameIgnoreCaseAndTripGroup(request.getName(), activeGroup);
        PreTripMember member;
        if (existing.isPresent()) {
            member = existing.get();
            if (request.getBudgetLimit() != null) {
                member.setBudgetLimit(request.getBudgetLimit());
            }
            if (request.getSplitMode() != null) {
                member.setSplitMode(mode);
                member.setEffectiveFrom(effective);
            }
            if (request.getEffectiveTo() != null) {
                member.setEffectiveTo(request.getEffectiveTo());
            }
            if (request.getIsDeparted() != null) {
                member.setIsDeparted(request.getIsDeparted());
            }
            if (request.getSettledOnDeparture() != null) {
                member.setSettledOnDeparture(request.getSettledOnDeparture());
            }
        } else {
            member = PreTripMember.builder()
                    .name(request.getName().trim())
                    .budgetLimit(request.getBudgetLimit() != null ? request.getBudgetLimit() : new BigDecimal("10000.00"))
                    .splitMode(mode)
                    .effectiveFrom(effective)
                    .effectiveTo(request.getEffectiveTo())
                    .isDeparted(request.getIsDeparted() != null ? request.getIsDeparted() : false)
                    .settledOnDeparture(request.getSettledOnDeparture() != null ? request.getSettledOnDeparture() : false)
                    .tripGroup(activeGroup)
                    .build();
        }

        // Add to activeGroup.memberUsernames if not present
        String cleanName = request.getName().trim();
        boolean alreadyInGroup = activeGroup.getMemberUsernames().stream()
                .anyMatch(u -> u.equalsIgnoreCase(cleanName));
        if (!alreadyInGroup) {
            activeGroup.getMemberUsernames().add(cleanName);
            tripGroupRepository.save(activeGroup);
        }

        PreTripMember saved = memberRepository.save(member);

        // Update allocations if provided
        if (request.getAllocations() != null) {
            allocationRepository.deleteByMember(saved);
            List<MemberExpenseAllocation> newAllocations = new ArrayList<>();
            for (MemberExpenseAllocationDto dto : request.getAllocations()) {
                if (dto.getAllocatedAmount() != null && dto.getAllocatedAmount().compareTo(BigDecimal.ZERO) > 0) {
                    newAllocations.add(MemberExpenseAllocation.builder()
                            .member(saved)
                            .expenseId(dto.getExpenseId())
                            .sourceType(dto.getSourceType())
                            .title(dto.getTitle() != null ? dto.getTitle() : "Shared Expense")
                            .originalAmount(dto.getOriginalAmount())
                            .allocatedAmount(dto.getAllocatedAmount())
                            .daysCount(dto.getDaysCount())
                            .totalDays(dto.getTotalDays())
                            .notes(dto.getNotes())
                            .build());
                }
            }
            if (!newAllocations.isEmpty()) {
                allocationRepository.saveAll(newAllocations);
            }
        }

        List<MemberExpenseAllocation> memberAllocations = allocationRepository.findByMember(saved);
        return mapToMemberResponse(saved, memberAllocations);
    }

    @Override
    @Transactional
    public PreTripMemberResponse updateMemberTimeline(Long id, PreTripMemberRequest request) {
        TripGroup activeGroup = getActiveTripGroup();
        verifyWriteAccess(activeGroup);

        PreTripMember member = memberRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Member not found with ID: " + id));

        if (!member.getTripGroup().getId().equals(activeGroup.getId())) {
            throw new AccessDeniedException("Access denied to this record");
        }

        if (request.getBudgetLimit() != null) {
            member.setBudgetLimit(request.getBudgetLimit());
        }

        if (request.getSplitMode() != null) {
            member.setSplitMode(request.getSplitMode());
            if (request.getSplitMode() == SplitMode.FROM_START) {
                member.setEffectiveFrom(null);
            } else if (request.getSplitMode() == SplitMode.FROM_NOW) {
                member.setEffectiveFrom(LocalDateTime.now());
            } else if (request.getSplitMode() == SplitMode.CUSTOM_DATE) {
                member.setEffectiveFrom(request.getEffectiveFrom() != null ? request.getEffectiveFrom() : LocalDateTime.now());
            }
        } else if (request.getEffectiveFrom() != null) {
            member.setEffectiveFrom(request.getEffectiveFrom());
            member.setSplitMode(SplitMode.CUSTOM_DATE);
        }

        if (request.getEffectiveTo() != null) {
            member.setEffectiveTo(request.getEffectiveTo());
        }

        if (request.getIsDeparted() != null) {
            member.setIsDeparted(request.getIsDeparted());
        }

        if (request.getSettledOnDeparture() != null) {
            member.setSettledOnDeparture(request.getSettledOnDeparture());
        }

        PreTripMember saved = memberRepository.save(member);

        // Update allocations if explicitly supplied
        if (request.getAllocations() != null) {
            allocationRepository.deleteByMember(saved);
            List<MemberExpenseAllocation> newAllocations = new ArrayList<>();
            for (MemberExpenseAllocationDto dto : request.getAllocations()) {
                if (dto.getAllocatedAmount() != null && dto.getAllocatedAmount().compareTo(BigDecimal.ZERO) > 0) {
                    newAllocations.add(MemberExpenseAllocation.builder()
                            .member(saved)
                            .expenseId(dto.getExpenseId())
                            .sourceType(dto.getSourceType())
                            .title(dto.getTitle() != null ? dto.getTitle() : "Shared Expense")
                            .originalAmount(dto.getOriginalAmount())
                            .allocatedAmount(dto.getAllocatedAmount())
                            .daysCount(dto.getDaysCount())
                            .totalDays(dto.getTotalDays())
                            .notes(dto.getNotes())
                            .build());
                }
            }
            if (!newAllocations.isEmpty()) {
                allocationRepository.saveAll(newAllocations);
            }
        }

        List<MemberExpenseAllocation> memberAllocations = allocationRepository.findByMember(saved);
        return mapToMemberResponse(saved, memberAllocations);
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
        allocationRepository.deleteByMember(member);
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
                .spentBy(request.getSpentBy())
                .expenseDate(request.getExpenseDate())
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
                .orElseThrow(() -> new RuntimeException("Expense not found with ID: " + id));

        if (!expense.getTripGroup().getId().equals(activeGroup.getId())) {
            throw new AccessDeniedException("Access denied to this record");
        }

        expense.setTitle(request.getTitle());
        expense.setAmount(request.getAmount());
        expense.setSpentBy(request.getSpentBy());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setNotes(request.getNotes());

        PreTripExpense saved = expenseRepository.save(expense);
        return mapToExpenseResponse(saved);
    }

    @Override
    @Transactional
    public void deleteExpense(Long id) {
        TripGroup activeGroup = getActiveTripGroup();
        verifyWriteAccess(activeGroup);

        PreTripExpense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found with ID: " + id));

        if (!expense.getTripGroup().getId().equals(activeGroup.getId())) {
            throw new AccessDeniedException("Access denied to this record");
        }
        expenseRepository.delete(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripExpenseHistoryItemDto> getTripHistoryItems() {
        TripGroup activeGroup = getActiveTripGroup();
        List<PreTripExpense> preTrip = expenseRepository.findByTripGroup(activeGroup);
        List<Expense> active = activeExpenseRepository.findByTripGroup(activeGroup);

        List<TripExpenseHistoryItemDto> result = new ArrayList<>();

        for (PreTripExpense pe : preTrip) {
            result.add(TripExpenseHistoryItemDto.builder()
                    .id(pe.getId())
                    .sourceType("PRE_TRIP")
                    .title(pe.getTitle())
                    .amount(pe.getAmount())
                    .category("Pre-Trip Booking")
                    .paidBy(pe.getSpentBy())
                    .expenseDateTime(pe.getExpenseDateTime())
                    .notes(pe.getNotes())
                    .build());
        }

        for (Expense e : active) {
            if (e.getCategory() == Category.SETTLEMENT) continue;
            result.add(TripExpenseHistoryItemDto.builder()
                    .id(e.getId())
                    .sourceType("ACTIVE")
                    .title(e.getTitle())
                    .amount(e.getAmount())
                    .category(e.getCategory().name())
                    .paidBy(e.getPaidBy())
                    .expenseDateTime(e.getExpenseDateTime())
                    .notes(e.getNotes())
                    .build());
        }

        result.sort((a, b) -> {
            if (a.getExpenseDateTime() == null) return -1;
            if (b.getExpenseDateTime() == null) return 1;
            return a.getExpenseDateTime().compareTo(b.getExpenseDateTime());
        });

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public MemberExitBillDto getMemberExitBill(Long memberId) {
        TripGroup activeGroup = getActiveTripGroup();
        PreTripMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found: " + memberId));

        if (!member.getTripGroup().getId().equals(activeGroup.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        List<PreTripMember> allMembers = memberRepository.findByTripGroup(activeGroup);
        List<PreTripExpense> preTripExpenses = expenseRepository.findByTripGroup(activeGroup);
        List<Expense> activeExpenses = activeExpenseRepository.findByTripGroup(activeGroup);
        List<MemberExpenseAllocation> memberAllocations = allocationRepository.findByMember(member);

        // Allocations list
        List<MemberExpenseAllocationDto> allocDtos = memberAllocations.stream()
                .map(this::mapToAllocationDto)
                .collect(Collectors.toList());

        BigDecimal totalAllocated = memberAllocations.stream()
                .map(MemberExpenseAllocation::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Stay expenses
        List<ExitBillItemDto> stayItems = new ArrayList<>();
        BigDecimal totalStayShare = BigDecimal.ZERO;
        BigDecimal totalPaidByMember = BigDecimal.ZERO;

        String memberKey = member.getName().toLowerCase();

        // 1. Pre-trip check
        for (PreTripExpense pe : preTripExpenses) {
            LocalDateTime dt = pe.getExpenseDateTime();
            boolean isEligible = (member.getEffectiveFrom() == null || !member.getEffectiveFrom().isAfter(dt))
                    && (member.getEffectiveTo() == null || !dt.isAfter(member.getEffectiveTo()));

            // If explicitly allocated in history allocations, do not double-count in point-in-time
            boolean alreadyAllocated = memberAllocations.stream()
                    .anyMatch(a -> "PRE_TRIP".equalsIgnoreCase(a.getSourceType()) && pe.getId().equals(a.getExpenseId()));

            if (isEligible && !alreadyAllocated) {
                List<PreTripMember> eligible = allMembers.stream()
                        .filter(m -> (m.getEffectiveFrom() == null || !m.getEffectiveFrom().isAfter(dt))
                                && (m.getEffectiveTo() == null || !dt.isAfter(m.getEffectiveTo())))
                        .toList();
                int count = Math.max(1, eligible.size());
                BigDecimal share = pe.getAmount().divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
                stayItems.add(ExitBillItemDto.builder()
                        .id(pe.getId())
                        .title(pe.getTitle())
                        .category("Pre-Trip Booking")
                        .totalAmount(pe.getAmount())
                        .activeMemberCount(count)
                        .memberShare(share)
                        .expenseDateTime(dt)
                        .paidBy(pe.getSpentBy())
                        .build());
                totalStayShare = totalStayShare.add(share);
            }

            if (pe.getSpentBy() != null && pe.getSpentBy().equalsIgnoreCase(member.getName())) {
                totalPaidByMember = totalPaidByMember.add(pe.getAmount());
            }
        }

        // 2. Active expenses check
        for (Expense e : activeExpenses) {
            if (e.getCategory() == Category.SETTLEMENT) continue;
            LocalDateTime dt = e.getExpenseDateTime();
            boolean isEligible = (member.getEffectiveFrom() == null || !member.getEffectiveFrom().isAfter(dt))
                    && (member.getEffectiveTo() == null || !dt.isAfter(member.getEffectiveTo()));

            boolean alreadyAllocated = memberAllocations.stream()
                    .anyMatch(a -> "ACTIVE".equalsIgnoreCase(a.getSourceType()) && e.getId().equals(a.getExpenseId()));

            if (isEligible && !alreadyAllocated) {
                List<PreTripMember> eligible = allMembers.stream()
                        .filter(m -> (m.getEffectiveFrom() == null || !m.getEffectiveFrom().isAfter(dt))
                                && (m.getEffectiveTo() == null || !dt.isAfter(m.getEffectiveTo())))
                        .toList();
                int count = Math.max(1, eligible.size());
                BigDecimal share = e.getAmount().divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
                stayItems.add(ExitBillItemDto.builder()
                        .id(e.getId())
                        .title(e.getTitle())
                        .category(e.getCategory().name())
                        .totalAmount(e.getAmount())
                        .activeMemberCount(count)
                        .memberShare(share)
                        .expenseDateTime(dt)
                        .paidBy(e.getPaidBy())
                        .build());
                totalStayShare = totalStayShare.add(share);
            }

            if (e.getPaidBy() != null && e.getPaidBy().equalsIgnoreCase(member.getName())) {
                totalPaidByMember = totalPaidByMember.add(e.getAmount());
            }
        }

        // 3. Settlement expenses check
        for (Expense e : activeExpenses) {
            if (e.getCategory() == Category.SETTLEMENT && e.getPaidBy() != null) {
                if (e.getPaidBy().equalsIgnoreCase(member.getName())) {
                    totalPaidByMember = totalPaidByMember.add(e.getAmount());
                }
            }
        }

        BigDecimal totalOwed = totalAllocated.add(totalStayShare);
        BigDecimal netDue = totalOwed.subtract(totalPaidByMember);
        String suggestedPayee = activeGroup.getCreator().getUsername();

        return MemberExitBillDto.builder()
                .memberId(member.getId())
                .memberName(member.getName())
                .effectiveFrom(member.getEffectiveFrom())
                .effectiveTo(member.getEffectiveTo())
                .isDeparted(member.getIsDeparted())
                .settledOnDeparture(member.getSettledOnDeparture())
                .historyAllocations(allocDtos)
                .stayExpenses(stayItems)
                .totalAllocatedFromHistory(totalAllocated)
                .totalStayExpensesShare(totalStayShare)
                .totalOwed(totalOwed)
                .totalPaid(totalPaidByMember)
                .netDue(netDue)
                .suggestedPayee(suggestedPayee)
                .build();
    }

    @Override
    @Transactional
    public MemberExitBillDto checkoutMember(Long memberId) {
        TripGroup activeGroup = getActiveTripGroup();
        verifyWriteAccess(activeGroup);

        PreTripMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found: " + memberId));

        if (!member.getTripGroup().getId().equals(activeGroup.getId())) {
            throw new AccessDeniedException("Access denied");
        }

        MemberExitBillDto bill = getMemberExitBill(memberId);

        // Record a settlement if netDue > 0
        if (bill.getNetDue() != null && bill.getNetDue().compareTo(new BigDecimal("0.01")) > 0) {
            String payee = bill.getSuggestedPayee();
            Expense settlement = Expense.builder()
                    .title("Early Checkout Settlement: " + member.getName() + " -> " + payee)
                    .amount(bill.getNetDue())
                    .category(Category.SETTLEMENT)
                    .paymentMode(PaymentMode.UPI)
                    .paidBy(member.getName())
                    .expenseDate(java.time.LocalDate.now())
                    .expenseTime(java.time.LocalTime.now())
                    .notes("Settle:" + member.getName() + ":" + payee)
                    .tripGroup(activeGroup)
                    .build();
            activeExpenseRepository.save(settlement);
        }

        member.setIsDeparted(true);
        member.setSettledOnDeparture(true);
        if (member.getEffectiveTo() == null) {
            member.setEffectiveTo(LocalDateTime.now());
        }
        memberRepository.save(member);

        return getMemberExitBill(memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public PreTripSummaryDto getSummary() {
        TripGroup activeGroup = getActiveTripGroup();
        List<PreTripExpense> preTripExpenses = expenseRepository.findByTripGroup(activeGroup);
        List<Expense> activeExpenses = activeExpenseRepository.findByTripGroup(activeGroup);
        List<PreTripMember> members = memberRepository.findByTripGroup(activeGroup);
        List<MemberExpenseAllocation> allAllocations = allocationRepository.findByMemberIn(members);

        // Total spent across pre-trip and active expenses
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

        // Calculate sharePerMember as nominal average
        BigDecimal sharePerMember = members.isEmpty() ? BigDecimal.ZERO :
                totalSpent.divide(BigDecimal.valueOf(members.size()), 2, RoundingMode.HALF_UP);

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

        // 1. Group allocations by expense
        Map<Long, List<MemberExpenseAllocation>> preTripAllocMap = allAllocations.stream()
                .filter(a -> "PRE_TRIP".equalsIgnoreCase(a.getSourceType()))
                .collect(Collectors.groupingBy(MemberExpenseAllocation::getExpenseId));

        Map<Long, List<MemberExpenseAllocation>> activeAllocMap = allAllocations.stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getSourceType()))
                .collect(Collectors.groupingBy(MemberExpenseAllocation::getExpenseId));

        // 2. Process Pre-Trip Expenses
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

            // Remainder split across other eligible members
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

        // 3. Process Active Trip Expenses (excluding settlements)
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

        // 4. Adjust for settlements
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

    private PreTripMemberResponse mapToMemberResponse(PreTripMember member, List<MemberExpenseAllocation> allocations) {
        List<MemberExpenseAllocationDto> allocDtos = allocations.stream()
                .map(this::mapToAllocationDto)
                .collect(Collectors.toList());

        return PreTripMemberResponse.builder()
                .id(member.getId())
                .name(member.getName())
                .budgetLimit(member.getBudgetLimit())
                .effectiveFrom(member.getEffectiveFrom())
                .effectiveTo(member.getEffectiveTo())
                .splitMode(member.getSplitMode())
                .isDeparted(member.getIsDeparted())
                .settledOnDeparture(member.getSettledOnDeparture())
                .createdAt(member.getCreatedAt())
                .allocations(allocDtos)
                .build();
    }

    private MemberExpenseAllocationDto mapToAllocationDto(MemberExpenseAllocation alloc) {
        return MemberExpenseAllocationDto.builder()
                .id(alloc.getId())
                .expenseId(alloc.getExpenseId())
                .sourceType(alloc.getSourceType())
                .title(alloc.getTitle())
                .originalAmount(alloc.getOriginalAmount())
                .allocatedAmount(alloc.getAllocatedAmount())
                .daysCount(alloc.getDaysCount())
                .totalDays(alloc.getTotalDays())
                .notes(alloc.getNotes())
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
