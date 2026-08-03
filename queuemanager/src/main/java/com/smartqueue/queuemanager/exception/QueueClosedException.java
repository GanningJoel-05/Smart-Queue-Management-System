package com.smartqueue.queuemanager.exception;

public class QueueClosedException extends RuntimeException {
    public QueueClosedException(String message) {
        super(message);
    }
}