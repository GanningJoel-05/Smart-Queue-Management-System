package com.smartqueue.queuemanager.service;

import com.smartqueue.queuemanager.dto.request.AdminRegisterRequest;
import com.smartqueue.queuemanager.dto.request.AuthRequest;
import com.smartqueue.queuemanager.dto.response.AuthResponse;
import com.smartqueue.queuemanager.entity.Doctor;
import com.smartqueue.queuemanager.entity.User;
import com.smartqueue.queuemanager.exception.DuplicateEmailException;
import com.smartqueue.queuemanager.exception.ResourceNotFoundException;
import com.smartqueue.queuemanager.repository.ClinicRepository;
import com.smartqueue.queuemanager.repository.DoctorRepository;
import com.smartqueue.queuemanager.repository.UserRepository;
import com.smartqueue.queuemanager.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final ClinicRepository clinicRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.expiry.ms}")
    private long expiryMs;

    // ── Admin self-registration ────────────────────────────────────────────
    @Transactional
    public AuthResponse registerAdmin(AdminRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + request.getEmail());
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.ADMIN)
                .build();
        user = userRepository.save(user);
        return buildAuthResponse(user, jwtUtil.generateToken(user));
    }

    // ── Patient self-registration ──────────────────────────────────────────
    @Transactional
    public AuthResponse registerPatient(AuthRequest.PatientRegister request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + request.getEmail());
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.PATIENT)
                .build();
        user = userRepository.save(user);
        return buildAuthResponse(user, jwtUtil.generateToken(user));
    }

    // ── Doctor registration (Admin only) ──────────────────────────────────
    @Transactional
    public AuthResponse registerDoctor(AuthRequest.DoctorRegister request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.DOCTOR)
                .build();
        user = userRepository.save(user);

        var clinic = clinicRepository.findById(request.getClinicId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Clinic not found: " + request.getClinicId()));

        Doctor doctor = Doctor.builder()
                .user(user)
                .clinic(clinic)
                .specialization(request.getSpecialization())
                .avgConsultationMins(
                        request.getAvgConsultationMins() != null
                                ? request.getAvgConsultationMins() : 10)
                .status(Doctor.Status.AVAILABLE)
                .build();

        doctorRepository.save(doctor);
        return buildAuthResponse(user, jwtUtil.generateToken(user));
    }

    // ── Login (all roles) ─────────────────────────────────────────────────
    public AuthResponse login(AuthRequest.Login request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return buildAuthResponse(user, jwtUtil.generateToken(user));
    }

    // ── Helper ────────────────────────────────────────────────────────────
    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .expiresInMs(expiryMs)
                .build();
    }
}