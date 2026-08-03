package com.smartqueue.queuemanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Resource not found ─────────────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage());
    }

    // ── Duplicate email on registration ────────────────────────────────────
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex) {
        return build(HttpStatus.CONFLICT, "Duplicate Email", ex.getMessage());
    }

    // ── Duplicate token booking ────────────────────────────────────────────
    @ExceptionHandler(DuplicateBookingException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateBooking(DuplicateBookingException ex) {
        return build(HttpStatus.CONFLICT, "Duplicate Booking", ex.getMessage());
    }

    // ── Token already cancelled ────────────────────────────────────────────
    @ExceptionHandler(TokenAlreadyCancelledException.class)
    public ResponseEntity<ErrorResponse> handleCancelled(TokenAlreadyCancelledException ex) {
        return build(HttpStatus.BAD_REQUEST, "Token Already Cancelled", ex.getMessage());
    }

    // ── Queue closed (doctor is DONE) ──────────────────────────────────────
    @ExceptionHandler(QueueClosedException.class)
    public ResponseEntity<ErrorResponse> handleQueueClosed(QueueClosedException ex) {
        return build(HttpStatus.BAD_REQUEST, "Queue Closed", ex.getMessage());
    }

    // ── Unauthorized (bad/expired JWT) ─────────────────────────────────────
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    // ── Wrong email/password on login ──────────────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Invalid Credentials", "Email or password is incorrect");
    }

    // ── Role doesn't have permission ───────────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Access Denied", "You don't have permission to perform this action");
    }

    // ── @Valid validation failures ─────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        ErrorResponse response = new ErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Validation Failed",
                "One or more fields are invalid",
                LocalDateTime.now(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    // ── Catch-all ─────────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex.getMessage());
    }

    // ── Helper ─────────────────────────────────────────────────────────────
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), error, message, LocalDateTime.now(), null));
    }

    // ── Error response structure ───────────────────────────────────────────
    public record ErrorResponse(
            int status,
            String error,
            String message,
            LocalDateTime timestamp,
            Map<String, String> fieldErrors
    ) {}
}