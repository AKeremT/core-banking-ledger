package com.akt.banking_v1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(

        @NotBlank @Size(min = 10, max = 11) String identityNumber,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email
) {
}
