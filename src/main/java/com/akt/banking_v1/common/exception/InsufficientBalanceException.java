package com.akt.banking_v1.common.exception;

public class InsufficientBalanceException extends BankingException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
