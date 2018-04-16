package com.db.awmd.challenge.exception;

public class AccountNotPresentException extends RuntimeException {
    public AccountNotPresentException(String message) {
        super(message);
    }
}
