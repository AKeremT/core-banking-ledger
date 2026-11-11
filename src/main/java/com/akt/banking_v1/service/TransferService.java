package com.akt.banking_v1.service;

import com.akt.banking_v1.common.exception.*;
import com.akt.banking_v1.config.DataInitializer;
import com.akt.banking_v1.dto.DepositRequest;
import com.akt.banking_v1.dto.TransferRequest;
import com.akt.banking_v1.dto.TransferResponse;
import com.akt.banking_v1.dto.WithdrawRequest;
import com.akt.banking_v1.enums.AccountStatus;
import com.akt.banking_v1.enums.EntryType;
import com.akt.banking_v1.enums.TransactionStatus;
import com.akt.banking_v1.enums.TransactionType;
import com.akt.banking_v1.mapper.TransferMapper;
import com.akt.banking_v1.model.Account;
import com.akt.banking_v1.model.LedgerEntry;
import com.akt.banking_v1.model.Transaction;
import com.akt.banking_v1.repository.AccountRepository;
import com.akt.banking_v1.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferMapper transferMapper;

    @Transactional(rollbackFor = Exception.class)
    public TransferResponse transfer(TransferRequest request, String idempotencyKey) {

        return executeTransfer(
                request.senderAccountNumber(),
                request.receiverAccountNumber(),
                request.amount(),
                request.description(),
                idempotencyKey,
                TransactionType.INTERNAL_TRANSFER,
                "TXN-"
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public TransferResponse deposit(DepositRequest request, String idempotencyKey) {

        return executeTransfer(
                DataInitializer.SYSTEM_TRY_ACCOUNT,
                request.accountNumber(),
                request.amount(),
                request.description(),
                idempotencyKey,
                TransactionType.DEPOSIT,
                "DEP-"
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public TransferResponse withdraw(WithdrawRequest request, String idempotencyKey) {

        return executeTransfer(
                request.accountNumber(),
                DataInitializer.SYSTEM_TRY_ACCOUNT,
                request.amount(),
                request.description(),
                idempotencyKey,
                TransactionType.WITHDRAW,
                "WDR-"
        );
    }

    private TransferResponse executeTransfer(
            String senderAccountNumber,
            String receiverAccountNumber,
            BigDecimal amount,
            String description,
            String idempotencyKey,
            TransactionType transactionType,
            String referencePrefix
    ) {

        Optional<Transaction> existingTx = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTx.isPresent())
            return transferMapper.toResponse(existingTx.get());

        if (senderAccountNumber.equals(receiverAccountNumber))
            throw new SelfTransferException("Sender and receiver accounts are the same!");

        boolean isSenderFirst = senderAccountNumber.compareTo(receiverAccountNumber) < 0;

        String firstAccountNumber = isSenderFirst ? senderAccountNumber : receiverAccountNumber;
        String secondAccountNumber = isSenderFirst ? receiverAccountNumber : senderAccountNumber;

        Account firstAccount = accountRepository.findByAccountNumberWithLock(firstAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + firstAccountNumber));

        Account secondAccount = accountRepository.findByAccountNumberWithLock(secondAccountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + secondAccountNumber));

        Account sender = isSenderFirst ? firstAccount : secondAccount;
        Account receiver = isSenderFirst ? secondAccount : firstAccount;

        if (sender.getStatus() != AccountStatus.ACTIVE || receiver.getStatus() != AccountStatus.ACTIVE)
            throw new AccountInactiveException("Inactive accounts!");

        if (sender.getCurrency() != receiver.getCurrency())
            throw new CurrencyMismatchException("Currency mismatch!");

        if (sender.getBalance().compareTo(amount) < 0)
            throw new InsufficientBalanceException("Insufficient balance!");

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        String referenceNumber = referencePrefix
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Transaction transaction = Transaction.builder()
                .idempotencyKey(idempotencyKey)
                .referenceNumber(referenceNumber)
                .type(transactionType)
                .status(TransactionStatus.COMPLETED)
                .amount(amount)
                .currency(sender.getCurrency())
                .description(description)
                .build();

        LedgerEntry debitEntry = LedgerEntry.builder()
                .account(sender)
                .entryType(EntryType.DEBIT)
                .amount(amount)
                .transaction(transaction)
                .build();

        LedgerEntry creditEntry = LedgerEntry.builder()
                .account(receiver)
                .entryType(EntryType.CREDIT)
                .amount(amount)
                .transaction(transaction)
                .build();

        transaction.getLedgerEntries().add(debitEntry);
        transaction.getLedgerEntries().add(creditEntry);

        Transaction savedTransaction = transactionRepository.save(transaction);
        return transferMapper.toResponse(savedTransaction);
    }
}
