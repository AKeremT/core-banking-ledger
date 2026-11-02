package com.akt.banking_v1.dto;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(

        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        @Nullable Map<String, String> validationErrors
) {
}
