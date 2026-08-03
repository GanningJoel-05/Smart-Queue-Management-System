package com.smartqueue.queuemanager.exception;

public class TokenAlreadyCancelledException extends RuntimeException {
    public TokenAlreadyCancelledException(String message) {
        super(message);
    }
}