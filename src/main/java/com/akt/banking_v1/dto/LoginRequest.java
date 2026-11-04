package com.akt.banking_v1.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank String username,
        @NotBlank String password
) {
}
