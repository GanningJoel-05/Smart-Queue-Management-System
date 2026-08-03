package com.smartqueue.queuemanager.exception;

// ── ResourceNotFoundException ─────────────────────────────────────────────
// Thrown when a clinic, doctor, token, or user is not found by ID

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}