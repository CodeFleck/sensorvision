package io.indcloud.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.indcloud.model.DashboardTemplate;
import io.indcloud.model.DashboardTemplateCategory;

import java.time.LocalDateTime;

/**
 * Response DTO for Dashboard Templates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTemplateResponse {
    private Long id;
    private String name;
    private String description;
    private DashboardTemplateCategory category;
    private String categoryDisplayName;
    private String icon;
    private String previewImageUrl;
    private JsonNode dashboardConfig;
    private JsonNode widgetsConfig;
    private Boolean isSystem;
    private Integer usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Convert entity to DTO
     */
    public static DashboardTemplateResponse fromEntity(DashboardTemplate template) {
        return DashboardTemplateResponse.builder()
            .id(template.getId())
            .name(template.getName())
            .description(template.getDescription())
            .category(template.getCategory())
            .categoryDisplayName(template.getCategory().getDisplayName())
            .icon(template.getIcon())
            .previewImageUrl(template.getPreviewImageUrl())
            .dashboardConfig(template.getDashboardConfig())
            .widgetsConfig(template.getWidgetsConfig())
            .isSystem(template.getIsSystem())
            .usageCount(template.getUsageCount())
            .createdAt(template.getCreatedAt())
            .updatedAt(template.getUpdatedAt())
            .build();
    }

    /**
     * Convert entity to DTO without full config (for list views)
     */
    public static DashboardTemplateResponse fromEntitySummary(DashboardTemplate template) {
        return DashboardTemplateResponse.builder()
            .id(template.getId())
            .name(template.getName())
            .description(template.getDescription())
            .category(template.getCategory())
            .categoryDisplayName(template.getCategory().getDisplayName())
            .icon(template.getIcon())
            .previewImageUrl(template.getPreviewImageUrl())
            .isSystem(template.getIsSystem())
            .usageCount(template.getUsageCount())
            .createdAt(template.getCreatedAt())
            .build();
    }
}
