package com.akt.banking_v1.common.exception;

public class CustomerAlreadyExistsException extends BankingException {
    public CustomerAlreadyExistsException(String message) {
        super(message);
    }
}
