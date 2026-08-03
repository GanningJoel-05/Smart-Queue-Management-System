package com.smartqueue.queuemanager.service;

import com.smartqueue.queuemanager.entity.Doctor;
import com.smartqueue.queuemanager.entity.Token;
import com.smartqueue.queuemanager.repository.TokenRepository;
import com.smartqueue.queuemanager.websocket.QueueUpdateEvent;
import com.smartqueue.queuemanager.websocket.QueueWebSocketBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueEngineService {

    private final TokenRepository tokenRepository;
    private final QueueWebSocketBroadcaster broadcaster;

    // ─────────────────────────────────────────────────────────────────────
    // EWT CALCULATION
    // EWT = patients_ahead × doctor_avg_consultation_mins
    // ─────────────────────────────────────────────────────────────────────
    public int calculateEwt(Doctor doctor, int queuePosition, LocalDate date) {
        int patientsAhead = tokenRepository.countWaitingAheadOf(
                doctor.getId(), date, queuePosition);
        return patientsAhead * doctor.getAvgConsultationMins();
    }

    // ─────────────────────────────────────────────────────────────────────
    // NORMAL TOKEN ASSIGNMENT
    // Assign next queue position and calculate initial EWT
    // ─────────────────────────────────────────────────────────────────────
    public int assignNextQueuePosition(Doctor doctor, LocalDate date) {
        List<Token> activeQueue = tokenRepository
                .findActiveQueueByDoctorAndDate(doctor.getId(), date);
        // Next position = last position + 1, or 1 if queue is empty
        return activeQueue.isEmpty() ? 1 :
                activeQueue.get(activeQueue.size() - 1).getQueuePosition() + 1;
    }

    // ─────────────────────────────────────────────────────────────────────
    // URGENT INSERTION LOGIC
    // Insert urgent patient right after IN_CONSULTATION (position 2)
    // Shift everyone from position 2 onwards by +1
    // Recalculate EWT for all shifted patients
    // Broadcast WebSocket update to all affected patients
    // ─────────────────────────────────────────────────────────────────────
    @Transactional
    public int insertUrgentToken(Doctor doctor, LocalDate date) {
        // Urgent patient always gets position 2 (right after current consultation)
        int urgentPosition = 2;

        // Shift all WAITING tokens at position >= 2 by +1
        List<Token> tokensToShift = tokenRepository
                .findWaitingTokensFromPosition(doctor.getId(), date, urgentPosition);

        for (Token t : tokensToShift) {
            t.setQueuePosition(t.getQueuePosition() + 1);
            t.setEstimatedWaitMins(calculateEwt(doctor, t.getQueuePosition(), date));
        }
        tokenRepository.saveAll(tokensToShift);

        log.info("Urgent insert: shifted {} tokens for doctor {}", tokensToShift.size(), doctor.getId());

        // Broadcast POSITION_UPDATED to all shifted patients
        if (!tokensToShift.isEmpty()) {
            broadcaster.broadcastQueueUpdate(
                    tokensToShift, doctor.getId(), QueueUpdateEvent.EventType.POSITION_UPDATED);
        }

        return urgentPosition;
    }

    // ─────────────────────────────────────────────────────────────────────
    // ADVANCE QUEUE
    // Called after: Consulted, No Show, Token Cancelled
    // Shifts all WAITING tokens down by 1 position, recalculates EWT
    // ─────────────────────────────────────────────────────────────────────
    @Transactional
    public void advanceQueue(Doctor doctor, LocalDate date,
                             QueueUpdateEvent.EventType eventType) {
        List<Token> waitingTokens = tokenRepository
                .findAllWaitingByDoctorAndDate(doctor.getId(), date);

        for (Token t : waitingTokens) {
            t.setQueuePosition(t.getQueuePosition() - 1);
            t.setEstimatedWaitMins(calculateEwt(doctor, t.getQueuePosition(), date));
        }
        tokenRepository.saveAll(waitingTokens);

        // Send YOUR_TURN_SOON to patient now at position 2
        waitingTokens.stream()
                .filter(t -> t.getQueuePosition() == 2)
                .findFirst()
                .ifPresent(t -> broadcaster.sendTargetedEvent(
                        t, doctor.getId(), QueueUpdateEvent.EventType.YOUR_TURN_SOON));

        // Send CALLED_NOW to patient now at position 1
        waitingTokens.stream()
                .filter(t -> t.getQueuePosition() == 1)
                .findFirst()
                .ifPresent(t -> broadcaster.sendTargetedEvent(
                        t, doctor.getId(), QueueUpdateEvent.EventType.CALLED_NOW));

        // Broadcast the general advance event to all
        broadcaster.broadcastQueueUpdate(waitingTokens, doctor.getId(), eventType);

        log.info("Queue advanced for doctor {} — {} patients updated", doctor.getId(), waitingTokens.size());
    }

    // ─────────────────────────────────────────────────────────────────────
    // DOCTOR AVG CONSULTATION TIME — RUNNING AVERAGE UPDATE
    // new_avg = ((old_avg × total_sessions) + actual_duration) / (total_sessions + 1)
    // ─────────────────────────────────────────────────────────────────────
    public void updateDoctorAvgConsultationTime(Doctor doctor, int actualDurationMins) {
        int oldAvg = doctor.getAvgConsultationMins();
        int sessions = doctor.getTotalSessionsCompleted();

        int newAvg = ((oldAvg * sessions) + actualDurationMins) / (sessions + 1);

        doctor.setAvgConsultationMins(newAvg);
        doctor.setTotalSessionsCompleted(sessions + 1);

        log.debug("Doctor {} avg updated: {} → {} mins (session #{})",
                doctor.getId(), oldAvg, newAvg, sessions + 1);
    }
}