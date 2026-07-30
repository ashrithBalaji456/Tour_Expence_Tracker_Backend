package com.tripexpense.tracker.controller;

import com.tripexpense.tracker.dto.FundRequest;
import com.tripexpense.tracker.dto.FundResponse;
import com.tripexpense.tracker.service.FundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funds")
@RequiredArgsConstructor
public class FundController {

    private final FundService fundService;

    @PostMapping
    public ResponseEntity<FundResponse> addFund(@Valid @RequestBody FundRequest request) {
        FundResponse response = fundService.addFund(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<FundResponse>> getAllFunds() {
        List<FundResponse> funds = fundService.getAllFunds();
        return ResponseEntity.ok(funds);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FundResponse> updateFund(
            @PathVariable Long id,
            @RequestBody FundRequest request
    ) {
        FundResponse response = fundService.updateFund(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFund(@PathVariable Long id) {
        fundService.deleteFund(id);
        return ResponseEntity.noContent().build();
    }
}
