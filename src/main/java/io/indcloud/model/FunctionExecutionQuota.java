package io.indcloud.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Function execution quota tracking for rate limiting.
 * Tracks execution counts and enforces limits per organization.
 */
@Entity
@Table(name = "function_execution_quotas")
@Data
public class FunctionExecutionQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    // Quota limits
    @Column(name = "executions_per_minute", nullable = false)
    private Integer executionsPerMinute = 60;

    @Column(name = "executions_per_hour", nullable = false)
    private Integer executionsPerHour = 1000;

    @Column(name = "executions_per_day", nullable = false)
    private Integer executionsPerDay = 10000;

    @Column(name = "executions_per_month", nullable = false)
    private Integer executionsPerMonth = 100000;

    // Current usage
    @Column(name = "current_minute_count", nullable = false)
    private Integer currentMinuteCount = 0;

    @Column(name = "current_hour_count", nullable = false)
    private Integer currentHourCount = 0;

    @Column(name = "current_day_count", nullable = false)
    private Integer currentDayCount = 0;

    @Column(name = "current_month_count", nullable = false)
    private Integer currentMonthCount = 0;

    // Reset timestamps
    @Column(name = "minute_reset_at", nullable = false)
    private Instant minuteResetAt = Instant.now();

    @Column(name = "hour_reset_at", nullable = false)
    private Instant hourResetAt = Instant.now();

    @Column(name = "day_reset_at", nullable = false)
    private Instant dayResetAt = Instant.now();

    @Column(name = "month_reset_at", nullable = false)
    private Instant monthResetAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.minuteResetAt = now;
        this.hourResetAt = now;
        this.dayResetAt = now;
        this.monthResetAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
