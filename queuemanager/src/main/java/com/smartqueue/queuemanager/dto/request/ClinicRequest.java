package com.smartqueue.queuemanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

@Data
public class ClinicRequest {

    @NotBlank(message = "Clinic name is required")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Location is required")
    @Size(max = 255)
    private String location;

    @Size(max = 100)
    private String specialization;

    private LocalTime openingTime;
    private LocalTime closingTime;
}