package com.akt.banking_v1.service;

import com.akt.banking_v1.common.exception.AccountNotFoundException;
import com.akt.banking_v1.dto.AccountStatementResponse;
import com.akt.banking_v1.dto.StatementItemResponse;
import com.akt.banking_v1.enums.EntryType;
import com.akt.banking_v1.enums.TransactionType;
import com.akt.banking_v1.model.Account;
import com.akt.banking_v1.model.LedgerEntry;
import com.akt.banking_v1.repository.AccountRepository;
import com.akt.banking_v1.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountStatementService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public AccountStatementResponse getStatement(String accountNumber, Instant startDate, Instant endDate) {

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with: " + accountNumber));

        Instant effectiveStartDate = (startDate != null) ? startDate : Instant.now().minus(30, ChronoUnit.DAYS);
        Instant effectiveEndDate = (endDate != null) ? endDate : Instant.now();

        List<LedgerEntry> entries = ledgerEntryRepository.findByAccountIdAndDateRange(account.getId(), effectiveStartDate, effectiveEndDate);

        BigDecimal totalCredit = entries
                .stream()
                .filter(e -> e.getEntryType() == EntryType.CREDIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDebit = entries
                .stream()
                .filter(e -> e.getEntryType() == EntryType.DEBIT)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netChange = totalCredit.subtract(totalDebit);

        Map<TransactionType, BigDecimal> summaryByType = entries.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getTransaction().getType(),
                        Collectors.reducing(BigDecimal.ZERO, LedgerEntry::getAmount, BigDecimal::add)
                ));

        List<StatementItemResponse> items = entries.stream()
                .map(e -> new StatementItemResponse(
                        e.getTransaction().getReferenceNumber(),
                        e.getEntryType(),
                        e.getTransaction().getType(),
                        e.getAmount(),
                        e.getTransaction().getDescription(),
                        e.getCreatedAt()
                ))
                .toList();

        return new AccountStatementResponse(
                accountNumber,
                account.getCurrency(),
                account.getBalance(),
                effectiveStartDate,
                effectiveEndDate,
                totalCredit,
                totalDebit,
                netChange,
                summaryByType,
                items
        );
    }

    public AccountStatementResponse getStatement(String accountNumber) {

        return getStatement(accountNumber, null, null);
    }
}
