package com.akt.banking_v1.service;

import com.akt.banking_v1.common.exception.AccountNotFoundException;
import com.akt.banking_v1.common.exception.CustomerNotFoundException;
import com.akt.banking_v1.dto.AccountResponse;
import com.akt.banking_v1.dto.CreateAccountRequest;
import com.akt.banking_v1.enums.AccountStatus;
import com.akt.banking_v1.mapper.AccountMapper;
import com.akt.banking_v1.model.Account;
import com.akt.banking_v1.model.Customer;
import com.akt.banking_v1.repository.AccountRepository;
import com.akt.banking_v1.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final AccountMapper accountMapper;

    @Transactional(rollbackFor = Exception.class)
    public AccountResponse createAccount(CreateAccountRequest request) {

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + request.customerId()));

        String accountNumber;
        do {
            accountNumber = generateIban();
        } while (accountRepository.existsByAccountNumber(accountNumber));

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .currency(request.currency())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .accountType(request.accountType())
                .customer(customer)
                .build();

        Account savedAccount = accountRepository.save(account);
        return accountMapper.toResponse(savedAccount);
    }

    public AccountResponse getAccountByAccountNumber(String accountNumber) {

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountNumber));

        return accountMapper.toResponse(account);
    }

    public List<AccountResponse> getAccountsByCustomerId(Long customerId) {

        return accountRepository.findByCustomerId(customerId).stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    private String generateIban() {

        StringBuilder sb = new StringBuilder("TR");

        for (int i = 0; i < 24; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }
}
