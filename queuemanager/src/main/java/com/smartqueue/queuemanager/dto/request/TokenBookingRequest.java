package com.smartqueue.queuemanager.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TokenBookingRequest {

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    // NORMAL by default; patient can declare URGENT
    private String urgency = "NORMAL";
}