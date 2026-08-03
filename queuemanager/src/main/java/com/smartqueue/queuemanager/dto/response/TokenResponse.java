package com.smartqueue.queuemanager.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TokenResponse {

    private Long id;
    private Integer tokenNumber;
    private Integer queuePosition;
    private Integer estimatedWaitMins;
    private String urgency;
    private String status;
    private Long doctorId;
    private String doctorName;
    private Long clinicId;
    private String clinicName;
    private Long patientId;
    private String patientName;
    private LocalDate tokenDate;
    private LocalDateTime bookedAt;
    private LocalDateTime consultedAt;
    private Integer actualDurationMins;
}