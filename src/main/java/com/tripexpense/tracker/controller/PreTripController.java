package com.tripexpense.tracker.controller;

import com.tripexpense.tracker.dto.*;
import com.tripexpense.tracker.service.PreTripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pretrip")
@RequiredArgsConstructor
public class PreTripController {

    private final PreTripService preTripService;

    // Members
    @GetMapping("/members")
    public ResponseEntity<List<PreTripMemberResponse>> getAllMembers() {
        return ResponseEntity.ok(preTripService.getAllMembers());
    }

    @PostMapping("/members")
    public ResponseEntity<PreTripMemberResponse> saveMember(@RequestBody PreTripMemberRequest request) {
        return new ResponseEntity<>(preTripService.saveMember(request), HttpStatus.CREATED);
    }

    @DeleteMapping("/members/{id}")
    public ResponseEntity<Void> deleteMember(@PathVariable Long id) {
        preTripService.deleteMember(id);
        return ResponseEntity.noContent().build();
    }

    // Expenses
    @GetMapping("/expenses")
    public ResponseEntity<List<PreTripExpenseResponse>> getAllExpenses() {
        return ResponseEntity.ok(preTripService.getAllExpenses());
    }

    @PostMapping("/expenses")
    public ResponseEntity<PreTripExpenseResponse> createExpense(@RequestBody PreTripExpenseRequest request) {
        return new ResponseEntity<>(preTripService.createExpense(request), HttpStatus.CREATED);
    }

    @PutMapping("/expenses/{id}")
    public ResponseEntity<PreTripExpenseResponse> updateExpense(
            @PathVariable Long id,
            @RequestBody PreTripExpenseRequest request
    ) {
        return ResponseEntity.ok(preTripService.updateExpense(id, request));
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        preTripService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    // Summary calculations
    @GetMapping("/summary")
    public ResponseEntity<PreTripSummaryDto> getSummary() {
        return ResponseEntity.ok(preTripService.getSummary());
    }
}
