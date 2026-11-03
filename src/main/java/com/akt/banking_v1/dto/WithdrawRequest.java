package com.akt.banking_v1.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record WithdrawRequest(

        @NotBlank String accountNumber,
        @NotNull @Positive @DecimalMin(value = "0.01") BigDecimal amount,
        String description
) {
}
