package org.sensorvision.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.NotificationLogResponse;
import org.sensorvision.dto.NotificationPreferenceRequest;
import org.sensorvision.dto.NotificationPreferenceResponse;
import org.sensorvision.model.NotificationChannel;
import org.sensorvision.model.NotificationLog;
import org.sensorvision.model.User;
import org.sensorvision.model.UserNotificationPreference;
import org.sensorvision.repository.NotificationLogRepository;
import org.sensorvision.security.SecurityUtils;
import org.sensorvision.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationLogRepository notificationLogRepository;
    private final SecurityUtils securityUtils;

    /**
     * Get current user's notification preferences
     */
    @GetMapping("/preferences")
    public ResponseEntity<List<NotificationPreferenceResponse>> getPreferences() {
        User user = securityUtils.getCurrentUser();
        log.info("Getting notification preferences for user: {}", user.getUsername());

        List<UserNotificationPreference> preferences = notificationService.getUserPreferences(user);

        List<NotificationPreferenceResponse> response = preferences.stream()
                .map(this::toPreferenceResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Create or update a notification preference
     */
    @PostMapping("/preferences")
    public ResponseEntity<NotificationPreferenceResponse> savePreference(
            @Valid @RequestBody NotificationPreferenceRequest request
    ) {
        User user = securityUtils.getCurrentUser();
        log.info("Saving notification preference for user: {}, channel: {}",
                user.getUsername(), request.channel());

        UserNotificationPreference preference = UserNotificationPreference.builder()
                .user(user)
                .channel(request.channel())
                .enabled(request.enabled() != null ? request.enabled() : true)
                .destination(request.destination())
                .minSeverity(request.minSeverity())
                .immediate(request.immediate() != null ? request.immediate() : true)
                .digestIntervalMinutes(request.digestIntervalMinutes())
                .build();

        UserNotificationPreference saved = notificationService.savePreference(preference);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toPreferenceResponse(saved));
    }

    /**
     * Delete a notification preference for a specific channel
     */
    @DeleteMapping("/preferences/{channel}")
    public ResponseEntity<Void> deletePreference(
            @PathVariable NotificationChannel channel
    ) {
        User user = securityUtils.getCurrentUser();
        log.info("Deleting notification preference for user: {}, channel: {}",
                user.getUsername(), channel);

        notificationService.deletePreference(user, channel);

        return ResponseEntity.noContent().build();
    }

    /**
     * Get notification history for current user
     */
    @GetMapping("/logs")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<NotificationLogResponse>> getNotificationLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = securityUtils.getCurrentUser();
        log.info("Getting notification logs for user: {}, page: {}, size: {}",
                user.getUsername(), page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationLog> logs = notificationLogRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        Page<NotificationLogResponse> response = logs.map(this::toLogResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Get notification statistics for current user
     */
    @GetMapping("/stats")
    public ResponseEntity<NotificationStats> getNotificationStats() {
        User user = securityUtils.getCurrentUser();
        log.info("Getting notification stats for user: {}", user.getUsername());

        long sentCount = notificationLogRepository.countByUserAndStatus(
                user, NotificationLog.NotificationStatus.SENT);
        long failedCount = notificationLogRepository.countByUserAndStatus(
                user, NotificationLog.NotificationStatus.FAILED);
        long totalCount = sentCount + failedCount;

        NotificationStats stats = new NotificationStats(totalCount, sentCount, failedCount);

        return ResponseEntity.ok(stats);
    }

    private NotificationPreferenceResponse toPreferenceResponse(UserNotificationPreference pref) {
        return new NotificationPreferenceResponse(
                pref.getId(),
                pref.getChannel(),
                pref.getEnabled(),
                pref.getDestination(),
                pref.getMinSeverity(),
                pref.getImmediate(),
                pref.getDigestIntervalMinutes(),
                pref.getCreatedAt(),
                pref.getUpdatedAt()
        );
    }

    private NotificationLogResponse toLogResponse(NotificationLog log) {
        return new NotificationLogResponse(
                log.getId(),
                log.getAlert() != null ? log.getAlert().getId() : null,
                log.getChannel(),
                log.getDestination(),
                log.getSubject(),
                log.getMessage(),
                log.getStatus(),
                log.getErrorMessage(),
                log.getSentAt(),
                log.getCreatedAt()
        );
    }

    /**
     * DTO for notification statistics
     */
    public record NotificationStats(
            long total,
            long sent,
            long failed
    ) {
    }
}
