package com.akt.banking_v1.dto;

import com.akt.banking_v1.enums.Currency;
import com.akt.banking_v1.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AccountStatementResponse(

        String accountNumber,
        Currency currency,
        BigDecimal currentBalance,
        Instant startDate,
        Instant endDate,
        BigDecimal totalCredit,
        BigDecimal totalDebit,
        BigDecimal netChange,
        Map<TransactionType, BigDecimal> summaryByType,
        List<StatementItemResponse> items
) {
}
