package io.indcloud.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.service.TrashService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to permanently delete expired soft-deleted items.
 * Runs daily at 2 AM to clean up items that have been in trash for more than 30 days.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrashCleanupScheduler {

    private final TrashService trashService;

    /**
     * Permanently delete items that have expired.
     * Runs daily at 2:00 AM server time.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Every day at 2 AM
    public void cleanupExpiredTrashItems() {
        log.info("Starting scheduled trash cleanup job...");
        try {
            int deletedCount = trashService.permanentlyDeleteExpiredItems();
            log.info("Trash cleanup completed. Permanently deleted {} expired items.", deletedCount);
        } catch (Exception e) {
            log.error("Error during trash cleanup job: {}", e.getMessage(), e);
        }
    }
}
