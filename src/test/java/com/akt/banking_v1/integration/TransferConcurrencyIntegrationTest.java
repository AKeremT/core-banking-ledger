package com.akt.banking_v1.integration;

import com.akt.banking_v1.dto.TransferRequest;
import com.akt.banking_v1.enums.AccountStatus;
import com.akt.banking_v1.enums.AccountType;
import com.akt.banking_v1.enums.Currency;
import com.akt.banking_v1.model.Account;
import com.akt.banking_v1.model.Customer;
import com.akt.banking_v1.repository.AccountRepository;
import com.akt.banking_v1.repository.CustomerRepository;
import com.akt.banking_v1.repository.TransactionRepository;
import com.akt.banking_v1.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.akt.banking_v1.TestcontainersConfiguration;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
public class TransferConcurrencyIntegrationTest {

    @Autowired
    private TransferService transferService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void testDeadlockFreeCrossTransfersConcurrently() throws InterruptedException {

        Customer customerA = Customer.builder()
                .identityNumber("123")
                .firstName("Kerem")
                .lastName("Temel")
                .email("akt@gmail.com")
                .build();

        Customer customerB = Customer.builder()
                .identityNumber("321")
                .firstName("Kerem")
                .lastName("Temel")
                .email("tka@gmail.com")
                .build();

        customerA = customerRepository.save(customerA);
        customerB = customerRepository.save(customerB);

        Account accountA = Account.builder()
                .accountNumber("akt123")
                .balance(new BigDecimal("10000.0000"))
                .currency(Currency.TRY)
                .status(AccountStatus.ACTIVE)
                .accountType(AccountType.CHECKING)
                .customer(customerA)
                .build();

        Account accountB = Account.builder()
                .accountNumber("tka321")
                .balance(new BigDecimal("10000.0000"))
                .currency(Currency.TRY)
                .status(AccountStatus.ACTIVE)
                .accountType(AccountType.CHECKING)
                .customer(customerB)
                .build();

        accountRepository.save(accountA);
        accountRepository.save(accountB);

        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        BigDecimal transferAmount = new  BigDecimal("10.0000");

        for (int i = 0; i < threadCount; i++) {

            final String senderIban = (i % 2 == 0) ? accountA.getAccountNumber() : accountB.getAccountNumber();
            final String receiverIban = (i % 2 == 0) ? accountB.getAccountNumber() : accountA.getAccountNumber();

            executorService.submit(() -> {
                try {

                    startGun.await();

                    TransferRequest request = new TransferRequest(
                            senderIban,
                            receiverIban,
                            transferAmount,
                            "Concurrent cross transfer"
                    );

                    transferService.transfer(request, UUID.randomUUID().toString());
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGun.countDown();
        boolean completed = finishLine.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "Test has been timeout! Deadlock!");
        executorService.shutdown();

        assertEquals(100, successCount.get());
        assertEquals(0, errorCount.get());

        Account account1 = accountRepository.findByAccountNumber(accountA.getAccountNumber()).get();
        Account account2 = accountRepository.findByAccountNumber(accountB.getAccountNumber()).get();

        assertEquals(0, new BigDecimal("10000.0000").compareTo(account1.getBalance()));
        assertEquals(0, new BigDecimal("10000.0000").compareTo(account2.getBalance()));

        assertEquals(100, transactionRepository.count());
    }

    @Test
    void testDoubleSpendingPreventionConcurrently() throws InterruptedException {

        Customer customerA = Customer.builder()
                .identityNumber("55555")
                .firstName("Kerem")
                .lastName("Temel")
                .email("akt14@gmail.com")
                .build();

        customerA = customerRepository.save(customerA);

        Account accountA = Account.builder()
                .accountNumber("SRC_ACC_100")
                .balance(new BigDecimal("100.0000"))
                .currency(Currency.TRY)
                .status(AccountStatus.ACTIVE)
                .accountType(AccountType.CHECKING)
                .customer(customerA)
                .build();

        Account accountB = Account.builder()
                .accountNumber("TRG_ACC_0")
                .balance(BigDecimal.ZERO)
                .currency(Currency.TRY)
                .status(AccountStatus.ACTIVE)
                .accountType(AccountType.CHECKING)
                .customer(customerA)
                .build();

        accountRepository.save(accountA);
        accountRepository.save(accountB);

        int threadCount = 10;
        BigDecimal transferAmount = new BigDecimal("20.0000");
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {

            final String senderIban = accountA.getAccountNumber();
            final String receiverIban = accountB.getAccountNumber();

            executorService.submit(() -> {
                try {

                    startGun.await();

                    TransferRequest request = new TransferRequest(
                            senderIban,
                            receiverIban,
                            transferAmount,
                            "Double spending test"
                    );

                    transferService.transfer(request, UUID.randomUUID().toString());
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGun.countDown();
        boolean completed = finishLine.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "Test timeout! Deadlock!");
        executorService.shutdown();

        assertEquals(5, successCount.get());
        assertEquals(5, errorCount.get());

        Account account1 = accountRepository.findByAccountNumber(accountA.getAccountNumber()).get();
        Account account2 = accountRepository.findByAccountNumber(accountB.getAccountNumber()).get();

        assertEquals(0, new BigDecimal("0.0000").compareTo(account1.getBalance()));
        assertEquals(0, new BigDecimal("100.0000").compareTo(account2.getBalance()));
    }
}
