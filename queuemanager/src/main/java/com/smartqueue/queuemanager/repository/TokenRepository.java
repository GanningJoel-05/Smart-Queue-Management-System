package com.smartqueue.queuemanager.repository;

import com.smartqueue.queuemanager.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    // ── Today's queue for a doctor ordered by position ─────────────────────
    @Query("SELECT t FROM Token t WHERE t.doctor.id = :doctorId AND t.tokenDate = :date " +
            "AND t.status IN ('WAITING', 'IN_CONSULTATION') ORDER BY t.queuePosition ASC")
    List<Token> findActiveQueueByDoctorAndDate(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date);

    // ── First WAITING token (next in line) ────────────────────────────────
    @Query("SELECT t FROM Token t WHERE t.doctor.id = :doctorId AND t.tokenDate = :date " +
            "AND t.status = 'WAITING' ORDER BY t.queuePosition ASC LIMIT 1")
    Optional<Token> findNextWaitingToken(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date);

    // ── Currently IN_CONSULTATION token ───────────────────────────────────
    @Query("SELECT t FROM Token t WHERE t.doctor.id = :doctorId AND t.tokenDate = :date " +
            "AND t.status = 'IN_CONSULTATION'")
    Optional<Token> findInConsultationToken(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date);

    // ── Count of WAITING tokens ahead of a given position ─────────────────
    @Query("SELECT COUNT(t) FROM Token t WHERE t.doctor.id = :doctorId AND t.tokenDate = :date " +
            "AND t.status = 'WAITING' AND t.queuePosition < :position")
    int countWaitingAheadOf(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("position") int position);

    // ── Count of WAITING tokens for a doctor today (for queue depth display) ─
    @Query("SELECT COUNT(t) FROM Token t WHERE t.doctor.id = :doctorId AND t.tokenDate = CURRENT_DATE " +
            "AND t.status = 'WAITING'")
    int countWaitingTokensByDoctorAndToday(@Param("doctorId") Long doctorId);

    // ── Max token number for the day (to generate next serial number) ──────
    @Query("SELECT COALESCE(MAX(t.tokenNumber), 0) FROM Token t WHERE t.doctor.id = :doctorId " +
            "AND t.tokenDate = :date")
    int findMaxTokenNumberForDay(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date);

    // ── All WAITING tokens at or after a position (for position shifting) ──
    @Query("SELECT t FROM Token t WHERE t.doctor.id = :doctorId AND t.tokenDate = :date " +
            "AND t.status = 'WAITING' AND t.queuePosition >= :fromPosition ORDER BY t.queuePosition ASC")
    List<Token> findWaitingTokensFromPosition(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("fromPosition") int fromPosition);

    // ── All WAITING tokens for a doctor today (for WebSocket broadcast) ───
    @Query("SELECT t FROM Token t WHERE t.doctor.id = :doctorId AND t.tokenDate = :date " +
            "AND t.status = 'WAITING' ORDER BY t.queuePosition ASC")
    List<Token> findAllWaitingByDoctorAndDate(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date);

    // ── Patient's active token for a doctor today (duplicate booking check) ─
    @Query("SELECT t FROM Token t WHERE t.patient.id = :patientId AND t.doctor.id = :doctorId " +
            "AND t.tokenDate = :date AND t.status IN ('WAITING', 'IN_CONSULTATION')")
    Optional<Token> findActiveTokenByPatientAndDoctor(
            @Param("patientId") Long patientId,
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date);

    // ── Patient booking history ────────────────────────────────────────────
    List<Token> findByPatientIdOrderByBookedAtDesc(Long patientId);

    // ── Daily reset — cancel all WAITING tokens from yesterday ─────────────
    @Modifying
    @Query("UPDATE Token t SET t.status = 'CANCELLED' WHERE t.status = 'WAITING' AND t.tokenDate < :today")
    int cancelStaleWaitingTokens(@Param("today") LocalDate today);

    // ── Stats: count of tokens by doctor, date, and status ────────────────
    @Query("SELECT COUNT(t) FROM Token t WHERE t.doctor.id = :doctorId AND t.tokenDate = :date AND t.status = :status")
    int countByDoctorDateAndStatus(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("status") Token.Status status);

    // ── Stats: all tokens today across all clinics (admin dashboard) ───────
    @Query("SELECT COUNT(t) FROM Token t WHERE t.clinic.id = :clinicId AND t.tokenDate = :date " +
            "AND t.status IN ('WAITING', 'IN_CONSULTATION')")
    int countActiveTokensByClinicAndDate(
            @Param("clinicId") Long clinicId,
            @Param("date") LocalDate date);
}