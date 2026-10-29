package com.akt.banking_v1.dto;

import com.akt.banking_v1.enums.AccountStatus;
import com.akt.banking_v1.enums.AccountType;
import com.akt.banking_v1.enums.Currency;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(

        Long id,
        String accountNumber,
        BigDecimal balance,
        Currency currency,
        AccountStatus status,
        AccountType accountType,
        Long customerId,
        Instant createdAt
) {
}
