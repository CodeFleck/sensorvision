package org.sensorvision.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.ApiResponse;
import org.sensorvision.model.TrashLog;
import org.sensorvision.service.TrashService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin controller for managing soft-deleted items (trash).
 * Allows viewing and restoring deleted items within the 30-day retention period.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/trash")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTrashController {

    private final TrashService trashService;

    /**
     * Get all items in trash.
     */
    @GetMapping
    public ResponseEntity<List<TrashItemDto>> getAllTrashItems() {
        List<TrashLog> items = trashService.getAllTrashItems();
        List<TrashItemDto> dtos = items.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get trash items filtered by entity type.
     */
    @GetMapping("/type/{entityType}")
    public ResponseEntity<List<TrashItemDto>> getTrashItemsByType(@PathVariable String entityType) {
        List<TrashLog> items = trashService.getTrashItemsByType(entityType);
        List<TrashItemDto> dtos = items.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get trash statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTrashStats() {
        Map<String, Object> stats = trashService.getTrashStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Restore a deleted item.
     */
    @PostMapping("/{trashId}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreItem(@PathVariable Long trashId) {
        trashService.restore(trashId);
        return ResponseEntity.ok(ApiResponse.success(null, "Item restored successfully"));
    }

    /**
     * DTO for trash item response.
     */
    private TrashItemDto toDto(TrashLog item) {
        return new TrashItemDto(
                item.getId(),
                item.getEntityType(),
                item.getEntityId(),
                item.getEntityName(),
                item.getEntitySnapshot(),
                item.getDeletedAt(),
                item.getDeletedBy(),
                item.getDeletionReason(),
                item.getExpiresAt(),
                item.getDaysRemaining(),
                item.getOrganization() != null ? item.getOrganization().getId() : null,
                item.getOrganization() != null ? item.getOrganization().getName() : null
        );
    }

    /**
     * DTO record for trash items.
     */
    public record TrashItemDto(
            Long id,
            String entityType,
            String entityId,
            String entityName,
            Map<String, Object> entitySnapshot,
            Instant deletedAt,
            String deletedBy,
            String deletionReason,
            Instant expiresAt,
            long daysRemaining,
            Long organizationId,
            String organizationName
    ) {}
}
