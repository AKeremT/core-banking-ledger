package com.akt.banking_v1.dto;

import com.akt.banking_v1.enums.Currency;
import com.akt.banking_v1.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(

        UUID transactionId,
        String referenceNumber,
        TransactionStatus status,
        String senderAccountNumber,
        String receiverAccountNumber,
        BigDecimal amount,
        Currency currency,
        Instant createdAt
) {
}
