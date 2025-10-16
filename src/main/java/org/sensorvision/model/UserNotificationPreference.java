package org.sensorvision.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_notification_preferences", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "channel"})
})
public class UserNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    // For EMAIL channel: email address
    // For SMS channel: phone number
    // For WEBHOOK channel: webhook URL
    @Column(name = "destination", length = 255)
    private String destination;

    // Minimum severity level to receive notifications
    @Enumerated(EnumType.STRING)
    @Column(name = "min_severity", length = 20)
    @Builder.Default
    private AlertSeverity minSeverity = AlertSeverity.LOW;

    // Send notifications immediately or in digest
    @Column(name = "immediate", nullable = false)
    @Builder.Default
    private Boolean immediate = true;

    // For digest notifications: interval in minutes
    @Column(name = "digest_interval_minutes")
    private Integer digestIntervalMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
