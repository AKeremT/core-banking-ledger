package com.akt.banking_v1.dto;

import java.time.Instant;

public record CustomerResponse (

        Long id,
        String identityNumber,
        String firstName,
        String lastName,
        String email,
        Instant createdAt
){
}
