package com.smartqueue.queuemanager.service;

import com.smartqueue.queuemanager.entity.Doctor;
import com.smartqueue.queuemanager.entity.Token;
import com.smartqueue.queuemanager.exception.ResourceNotFoundException;
import com.smartqueue.queuemanager.repository.ClinicRepository;
import com.smartqueue.queuemanager.repository.DoctorRepository;
import com.smartqueue.queuemanager.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClinicStatsService {

    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;
    private final TokenRepository tokenRepository;

    public Map<String, Object> getClinicStats(Long clinicId) {
        clinicRepository.findById(clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic not found: " + clinicId));

        LocalDate today = LocalDate.now();

        List<Doctor> doctors = doctorRepository.findByClinicId(clinicId);

        int totalActive = tokenRepository.countActiveTokensByClinicAndDate(clinicId, today);

        // Per-doctor breakdown
        List<Map<String, Object>> doctorStats = doctors.stream().map(d -> {
            int waiting   = tokenRepository.countByDoctorDateAndStatus(d.getId(), today, Token.Status.WAITING);
            int consulted = tokenRepository.countByDoctorDateAndStatus(d.getId(), today, Token.Status.CONSULTED);
            int noShows   = tokenRepository.countByDoctorDateAndStatus(d.getId(), today, Token.Status.NO_SHOW);

            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("doctorId", d.getId());
            stat.put("doctorName", d.getUser().getName());
            stat.put("status", d.getStatus().name());
            stat.put("currentWaiting", waiting);
            stat.put("totalConsulted", consulted);
            stat.put("noShows", noShows);
            stat.put("avgConsultationMins", d.getAvgConsultationMins());
            return stat;
        }).toList();

        // Find busiest doctor (most WAITING tokens)
        String busiestDoctor = doctors.stream()
                .max(Comparator.comparingInt(d ->
                        tokenRepository.countByDoctorDateAndStatus(d.getId(), today, Token.Status.WAITING)))
                .map(d -> d.getUser().getName())
                .orElse("N/A");

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("clinicId", clinicId);
        stats.put("date", today);
        stats.put("totalActiveTokens", totalActive);
        stats.put("totalDoctors", doctors.size());
        stats.put("busiestDoctor", busiestDoctor);
        stats.put("perDoctor", doctorStats);

        return stats;
    }
}