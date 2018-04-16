package com.db.awmd.challenge.exception;

public class AccountDebitException extends RuntimeException {

    public AccountDebitException(String message) {
        super(message);
    }

    public AccountDebitException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
