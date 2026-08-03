package com.smartqueue.queuemanager.service;

import com.smartqueue.queuemanager.dto.response.DoctorResponse;
import com.smartqueue.queuemanager.dto.response.TokenResponse;
import com.smartqueue.queuemanager.entity.Doctor;
import com.smartqueue.queuemanager.entity.Token;
import com.smartqueue.queuemanager.exception.ResourceNotFoundException;
import com.smartqueue.queuemanager.repository.DoctorRepository;
import com.smartqueue.queuemanager.repository.TokenRepository;
import com.smartqueue.queuemanager.websocket.QueueUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final TokenRepository tokenRepository;
    private final QueueEngineService queueEngine;
    private final TokenService tokenService;

    // ── View today's full queue ────────────────────────────────────────────
    public List<TokenResponse> getTodaysQueue(Long doctorId) {
        findDoctor(doctorId);
        return tokenRepository
                .findActiveQueueByDoctorAndDate(doctorId, LocalDate.now())
                .stream()
                .map(tokenService::toResponse)
                .toList();
    }

    // ── Mark current patient as CONSULTED ─────────────────────────────────
    @Transactional
    public TokenResponse markConsulted(Long doctorId) {
        Doctor doctor = findDoctor(doctorId);
        LocalDate today = LocalDate.now();

        // Find the patient currently IN_CONSULTATION
        Token currentToken = tokenRepository.findInConsultationToken(doctorId, today)
                .orElseGet(() ->
                        // If none in consultation, pull next WAITING token
                        tokenRepository.findNextWaitingToken(doctorId, today)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                        "No active patient in queue for doctor: " + doctorId))
                );

        LocalDateTime now = LocalDateTime.now();
        int actualDuration = currentToken.getStatus() == Token.Status.IN_CONSULTATION
                ? (int) ChronoUnit.MINUTES.between(currentToken.getBookedAt(), now)
                : doctor.getAvgConsultationMins();  // fallback if jumped straight to consulted

        currentToken.setStatus(Token.Status.CONSULTED);
        currentToken.setConsultedAt(now);
        currentToken.setActualDurationMins(actualDuration);
        tokenRepository.save(currentToken);

        // Update doctor's running average consultation time
        queueEngine.updateDoctorAvgConsultationTime(doctor, actualDuration);
        doctorRepository.save(doctor);

        // Advance the queue and broadcast to all waiting patients
        queueEngine.advanceQueue(doctor, today, QueueUpdateEvent.EventType.QUEUE_ADVANCED);

        // Pull next WAITING token and mark as IN_CONSULTATION
        tokenRepository.findNextWaitingToken(doctorId, today)
                .ifPresent(next -> {
                    next.setStatus(Token.Status.IN_CONSULTATION);
                    tokenRepository.save(next);
                    log.info("Patient {} is now IN_CONSULTATION with doctor {}",
                            next.getPatient().getId(), doctorId);
                });

        log.info("Doctor {} marked token #{} as CONSULTED. Duration: {} mins",
                doctorId, currentToken.getTokenNumber(), actualDuration);

        return tokenService.toResponse(currentToken);
    }

    // ── Mark current patient as NO SHOW ───────────────────────────────────
    @Transactional
    public TokenResponse markNoShow(Long doctorId) {
        Doctor doctor = findDoctor(doctorId);
        LocalDate today = LocalDate.now();

        Token currentToken = tokenRepository.findNextWaitingToken(doctorId, today)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No waiting patients in queue for doctor: " + doctorId));

        currentToken.setStatus(Token.Status.NO_SHOW);
        tokenRepository.save(currentToken);

        // Advance queue and notify everyone
        queueEngine.advanceQueue(doctor, today, QueueUpdateEvent.EventType.NO_SHOW);

        // Pull next token into consultation
        tokenRepository.findNextWaitingToken(doctorId, today)
                .ifPresent(next -> {
                    next.setStatus(Token.Status.IN_CONSULTATION);
                    tokenRepository.save(next);
                });

        log.info("Doctor {} marked token #{} as NO_SHOW", doctorId, currentToken.getTokenNumber());
        return tokenService.toResponse(currentToken);
    }

    // ── Update doctor availability status ─────────────────────────────────
    @Transactional
    public DoctorResponse updateStatus(Long doctorId, String status) {
        Doctor doctor = findDoctor(doctorId);
        doctor.setStatus(Doctor.Status.valueOf(status.toUpperCase()));
        doctorRepository.save(doctor);
        log.info("Doctor {} status updated to {}", doctorId, status);
        return toResponse(doctor);
    }

    // ── Doctor stats ──────────────────────────────────────────────────────
    public Map<String, Object> getDoctorStats(Long doctorId) {
        Doctor doctor = findDoctor(doctorId);
        LocalDate today = LocalDate.now();

        int consulted = tokenRepository.countByDoctorDateAndStatus(doctorId, today, Token.Status.CONSULTED);
        int noShows   = tokenRepository.countByDoctorDateAndStatus(doctorId, today, Token.Status.NO_SHOW);
        int waiting   = tokenRepository.countByDoctorDateAndStatus(doctorId, today, Token.Status.WAITING);

        return Map.of(
                "doctorId", doctorId,
                "doctorName", doctor.getUser().getName(),
                "date", today,
                "patientsConsulted", consulted,
                "noShowCount", noShows,
                "currentWaiting", waiting,
                "avgConsultationMins", doctor.getAvgConsultationMins(),
                "totalSessionsCompleted", doctor.getTotalSessionsCompleted(),
                "status", doctor.getStatus().name()
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private Doctor findDoctor(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
    }

    private DoctorResponse toResponse(Doctor d) {
        return DoctorResponse.builder()
                .id(d.getId())
                .userId(d.getUser().getId())
                .name(d.getUser().getName())
                .email(d.getUser().getEmail())
                .clinicId(d.getClinic().getId())
                .clinicName(d.getClinic().getName())
                .specialization(d.getSpecialization())
                .avgConsultationMins(d.getAvgConsultationMins())
                .status(d.getStatus().name())
                .build();
    }
}