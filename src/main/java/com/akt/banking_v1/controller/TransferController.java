package com.akt.banking_v1.controller;

import com.akt.banking_v1.dto.DepositRequest;
import com.akt.banking_v1.dto.TransferRequest;
import com.akt.banking_v1.dto.TransferResponse;
import com.akt.banking_v1.dto.WithdrawRequest;
import com.akt.banking_v1.service.TransferService;
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

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers & Cash Operations", description = "Endpoints for inter-account fund transfer, cash deposits," +
        "and cash withdrawals with idempotency and deadlock-free locking.")
public class TransferController {

    private final TransferService transferService;

    @Operation(
            summary = "Execute internal money transfer",
            description = "Performs an atomic double-entry transfer between two accounts with deadlock-free lock " +
                    "ordering and idempotency protection."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transfer completed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error, insufficient balance, or currency mismatch"),
            @ApiResponse(responseCode = "404", description = "Sender or receiver account not found"),
            @ApiResponse(responseCode = "409", description = "Self-transfer or inactive account error")
    })
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @Parameter(description = "Unique client-generated idempotency key (UUID)", required = true,
                    example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest transferRequest) {

        return ResponseEntity.ok(transferService.transfer(transferRequest, idempotencyKey));
    }

    @Operation(
            summary = "Deposit cash into account",
            description = "Deposits cash into a customer account by debiting the central bank vault and crediting " +
                    "the customer account."
            )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deposit completed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid amount"),
            @ApiResponse(responseCode = "404", description = "Customer account not found")
    })
    @PostMapping("/deposit")
    public ResponseEntity<TransferResponse> deposit(
            @Parameter(description = "Unique client-generated idempotency key (UUID)", required = true, example =
                    "c8f12a10-23a5-4f3b-b72e-8d5932a3f789")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DepositRequest depositRequest) {

        return ResponseEntity.ok(transferService.deposit(depositRequest, idempotencyKey));
    }

    @Operation(
            summary = "Withdraw cash from account",
            description = "Withdraws cash from a customer account by debiting the customer account and crediting " +
                    "the central bank vault."
            )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Withdrawal completed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or insufficient balance"),
            @ApiResponse(responseCode = "404", description = "Customer account not found")
    })
    @PostMapping("/withdraw")
    public ResponseEntity<TransferResponse> withdraw(
            @Parameter(description = "Unique client-generated idempotency key (UUID)", required = true, example =
                    "e5b721df-6b83-4a11-a832-7c2a514d9b41")
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WithdrawRequest withdrawRequest) {

        return ResponseEntity.ok(transferService.withdraw(withdrawRequest, idempotencyKey));
    }
}
