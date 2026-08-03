package com.smartqueue.queuemanager.service;

import com.smartqueue.queuemanager.dto.request.TokenBookingRequest;
import com.smartqueue.queuemanager.dto.response.TokenResponse;
import com.smartqueue.queuemanager.entity.Doctor;
import com.smartqueue.queuemanager.entity.Token;
import com.smartqueue.queuemanager.entity.User;
import com.smartqueue.queuemanager.exception.*;
import com.smartqueue.queuemanager.repository.DoctorRepository;
import com.smartqueue.queuemanager.repository.TokenRepository;
import com.smartqueue.queuemanager.repository.UserRepository;
import com.smartqueue.queuemanager.websocket.QueueUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final TokenRepository tokenRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final QueueEngineService queueEngine;

    // ── Book a token ───────────────────────────────────────────────────────
    @Transactional
    public TokenResponse bookToken(TokenBookingRequest request) {
        User patient = getCurrentPatient();
        LocalDate today = LocalDate.now();

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Doctor not found: " + request.getDoctorId()));

        // Guard: doctor must be AVAILABLE
        if (doctor.getStatus() == Doctor.Status.DONE) {
            throw new QueueClosedException("Doctor has closed their queue for today.");
        }

        // Guard: no duplicate booking for same doctor today
        tokenRepository.findActiveTokenByPatientAndDoctor(patient.getId(), doctor.getId(), today)
                .ifPresent(t -> { throw new DuplicateBookingException(
                        "You already have an active token for this doctor today. Token #" + t.getTokenNumber()); });

        Token.Urgency urgency;
        try {
            urgency = Token.Urgency.valueOf(request.getUrgency().toUpperCase());
        } catch (IllegalArgumentException e) {
            urgency = Token.Urgency.NORMAL;
        }

        // Determine queue position
        int queuePosition;
        if (urgency == Token.Urgency.URGENT) {
            queuePosition = queueEngine.insertUrgentToken(doctor, today);
        } else {
            queuePosition = queueEngine.assignNextQueuePosition(doctor, today);
        }

        // Calculate EWT
        int ewt = queueEngine.calculateEwt(doctor, queuePosition, today);

        // Generate serial token number for the day
        int tokenNumber = tokenRepository.findMaxTokenNumberForDay(doctor.getId(), today) + 1;

        Token token = Token.builder()
                .patient(patient)
                .doctor(doctor)
                .clinic(doctor.getClinic())
                .tokenNumber(tokenNumber)
                .queuePosition(queuePosition)
                .urgency(urgency)
                .status(Token.Status.WAITING)
                .estimatedWaitMins(ewt)
                .tokenDate(today)
                .build();

        token = tokenRepository.save(token);

        log.info("Token #{} booked by patient {} for doctor {} | position {} | EWT {} mins | {}",
                tokenNumber, patient.getId(), doctor.getId(), queuePosition, ewt, urgency);

        return toResponse(token);
    }

    // ── Get token details (current position + EWT) ─────────────────────────
    public TokenResponse getToken(Long tokenId) {
        Token token = findToken(tokenId);
        return toResponse(token);
    }

    // ── Cancel a token ─────────────────────────────────────────────────────
    @Transactional
    public void cancelToken(Long tokenId) {
        Token token = findToken(tokenId);

        if (token.getStatus() == Token.Status.CANCELLED) {
            throw new TokenAlreadyCancelledException("Token #" + token.getTokenNumber() + " is already cancelled.");
        }
        if (token.getStatus() != Token.Status.WAITING) {
            throw new TokenAlreadyCancelledException(
                    "Cannot cancel a token with status: " + token.getStatus());
        }

        token.setStatus(Token.Status.CANCELLED);
        tokenRepository.save(token);

        // Advance queue — fill the gap left by the cancelled token
        queueEngine.advanceQueue(token.getDoctor(), token.getTokenDate(),
                QueueUpdateEvent.EventType.TOKEN_CANCELLED);

        log.info("Token #{} cancelled. Queue advanced for doctor {}.",
                token.getTokenNumber(), token.getDoctor().getId());
    }

    // ── Patient booking history ────────────────────────────────────────────
    public List<TokenResponse> getHistory() {
        User patient = getCurrentPatient();
        return tokenRepository.findByPatientIdOrderByBookedAtDesc(patient.getId())
                .stream().map(this::toResponse).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private Token findToken(Long id) {
        return tokenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found: " + id));
    }

    private User getCurrentPatient() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
    }

    public TokenResponse toResponse(Token t) {
        return TokenResponse.builder()
                .id(t.getId())
                .tokenNumber(t.getTokenNumber())
                .queuePosition(t.getQueuePosition())
                .estimatedWaitMins(t.getEstimatedWaitMins())
                .urgency(t.getUrgency().name())
                .status(t.getStatus().name())
                .doctorId(t.getDoctor().getId())
                .doctorName(t.getDoctor().getUser().getName())
                .clinicId(t.getClinic().getId())
                .clinicName(t.getClinic().getName())
                .patientId(t.getPatient().getId())
                .patientName(t.getPatient().getName())
                .tokenDate(t.getTokenDate())
                .bookedAt(t.getBookedAt())
                .consultedAt(t.getConsultedAt())
                .actualDurationMins(t.getActualDurationMins())
                .build();
    }
}