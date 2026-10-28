package com.akt.banking_v1.common.exception;

public class CurrencyMismatchException extends BankingException {
    public CurrencyMismatchException(String message) {
        super(message);
    }
}
