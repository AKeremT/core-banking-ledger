package com.akt.banking_v1.mapper;

import com.akt.banking_v1.dto.AccountResponse;
import com.akt.banking_v1.model.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getAccountType(),
                account.getCustomer().getId(),
                account.getCreatedAt()
        );
    }
}
