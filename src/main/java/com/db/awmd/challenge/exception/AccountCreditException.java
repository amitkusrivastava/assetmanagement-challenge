package com.db.awmd.challenge.exception;

public class AccountCreditException extends RuntimeException {
    public AccountCreditException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
