package com.smartqueue.queuemanager.controller;

import com.smartqueue.queuemanager.dto.request.TokenBookingRequest;
import com.smartqueue.queuemanager.dto.response.TokenResponse;
import com.smartqueue.queuemanager.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
@Tag(name = "Tokens", description = "Token booking and queue management for patients")
public class TokenController {

    private final TokenService tokenService;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Book a token — returns token number, position, and EWT")
    public ResponseEntity<TokenResponse> bookToken(@Valid @RequestBody TokenBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tokenService.bookToken(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    @Operation(summary = "Get token details — current position and EWT")
    public ResponseEntity<TokenResponse> getToken(@PathVariable Long id) {
        return ResponseEntity.ok(tokenService.getToken(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Cancel a WAITING token")
    public ResponseEntity<Void> cancelToken(@PathVariable Long id) {
        tokenService.cancelToken(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Patient's booking history — all past tokens")
    public ResponseEntity<List<TokenResponse>> getHistory() {
        return ResponseEntity.ok(tokenService.getHistory());
    }
}