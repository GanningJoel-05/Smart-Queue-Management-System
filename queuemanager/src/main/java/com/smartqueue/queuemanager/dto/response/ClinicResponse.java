package com.smartqueue.queuemanager.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class ClinicResponse {

    private Long id;
    private String name;
    private String location;
    private String specialization;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private Long adminId;
    private String adminName;
    private Boolean isActive;
}