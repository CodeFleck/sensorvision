package io.indcloud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.indcloud.model.AlertSeverity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalAlertResponse {

    private UUID id;
    private UUID globalRuleId;
    private String globalRuleName;
    private Long organizationId;

    private String message;
    private AlertSeverity severity;

    private BigDecimal triggeredValue;
    private Integer deviceCount;
    private List<UUID> affectedDevices;

    private Instant triggeredAt;
    private Boolean acknowledged;
    private Instant acknowledgedAt;
    private Long acknowledgedBy;
    private String acknowledgedByName;

    private Boolean resolved;
    private Instant resolvedAt;
    private Long resolvedBy;
    private String resolvedByName;
    private String resolutionNote;

    private Instant createdAt;
    private Instant updatedAt;
}
