package org.sensorvision.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "function_executions")
public class FunctionExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "function_id", nullable = false)
    private ServerlessFunction function;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trigger_id")
    private FunctionTrigger trigger;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FunctionExecutionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_data")
    private JsonNode inputData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output_data")
    private JsonNode outputData;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack", columnDefinition = "TEXT")
    private String errorStack;

    @Column(name = "memory_used_mb")
    private Integer memoryUsedMb;

    // Constructors
    public FunctionExecution() {
    }

    public FunctionExecution(ServerlessFunction function, JsonNode inputData) {
        this.function = function;
        this.inputData = inputData;
        this.status = FunctionExecutionStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServerlessFunction getFunction() {
        return function;
    }

    public void setFunction(ServerlessFunction function) {
        this.function = function;
    }

    public FunctionTrigger getTrigger() {
        return trigger;
    }

    public void setTrigger(FunctionTrigger trigger) {
        this.trigger = trigger;
    }

    public FunctionExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(FunctionExecutionStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }

    public JsonNode getInputData() {
        return inputData;
    }

    public void setInputData(JsonNode inputData) {
        this.inputData = inputData;
    }

    public JsonNode getOutputData() {
        return outputData;
    }

    public void setOutputData(JsonNode outputData) {
        this.outputData = outputData;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorStack() {
        return errorStack;
    }

    public void setErrorStack(String errorStack) {
        this.errorStack = errorStack;
    }

    public Integer getMemoryUsedMb() {
        return memoryUsedMb;
    }

    public void setMemoryUsedMb(Integer memoryUsedMb) {
        this.memoryUsedMb = memoryUsedMb;
    }

    /**
     * Mark execution as completed and calculate duration
     */
    public void complete(FunctionExecutionStatus finalStatus, JsonNode output) {
        this.status = finalStatus;
        this.completedAt = Instant.now();
        this.durationMs = (int) (completedAt.toEpochMilli() - startedAt.toEpochMilli());
        this.outputData = output;
    }

    /**
     * Mark execution as failed with error details
     */
    public void fail(String errorMessage, String errorStack) {
        this.status = FunctionExecutionStatus.ERROR;
        this.completedAt = Instant.now();
        this.durationMs = (int) (completedAt.toEpochMilli() - startedAt.toEpochMilli());
        this.errorMessage = errorMessage;
        this.errorStack = errorStack;
    }
}
