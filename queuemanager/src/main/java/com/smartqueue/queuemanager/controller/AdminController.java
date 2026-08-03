package com.smartqueue.queuemanager.controller;

import com.smartqueue.queuemanager.dto.response.ClinicResponse;
import com.smartqueue.queuemanager.service.ClinicService;
import com.smartqueue.queuemanager.service.ClinicStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-level stats and management")
public class AdminController {

    private final ClinicStatsService clinicStatsService;
    private final ClinicService clinicService;

    @GetMapping("/clinics/{clinicId}/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get today's stats for a clinic — queue depth, busiest doctor, per-doctor breakdown")
    public ResponseEntity<Map<String, Object>> getClinicStats(@PathVariable Long clinicId) {
        return ResponseEntity.ok(clinicStatsService.getClinicStats(clinicId));
    }

    @GetMapping("/clinics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all clinics managed by the logged-in admin")
    public ResponseEntity<List<ClinicResponse>> getMyClinics() {
        return ResponseEntity.ok(clinicService.getAllClinics(null));
    }
}