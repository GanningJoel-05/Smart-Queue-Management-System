package com.smartqueue.queuemanager.controller;

import com.smartqueue.queuemanager.dto.response.DoctorResponse;
import com.smartqueue.queuemanager.dto.response.TokenResponse;
import com.smartqueue.queuemanager.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctors", description = "Doctor queue management and status")
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping("/{id}/queue")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "View today's full queue for this doctor")
    public ResponseEntity<List<TokenResponse>> getTodaysQueue(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getTodaysQueue(id));
    }

    @PutMapping("/{id}/next")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Mark current patient as Consulted — advances queue and broadcasts WebSocket")
    public ResponseEntity<TokenResponse> markConsulted(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.markConsulted(id));
    }

    @PutMapping("/{id}/noshow")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Mark current patient as No Show — skips and advances queue")
    public ResponseEntity<TokenResponse> markNoShow(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.markNoShow(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Update availability status — AVAILABLE / ON_BREAK / DONE")
    public ResponseEntity<DoctorResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(doctorService.updateStatus(id, status));
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "View doctor stats — patients seen, avg time, no-shows")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getDoctorStats(id));
    }
}