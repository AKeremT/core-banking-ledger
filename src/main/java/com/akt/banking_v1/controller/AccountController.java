package com.akt.banking_v1.controller;

import com.akt.banking_v1.dto.AccountResponse;
import com.akt.banking_v1.dto.AccountStatementResponse;
import com.akt.banking_v1.dto.CreateAccountRequest;
import com.akt.banking_v1.service.AccountService;
import com.akt.banking_v1.service.AccountStatementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountStatementService accountStatementService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody @Valid CreateAccountRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable("accountNumber") String accountNumber) {

        return ResponseEntity.ok(accountService.getAccountByAccountNumber(accountNumber));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountResponse>> getAccountByCustomer(@PathVariable("customerId") Long customerId) {

        return ResponseEntity.ok(accountService.getAccountsByCustomerId(customerId));
    }

    @GetMapping("/{accountNumber}/statement")
    public ResponseEntity<AccountStatementResponse> getStatement(
            @PathVariable String accountNumber,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate) {

        return ResponseEntity.ok(accountStatementService.getStatement(accountNumber, startDate, endDate));
    }
}
