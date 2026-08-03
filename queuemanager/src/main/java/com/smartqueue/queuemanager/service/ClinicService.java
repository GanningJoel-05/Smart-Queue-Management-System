package com.smartqueue.queuemanager.service;

import com.smartqueue.queuemanager.dto.request.ClinicRequest;
import com.smartqueue.queuemanager.dto.response.ClinicResponse;
import com.smartqueue.queuemanager.dto.response.DoctorResponse;
import com.smartqueue.queuemanager.entity.Clinic;
import com.smartqueue.queuemanager.entity.User;
import com.smartqueue.queuemanager.exception.ResourceNotFoundException;
import com.smartqueue.queuemanager.repository.ClinicRepository;
import com.smartqueue.queuemanager.repository.DoctorRepository;
import com.smartqueue.queuemanager.repository.TokenRepository;
import com.smartqueue.queuemanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClinicService {

    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    // ── Create clinic (Admin only) ─────────────────────────────────────────
    @Transactional
    public ClinicResponse createClinic(ClinicRequest request) {
        User admin = getCurrentAdmin();

        Clinic clinic = Clinic.builder()
                .name(request.getName())
                .location(request.getLocation())
                .specialization(request.getSpecialization())
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .admin(admin)
                .build();

        clinic = clinicRepository.save(clinic);
        return toResponse(clinic);
    }

    // ── List all active clinics (Public) ───────────────────────────────────
    public List<ClinicResponse> getAllClinics(String search) {
        List<Clinic> clinics;

        if (search != null && !search.isBlank()) {
            // Simple search across name, location, specialization
            clinics = clinicRepository.findByIsActiveTrueAndNameContainingIgnoreCase(search);
            clinics.addAll(clinicRepository.findByIsActiveTrueAndLocationContainingIgnoreCase(search));
            clinics.addAll(clinicRepository.findByIsActiveTrueAndSpecializationContainingIgnoreCase(search));
            clinics = clinics.stream().distinct().toList();
        } else {
            clinics = clinicRepository.findAllByIsActiveTrue();
        }

        return clinics.stream().map(this::toResponse).toList();
    }

    // ── Get clinic by ID (Public) ──────────────────────────────────────────
    public ClinicResponse getClinicById(Long id) {
        Clinic clinic = findActiveClinic(id);
        return toResponse(clinic);
    }

    // ── Update clinic (Admin only) ─────────────────────────────────────────
    @Transactional
    public ClinicResponse updateClinic(Long id, ClinicRequest request) {
        Clinic clinic = findActiveClinic(id);

        clinic.setName(request.getName());
        clinic.setLocation(request.getLocation());
        clinic.setSpecialization(request.getSpecialization());
        clinic.setOpeningTime(request.getOpeningTime());
        clinic.setClosingTime(request.getClosingTime());

        return toResponse(clinicRepository.save(clinic));
    }

    // ── Deactivate clinic (Admin only) — soft delete ───────────────────────
    @Transactional
    public void deactivateClinic(Long id) {
        Clinic clinic = findActiveClinic(id);
        clinic.setIsActive(false);
        clinicRepository.save(clinic);
    }

    // ── Get all doctors in a clinic (Public) ──────────────────────────────
    public List<DoctorResponse> getDoctorsInClinic(Long clinicId) {
        findActiveClinic(clinicId); // validate clinic exists
        return doctorRepository.findByClinicIdAndUserIsActiveTrue(clinicId)
                .stream()
                .map(doctor -> {
                    int queueDepth = tokenRepository
                            .countWaitingTokensByDoctorAndToday(doctor.getId());
                    return DoctorResponse.builder()
                            .id(doctor.getId())
                            .userId(doctor.getUser().getId())
                            .name(doctor.getUser().getName())
                            .email(doctor.getUser().getEmail())
                            .clinicId(clinicId)
                            .clinicName(doctor.getClinic().getName())
                            .specialization(doctor.getSpecialization())
                            .avgConsultationMins(doctor.getAvgConsultationMins())
                            .status(doctor.getStatus().name())
                            .currentQueueDepth(queueDepth)
                            .build();
                })
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private Clinic findActiveClinic(Long id) {
        return clinicRepository.findById(id)
                .filter(Clinic::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found: " + id));
    }

    private User getCurrentAdmin() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));
    }

    private ClinicResponse toResponse(Clinic c) {
        return ClinicResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .location(c.getLocation())
                .specialization(c.getSpecialization())
                .openingTime(c.getOpeningTime())
                .closingTime(c.getClosingTime())
                .adminId(c.getAdmin().getId())
                .adminName(c.getAdmin().getName())
                .isActive(c.getIsActive())
                .build();
    }
}