package com.smartqueue.queuemanager.repository;

import com.smartqueue.queuemanager.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // All doctors in a specific clinic
    List<Doctor> findByClinicId(Long clinicId);

    // All active doctors in a clinic (user account must be active)
    List<Doctor> findByClinicIdAndUserIsActiveTrue(Long clinicId);

    // Find doctor profile by their user account ID
    Optional<Doctor> findByUserId(Long userId);

    // Check if a doctor profile already exists for a user
    boolean existsByUserId(Long userId);
}