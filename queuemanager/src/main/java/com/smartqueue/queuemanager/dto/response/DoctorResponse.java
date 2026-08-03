package com.smartqueue.queuemanager.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DoctorResponse {

    private Long id;
    private Long userId;
    private String name;
    private String email;
    private Long clinicId;
    private String clinicName;
    private String specialization;
    private Integer avgConsultationMins;
    private String status;
    private Integer currentQueueDepth;   // populated on demand
}