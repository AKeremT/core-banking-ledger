package com.akt.banking_v1.service;

import com.akt.banking_v1.common.exception.*;
import com.akt.banking_v1.dto.TransferRequest;
import com.akt.banking_v1.dto.TransferResponse;
import com.akt.banking_v1.enums.AccountStatus;
import com.akt.banking_v1.enums.Currency;
import com.akt.banking_v1.enums.TransactionStatus;
import com.akt.banking_v1.enums.TransactionType;
import com.akt.banking_v1.mapper.TransferMapper;
import com.akt.banking_v1.model.Account;
import com.akt.banking_v1.model.Transaction;
import com.akt.banking_v1.repository.AccountRepository;
import com.akt.banking_v1.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransferMapper transferMapper;

    @InjectMocks
    private TransferService transferService;

    @Test
    void transfer_whenValidRequest_shouldCompleteSuccessfully() {

        String senderIban = "TR111";
        String receiverIban = "TR222";
        String idempotencyKey = "idempotencyKey";
        BigDecimal transferAmount = new BigDecimal("100");

        Account sender = Account.builder()
                .id(1L)
                .accountNumber(senderIban)
                .balance(new BigDecimal("500"))
                .currency(Currency.TRY)
                .status(AccountStatus.ACTIVE)
                .build();

        Account receiver = Account.builder()
                .id(2L)
                .accountNumber(receiverIban)
                .balance(new BigDecimal("500"))
                .currency(Currency.TRY)
                .status(AccountStatus.ACTIVE)
                .build();

        TransferRequest request = new TransferRequest(senderIban, receiverIban, transferAmount, "kira");

        when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

        when(accountRepository.findByAccountNumberWithLock(senderIban)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumberWithLock(receiverIban)).thenReturn(Optional.of(receiver));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        TransferResponse mockResponse = new TransferResponse(
                UUID.randomUUID(),
                "TXN-123",
                TransactionStatus.COMPLETED,
                senderIban,
                receiverIban,
                transferAmount,
                Currency.TRY,
                Instant.now()
        );

        when(transferMapper.toResponse(any(Transaction.class))).thenReturn(mockResponse);

        TransferResponse result = transferService.transfer(request, idempotencyKey);

        assertEquals("TXN-123", result.referenceNumber());
        assertEquals(TransactionStatus.COMPLETED, result.status());

        assertNotNull(result, "Transfer result should not be null");
        assertEquals(0, new BigDecimal("400.00").compareTo(sender.getBalance()),
                "Sender balance should be 400.00");
        assertEquals(0, new BigDecimal("600.00").compareTo(receiver.getBalance()),
                "Receiver balance should be 600.00");

        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(transferMapper, times(1)).toResponse(any(Transaction.class));
    }

    @Test
    void transfer_whenIdempotencyKeyIsPresent_shouldReturnExistingTransfer() {

        String senderIban = "TR111";
        String receiverIban = "TR222";
        String idempotencyKey = "idempotencyKey";
        BigDecimal transferAmount = new BigDecimal("100");
        String description = "kira";

        TransferRequest request = new TransferRequest(senderIban, receiverIban, transferAmount, description);

        Transaction existingTx = Transaction.builder()
                .idempotencyKey(idempotencyKey)
                .referenceNumber("AKT123")
                .type(TransactionType.INTERNAL_TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .amount(transferAmount)
                .currency(Currency.TRY)
                .description(description)
                .build();

        TransferResponse mockResponse = new TransferResponse(
                UUID.randomUUID(),
                "AKT123",
                TransactionStatus.COMPLETED,
                senderIban,
                receiverIban,
                transferAmount,
                Currency.TRY,
                Instant.now()
        );

        when(transactionRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingTx));

        when(transferMapper.toResponse(existingTx)).thenReturn(mockResponse);

        TransferResponse result = transferService.transfer(request, idempotencyKey);

        assertNotNull(result, "Transfer result should not be null");
        assertEquals("AKT123", result.referenceNumber());

        verify(accountRepository, never()).findByAccountNumberWithLock(any());
        verify(transactionRepository, never()).save(any());
        verify(transferMapper, times(1)).toResponse(existingTx);
    }

    @Test
    void transfer_whenSenderAndReceiverSame_shouldThrowSelfTransferException() {

        String senderIban = "TR111";
        String receiverIban = "TR111";
        String idempotencyKey = "idempotencyKey";
        BigDecimal transferAmount = new BigDecimal("100");
        String description = "kira";

        TransferRequest request = new TransferRequest(senderIban, receiverIban, transferAmount, description);

        when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

        assertThrows(SelfTransferException.class, () -> transferService.transfer(request, idempotencyKey));

        verify(accountRepository, never()).findByAccountNumberWithLock(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_whenInsufficientBalance_shouldThrowInsufficientBalanceException() {

        String senderIban = "TR111";
        String receiverIban = "TR222";
        String idempotencyKey = "idempotencyKey";
        BigDecimal transferAmount = new BigDecimal("100");
        String description = "kira";

        Account sender = Account.builder()
                .id(1L)
                .accountNumber(senderIban)
                .balance(new BigDecimal("50"))
                .currency(Currency.TRY)
                .status(AccountStatus.ACTIVE)
                .build();

        Account receiver = Account.builder()
                .id(2L)
                .accountNumber(receiverIban)
                .balance(new BigDecimal("500"))
                .currency(Currency.TRY)
                .status(AccountStatus.ACTIVE)
                .build();

        TransferRequest request = new TransferRequest(senderIban, receiverIban, transferAmount, description);

        when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

        when(accountRepository.findByAccountNumberWithLock(senderIban)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumberWithLock(receiverIban)).thenReturn(Optional.of(receiver));

        assertThrows(InsufficientBalanceException.class, () -> transferService.transfer(request, idempotencyKey));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_whenAccountNotFound_shouldThrowAccountNotFoundException() {

        String senderIban = "TR111";
        String receiverIban = "TR222";
        String idempotencyKey = "idempotencyKey";
        BigDecimal transferAmount = new BigDecimal("100");
        String description = "kira";

        TransferRequest request = new TransferRequest(senderIban, receiverIban, transferAmount, description);

        when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumberWithLock(senderIban)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> transferService.transfer(request, idempotencyKey));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_whenAccountIsInactive_shouldThrowAccountInactiveException() {

        String senderIban = "TR111";
        String receiverIban = "TR222";
        String idempotencyKey = "idempotencyKey";
        BigDecimal transferAmount = new BigDecimal("100");
        String description = "kira";

        Account sender = Account.builder()
                .id(1L)
                .accountNumber(senderIban)
                .balance(new BigDecimal("50"))
                .currency(Currency.TRY)
                .status(AccountStatus.SUSPENDED)
                .build();

        Account receiver = Account.builder()
                .id(2L)
                .accountNumber(receiverIban)
                .balance(new BigDecimal("500"))
                .currency(Currency.TRY)
                .status(AccountStatus.CLOSED)
                .build();

        TransferRequest request = new TransferRequest(senderIban, receiverIban, transferAmount, "kira");

        when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

        when(accountRepository.findByAccountNumberWithLock(senderIban)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumberWithLock(receiverIban)).thenReturn(Optional.of(receiver));

        assertThrows(AccountInactiveException.class, () -> transferService.transfer(request, idempotencyKey));

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_whenCurrencyMismatch_shouldThrowCurrencyMismatchException() {

        String senderIban = "TR111";
        String receiverIban = "TR222";
        String idempotencyKey = "idempotencyKey";
        BigDecimal transferAmount = new BigDecimal("100");
        String description = "kira";

        Account sender = Account.builder()
                .id(1L)
                .accountNumber(senderIban)
                .balance(new BigDecimal("50"))
                .currency(Currency.USD)
                .status(AccountStatus.ACTIVE)
                .build();

        Account receiver = Account.builder()
                .id(2L)
                .accountNumber(receiverIban)
                .balance(new BigDecimal("500"))
                .currency(Currency.EUR)
                .status(AccountStatus.ACTIVE)
                .build();

        TransferRequest request = new TransferRequest(senderIban, receiverIban, transferAmount, "kira");

        when(transactionRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

        when(accountRepository.findByAccountNumberWithLock(senderIban)).thenReturn(Optional.of(sender));
        when(accountRepository.findByAccountNumberWithLock(receiverIban)).thenReturn(Optional.of(receiver));

        assertThrows(CurrencyMismatchException.class, () -> transferService.transfer(request, idempotencyKey));

        verify(transactionRepository, never()).save(any());
    }
}
