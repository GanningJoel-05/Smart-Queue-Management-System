package com.smartqueue.queuemanager.scheduler;

import com.smartqueue.queuemanager.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyQueueResetScheduler {

    private final TokenRepository tokenRepository;

    // Runs every day at midnight (00:00:00)
    // Cancels any WAITING tokens left over from previous days
    // Doctor avg_consultation_mins is intentionally NOT reset — carries forward for accuracy
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void resetDailyQueues() {
        LocalDate today = LocalDate.now();
        int cancelled = tokenRepository.cancelStaleWaitingTokens(today);
        log.info("Daily queue reset at midnight — {} stale WAITING tokens cancelled for dates before {}",
                cancelled, today);
    }
}