package com.akt.banking_v1.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(

        @NotBlank String senderAccountNumber, // IBAN
        @NotBlank String receiverAccountNumber,
        @NotNull @Positive @DecimalMin(value = "0.01") BigDecimal amount,
        String description
) {
}
