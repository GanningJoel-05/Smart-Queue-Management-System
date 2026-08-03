package com.smartqueue.queuemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Smart Outpatient Queue Management System
 * <p>
 * Entry point for the Spring Boot application.
 *
 * @EnableScheduling activates the midnight queue-reset job
 * defined in DailyQueueResetScheduler.
 */
@SpringBootApplication
@EnableScheduling
public class QueueManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueueManagerApplication.class, args);
    }
}
