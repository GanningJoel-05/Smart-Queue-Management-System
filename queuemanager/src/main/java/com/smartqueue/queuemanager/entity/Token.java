package com.smartqueue.queuemanager.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tokens",
        indexes = {
                @Index(name = "idx_token_doctor_date", columnList = "doctor_id, token_date"),
                @Index(name = "idx_token_patient", columnList = "patient_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(name = "token_number", nullable = false)
    private Integer tokenNumber;             // serial number for the day (1, 2, 3...)

    @Column(name = "queue_position", nullable = false)
    private Integer queuePosition;           // dynamic — changes on urgent inserts, no-shows

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false)
    @Builder.Default
    private Urgency urgency = Urgency.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.WAITING;

    @Column(name = "estimated_wait_mins")
    private Integer estimatedWaitMins;       // dynamically recalculated

    @CreationTimestamp
    @Column(name = "booked_at", updatable = false)
    private LocalDateTime bookedAt;

    @Column(name = "consulted_at")
    private LocalDateTime consultedAt;       // set when doctor marks Consulted

    @Column(name = "actual_duration_mins")
    private Integer actualDurationMins;      // recorded after consultation

    @Column(name = "token_date", nullable = false)
    private LocalDate tokenDate;             // for daily grouping and reset

    public enum Urgency {
        NORMAL, URGENT
    }

    public enum Status {
        WAITING, IN_CONSULTATION, CONSULTED, NO_SHOW, CANCELLED
    }
}