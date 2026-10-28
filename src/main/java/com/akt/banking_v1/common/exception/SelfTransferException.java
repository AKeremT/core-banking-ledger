package com.akt.banking_v1.common.exception;

public class SelfTransferException extends BankingException {
    public SelfTransferException(String message) {
        super(message);
    }
}
