package com.smartqueue.queuemanager.repository;

import com.smartqueue.queuemanager.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, Long> {

    // All active clinics (public listing)
    List<Clinic> findAllByIsActiveTrue();

    // Search by name, location, or specialization (case-insensitive)
    List<Clinic> findByIsActiveTrueAndNameContainingIgnoreCase(String name);
    List<Clinic> findByIsActiveTrueAndLocationContainingIgnoreCase(String location);
    List<Clinic> findByIsActiveTrueAndSpecializationContainingIgnoreCase(String specialization);

    // Clinics owned by a specific admin
    List<Clinic> findByAdminId(Long adminId);
}