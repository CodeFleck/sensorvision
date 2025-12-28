package io.indcloud.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.service.DataRetentionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to execute data archival based on retention policies
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataArchivalScheduler {

    private final DataRetentionService retentionService;

    /**
     * Execute archival for all enabled policies
     * Runs daily at 2 AM by default
     */
    @Scheduled(cron = "${app.archival.schedule:0 0 2 * * *}")
    public void executeScheduledArchival() {
        log.info("Starting scheduled data archival execution");

        try {
            retentionService.executeAllArchival();
            log.info("Scheduled data archival completed successfully");
        } catch (Exception e) {
            log.error("Scheduled data archival failed: {}", e.getMessage(), e);
        }
    }
}
