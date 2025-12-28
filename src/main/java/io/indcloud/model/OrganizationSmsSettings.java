package io.indcloud.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_sms_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSmsSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    @Column(name = "daily_limit", nullable = false)
    @Builder.Default
    private Integer dailyLimit = 100;

    @Column(name = "monthly_budget", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal monthlyBudget = new BigDecimal("50.00");

    @Column(name = "current_month_count", nullable = false)
    @Builder.Default
    private Integer currentMonthCount = 0;

    @Column(name = "current_month_cost", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal currentMonthCost = BigDecimal.ZERO;

    @Column(name = "current_day_count", nullable = false)
    @Builder.Default
    private Integer currentDayCount = 0;

    @Column(name = "last_reset_date")
    private Instant lastResetDate;

    @Column(name = "alert_on_budget_threshold", nullable = false)
    @Builder.Default
    private Boolean alertOnBudgetThreshold = true;

    @Column(name = "budget_threshold_percentage", nullable = false)
    @Builder.Default
    private Integer budgetThresholdPercentage = 80;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
