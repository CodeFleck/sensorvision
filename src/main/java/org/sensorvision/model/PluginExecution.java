package org.sensorvision.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "plugin_executions")
public class PluginExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plugin_id", nullable = false)
    private DataPlugin plugin;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PluginExecutionStatus status;

    @Column(name = "records_processed", nullable = false)
    private Integer recordsProcessed = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Long durationMs;

    // Constructors
    public PluginExecution() {
    }

    public PluginExecution(DataPlugin plugin, PluginExecutionStatus status) {
        this.plugin = plugin;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DataPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(DataPlugin plugin) {
        this.plugin = plugin;
    }

    public Instant getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Instant executedAt) {
        this.executedAt = executedAt;
    }

    public PluginExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(PluginExecutionStatus status) {
        this.status = status;
    }

    public Integer getRecordsProcessed() {
        return recordsProcessed;
    }

    public void setRecordsProcessed(Integer recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
}
