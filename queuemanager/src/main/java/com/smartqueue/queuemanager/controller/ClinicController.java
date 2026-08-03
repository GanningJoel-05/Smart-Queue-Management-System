package com.smartqueue.queuemanager.controller;

import com.smartqueue.queuemanager.dto.request.ClinicRequest;
import com.smartqueue.queuemanager.dto.response.ClinicResponse;
import com.smartqueue.queuemanager.dto.response.DoctorResponse;
import com.smartqueue.queuemanager.service.ClinicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clinics")
@RequiredArgsConstructor
@Tag(name = "Clinics", description = "Clinic management — Admin CRUD + public listing")
public class ClinicController {

    private final ClinicService clinicService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new clinic (Admin only)")
    public ResponseEntity<ClinicResponse> createClinic(@Valid @RequestBody ClinicRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicService.createClinic(request));
    }

    @GetMapping
    @Operation(summary = "List all active clinics — supports ?search= for name/location/specialization")
    public ResponseEntity<List<ClinicResponse>> getAllClinics(
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(clinicService.getAllClinics(search));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get clinic details by ID")
    public ResponseEntity<ClinicResponse> getClinic(@PathVariable Long id) {
        return ResponseEntity.ok(clinicService.getClinicById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update clinic info (Admin only)")
    public ResponseEntity<ClinicResponse> updateClinic(
            @PathVariable Long id, @Valid @RequestBody ClinicRequest request) {
        return ResponseEntity.ok(clinicService.updateClinic(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate (soft-delete) a clinic (Admin only)")
    public ResponseEntity<Void> deactivateClinic(@PathVariable Long id) {
        clinicService.deactivateClinic(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/doctors")
    @Operation(summary = "Get all active doctors in a clinic with live queue depth")
    public ResponseEntity<List<DoctorResponse>> getDoctors(@PathVariable Long id) {
        return ResponseEntity.ok(clinicService.getDoctorsInClinic(id));
    }
}