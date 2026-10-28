package com.akt.banking_v1.service;

import com.akt.banking_v1.common.exception.*;
import com.akt.banking_v1.dto.TransferRequest;
import com.akt.banking_v1.dto.TransferResponse;
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

        Optional<Transaction> existingTx = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTx.isPresent())
            return transferMapper.toResponse(existingTx.get());

        if (request.senderAccountNumber().equals(request.receiverAccountNumber()))
            throw new SelfTransferException("Sender and receiver accounts are the same!");

        boolean isSenderFirst = request.senderAccountNumber().compareTo(request.receiverAccountNumber()) < 0;

        String firstAccountNumber = isSenderFirst ? request.senderAccountNumber() : request.receiverAccountNumber();
        String secondAccountNumber = isSenderFirst ? request.receiverAccountNumber() : request.senderAccountNumber();

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

        if (sender.getBalance().compareTo(request.amount()) < 0)
            throw new InsufficientBalanceException("Insufficient balance!");

        sender.setBalance(sender.getBalance().subtract(request.amount()));
        receiver.setBalance(receiver.getBalance().add(request.amount()));

        String referenceNumber = "TXN-"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Transaction transaction = Transaction.builder()
                .idempotencyKey(idempotencyKey)
                .referenceNumber(referenceNumber)
                .type(TransactionType.INTERNAL_TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .amount(request.amount())
                .currency(sender.getCurrency())
                .description(request.description())
                .build();

        LedgerEntry debitEntry = LedgerEntry.builder()
                .account(sender)
                .entryType(EntryType.DEBIT)
                .amount(request.amount())
                .transaction(transaction)
                .build();

        LedgerEntry creditEntry = LedgerEntry.builder()
                .account(receiver)
                .entryType(EntryType.CREDIT)
                .amount(request.amount())
                .transaction(transaction)
                .build();

        transaction.getLedgerEntries().add(debitEntry);
        transaction.getLedgerEntries().add(creditEntry);

        Transaction savedTransaction = transactionRepository.save(transaction);
        return transferMapper.toResponse(savedTransaction);
    }
}
