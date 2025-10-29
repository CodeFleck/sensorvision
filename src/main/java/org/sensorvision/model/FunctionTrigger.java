package org.sensorvision.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "function_triggers")
public class FunctionTrigger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "function_id", nullable = false)
    private ServerlessFunction function;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private FunctionTriggerType triggerType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_config", nullable = false)
    private JsonNode triggerConfig;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // Constructors
    public FunctionTrigger() {
    }

    public FunctionTrigger(FunctionTriggerType triggerType, JsonNode triggerConfig) {
        this.triggerType = triggerType;
        this.triggerConfig = triggerConfig;
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

    public FunctionTriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(FunctionTriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public JsonNode getTriggerConfig() {
        return triggerConfig;
    }

    public void setTriggerConfig(JsonNode triggerConfig) {
        this.triggerConfig = triggerConfig;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
