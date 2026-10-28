package com.akt.banking_v1.common.exception;

public class AccountInactiveException extends BankingException {
    public AccountInactiveException(String message) {
        super(message);
    }
}
