package com.akt.banking_v1.dto;

import com.akt.banking_v1.enums.AccountType;
import com.akt.banking_v1.enums.Currency;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(

        @NotNull Long customerId,
        @NotNull Currency currency,
        @NotNull AccountType accountType
) {
}
