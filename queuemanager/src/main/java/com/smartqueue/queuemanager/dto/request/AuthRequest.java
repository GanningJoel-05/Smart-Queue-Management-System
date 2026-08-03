package com.smartqueue.queuemanager.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthRequest {

    // ── Patient self-registration ──────────────────────────────────────────
    @Data
    public static class PatientRegister {

        @NotBlank(message = "Name is required")
        @Size(max = 100)
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;
    }

    // ── Doctor registration (Admin-only) ──────────────────────────────────
    @Data
    public static class DoctorRegister {

        @NotBlank(message = "Name is required")
        @Size(max = 100)
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6)
        private String password;

        private Long clinicId;           // which clinic to assign the doctor to

        @NotBlank(message = "Specialization is required")
        private String specialization;

        private Integer avgConsultationMins; // default consultation time in minutes
    }

    // ── Login (all roles use same endpoint) ───────────────────────────────
    @Data
    public static class Login {

        @NotBlank(message = "Email is required")
        @Email
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }
}