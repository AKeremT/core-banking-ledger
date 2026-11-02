package com.akt.banking_v1.dto;

import com.akt.banking_v1.enums.EntryType;
import com.akt.banking_v1.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record StatementItemResponse(

        String referenceNumber,
        EntryType entryType,
        TransactionType transactionType,
        BigDecimal amount,
        String description,
        Instant createdAt
) {
}
