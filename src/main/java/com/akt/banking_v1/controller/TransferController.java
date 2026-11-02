package com.akt.banking_v1.controller;

import com.akt.banking_v1.dto.DepositRequest;
import com.akt.banking_v1.dto.TransferRequest;
import com.akt.banking_v1.dto.TransferResponse;
import com.akt.banking_v1.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> transfer(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                     @Valid @RequestBody TransferRequest transferRequest) {

        return ResponseEntity.ok(transferService.transfer(transferRequest, idempotencyKey));
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransferResponse> deposit(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                    @Valid @RequestBody DepositRequest depositRequest) {

        return ResponseEntity.ok(transferService.deposit(depositRequest, idempotencyKey));
    }
}
