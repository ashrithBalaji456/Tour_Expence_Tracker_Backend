package com.tripexpense.tracker.service.impl;

import com.tripexpense.tracker.dto.ExpenseRequest;
import com.tripexpense.tracker.dto.ExpenseResponse;
import com.tripexpense.tracker.entity.Category;
import com.tripexpense.tracker.entity.Expense;
import com.tripexpense.tracker.entity.TripGroup;
import com.tripexpense.tracker.entity.User;
import com.tripexpense.tracker.repository.ExpenseRepository;
import com.tripexpense.tracker.repository.TripGroupRepository;
import com.tripexpense.tracker.repository.UserRepository;
import com.tripexpense.tracker.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
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
    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        TripGroup activeGroup = getActiveTripGroup();
        verifyWriteAccess(activeGroup);

        Expense expense = Expense.builder()
                .title(request.getTitle())
                .amount(request.getAmount())
                .category(request.getCategory())
                .paymentMode(request.getPaymentMode())
                .paidBy(request.getPaidBy() != null && !request.getPaidBy().isBlank() ? request.getPaidBy() : "Group Pool")
                .expenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : LocalDate.now())
                .notes(request.getNotes())
                .tripGroup(activeGroup)
                .build();

        Expense saved = expenseRepository.save(expense);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getAllExpenses() {
        TripGroup activeGroup = getActiveTripGroup();
        return expenseRepository.findByTripGroup(activeGroup).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id) {
        TripGroup activeGroup = getActiveTripGroup();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found with ID: " + id));
        
        if (!expense.getTripGroup().getId().equals(activeGroup.getId())) {
            throw new AccessDeniedException("Access denied to this record");
        }
        return mapToResponse(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByCategory(Category category) {
        TripGroup activeGroup = getActiveTripGroup();
        return expenseRepository.findByCategoryAndTripGroup(category, activeGroup).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpensesByDate(LocalDate date) {
        TripGroup activeGroup = getActiveTripGroup();
        return expenseRepository.findByExpenseDateAndTripGroup(date, activeGroup).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request) {
        TripGroup activeGroup = getActiveTripGroup();
        verifyWriteAccess(activeGroup);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found with ID: " + id));

        if (!expense.getTripGroup().getId().equals(activeGroup.getId())) {
            throw new AccessDeniedException("Access denied to this record");
        }

        expense.setTitle(request.getTitle());
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setPaymentMode(request.getPaymentMode());
        if (request.getPaidBy() != null && !request.getPaidBy().isBlank()) {
            expense.setPaidBy(request.getPaidBy());
        }
        if (request.getExpenseDate() != null) {
            expense.setExpenseDate(request.getExpenseDate());
        }
        expense.setNotes(request.getNotes());

        Expense updated = expenseRepository.save(expense);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteExpense(Long id) {
        TripGroup activeGroup = getActiveTripGroup();
        verifyWriteAccess(activeGroup);

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found with ID: " + id));

        if (!expense.getTripGroup().getId().equals(activeGroup.getId())) {
            throw new AccessDeniedException("Access denied to this record");
        }

        expenseRepository.delete(expense);
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .title(expense.getTitle())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .paymentMode(expense.getPaymentMode())
                .paidBy(expense.getPaidBy())
                .expenseDate(expense.getExpenseDate())
                .notes(expense.getNotes())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
