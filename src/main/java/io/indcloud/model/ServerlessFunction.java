package io.indcloud.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "serverless_functions")
public class ServerlessFunction extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FunctionRuntime runtime;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(nullable = false, length = 100)
    private String handler = "main";

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "timeout_seconds", nullable = false)
    private Integer timeoutSeconds = 30;

    @Column(name = "memory_limit_mb", nullable = false)
    private Integer memoryLimitMb = 512;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "environment_variables")
    private JsonNode environmentVariables;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "function", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FunctionTrigger> triggers = new ArrayList<>();

    // Constructors
    public ServerlessFunction() {
    }

    public ServerlessFunction(String name, FunctionRuntime runtime, String code) {
        this.name = name;
        this.runtime = runtime;
        this.code = code;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FunctionRuntime getRuntime() {
        return runtime;
    }

    public void setRuntime(FunctionRuntime runtime) {
        this.runtime = runtime;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Integer getMemoryLimitMb() {
        return memoryLimitMb;
    }

    public void setMemoryLimitMb(Integer memoryLimitMb) {
        this.memoryLimitMb = memoryLimitMb;
    }

    public JsonNode getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(JsonNode environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public List<FunctionTrigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<FunctionTrigger> triggers) {
        if (triggers == null) {
            this.triggers.clear();
            return;
        }

        this.triggers.removeIf(existingTrigger ->
            triggers.stream().noneMatch(t -> t.getId() != null && t.getId().equals(existingTrigger.getId()))
        );

        for (FunctionTrigger newTrigger : triggers) {
            if (newTrigger.getId() == null) {
                addTrigger(newTrigger);
            } else {
                boolean exists = this.triggers.stream()
                    .anyMatch(t -> t.getId().equals(newTrigger.getId()));
                if (!exists) {
                    addTrigger(newTrigger);
                }
            }
        }
    }

    public void addTrigger(FunctionTrigger trigger) {
        triggers.add(trigger);
        trigger.setFunction(this);
    }

    public void removeTrigger(FunctionTrigger trigger) {
        triggers.remove(trigger);
        trigger.setFunction(null);
    }
}
