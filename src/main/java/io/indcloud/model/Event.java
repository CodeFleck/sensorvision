package io.indcloud.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventSeverity severity;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum EventType {
        // Device events
        DEVICE_CREATED,
        DEVICE_UPDATED,
        DEVICE_DELETED,
        DEVICE_CONNECTED,
        DEVICE_DISCONNECTED,
        DEVICE_OFFLINE,
        DEVICE_GROUP_CREATED,
        DEVICE_GROUP_UPDATED,
        DEVICE_GROUP_DELETED,

        // Telemetry events
        TELEMETRY_RECEIVED,
        TELEMETRY_ANOMALY,

        // Rule events
        RULE_CREATED,
        RULE_UPDATED,
        RULE_DELETED,
        RULE_TRIGGERED,

        // Alert events
        ALERT_CREATED,
        ALERT_TRIGGERED,
        ALERT_ACKNOWLEDGED,
        ALERT_RESOLVED,

        // Dashboard events
        DASHBOARD_CREATED,
        DASHBOARD_UPDATED,
        DASHBOARD_DELETED,

        // Widget events
        WIDGET_CREATED,
        WIDGET_UPDATED,
        WIDGET_DELETED,

        // User events
        USER_LOGIN,
        USER_LOGOUT,
        USER_REGISTERED,
        USER_UPDATED,
        USER_DELETED,

        // System events
        SYSTEM_ERROR,
        SYSTEM_WARNING,
        SYSTEM_INFO,

        // Synthetic variable events
        SYNTHETIC_VARIABLE_CREATED,
        SYNTHETIC_VARIABLE_UPDATED,
        SYNTHETIC_VARIABLE_DELETED,
        SYNTHETIC_VARIABLE_CALCULATED
    }

    public enum EventSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
}
