package com.akt.banking_v1.controller;

import com.akt.banking_v1.dto.AccountResponse;
import com.akt.banking_v1.dto.AccountStatementResponse;
import com.akt.banking_v1.dto.CreateAccountRequest;
import com.akt.banking_v1.service.AccountService;
import com.akt.banking_v1.service.AccountStatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Accounts", description = "Account creation, and statement analysis endpoints")
public class AccountController {

    private final AccountService accountService;
    private final AccountStatementService accountStatementService;

    @Operation(
            summary = "Open a new bank account",
            description = "Creates a new bank account with a collision-guarded 24-digit Turkish IBAN and initial" +
                    "zero balance."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody @Valid CreateAccountRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    @Operation(summary = "Get account details by IBAN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(
            @Parameter(description = "Account number / IBAN", example = "TR330006100511123456789012")
            @PathVariable("accountNumber") String accountNumber) {

        return ResponseEntity.ok(accountService.getAccountByAccountNumber(accountNumber));
    }

    @Operation(summary = "List all accounts belonging to a customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountResponse>> getAccountByCustomer(@PathVariable("customerId") Long customerId) {

        return ResponseEntity.ok(accountService.getAccountsByCustomerId(customerId));
    }

    @Operation(
            summary = "Generate account statement (Ekstre)",
            description = "Calculates total debit/credit, category breakdown via Java Stream API for the given " +
                    "date range (defaults to last 30 days)."
            )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statement generated successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @GetMapping("/{accountNumber}/statement")
    public ResponseEntity<AccountStatementResponse> getStatement(
            @Parameter(description = "Account number / IBAN", example = "TR330006100511123456789012")
            @PathVariable String accountNumber,
            @Parameter(description = "Start date (UTC ISO-8601)", example = "2026-10-01T00:00:00Z")
            @RequestParam(required = false) Instant startDate,
            @Parameter(description = "End date (UTC ISO-8601)", example = "2026-11-03T23:59:59Z")
            @RequestParam(required = false) Instant endDate) {

        return ResponseEntity.ok(accountStatementService.getStatement(accountNumber, startDate, endDate));
    }
}
