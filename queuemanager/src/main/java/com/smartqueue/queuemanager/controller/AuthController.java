package com.smartqueue.queuemanager.controller;

import com.smartqueue.queuemanager.dto.request.AdminRegisterRequest;
import com.smartqueue.queuemanager.dto.request.AuthRequest;
import com.smartqueue.queuemanager.dto.response.AuthResponse;
import com.smartqueue.queuemanager.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login endpoints for all roles")
public class AuthController {

    private final AuthService authService;

    // ── Admin self-registration (Public — lock down after first admin is created) ──
    @PostMapping("/register/admin")
    @Operation(summary = "Admin self-registration (Public)")
    public ResponseEntity<AuthResponse> registerAdmin(
            @Valid @RequestBody AdminRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerAdmin(request));
    }

    // ── Patient self-registration (Public) ────────────────────────────────
    @PostMapping("/register/patient")
    @Operation(summary = "Patient self-registration (Public)")
    public ResponseEntity<AuthResponse> registerPatient(
            @Valid @RequestBody AuthRequest.PatientRegister request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerPatient(request));
    }

    // ── Doctor registration — Admin only ──────────────────────────────────
    @PostMapping("/register/doctor")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Doctor registration — Admin only")
    public ResponseEntity<AuthResponse> registerDoctor(
            @Valid @RequestBody AuthRequest.DoctorRegister request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerDoctor(request));
    }

    // ── Login — all roles ─────────────────────────────────────────────────
    @PostMapping("/login")
    @Operation(summary = "Login — all roles. Returns JWT token.")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody AuthRequest.Login request) {
        return ResponseEntity.ok(authService.login(request));
    }
}