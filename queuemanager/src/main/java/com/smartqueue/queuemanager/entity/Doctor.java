package com.smartqueue.queuemanager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One-to-One with User (the doctor's login account)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Many doctors belong to one clinic
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(name = "specialization", length = 100)
    private String specialization;

    // Default = 10 mins; auto-updated after each consultation session
    @Column(name = "avg_consultation_mins", nullable = false)
    @Builder.Default
    private Integer avgConsultationMins = 10;

    // Running count of completed sessions — used for avg recalculation
    @Column(name = "total_sessions_completed", nullable = false)
    @Builder.Default
    private Integer totalSessionsCompleted = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.AVAILABLE;

    public enum Status {
        AVAILABLE, ON_BREAK, DONE
    }
}