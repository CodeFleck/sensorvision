package io.indcloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.exception.BadRequestException;
import io.indcloud.exception.ResourceNotFoundException;
import io.indcloud.model.*;
import io.indcloud.repository.*;
import io.indcloud.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling soft delete and restore operations.
 * Items are soft-deleted and can be restored within 30 days.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TrashService {

    private final TrashLogRepository trashLogRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final OrganizationRepository organizationRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    /**
     * Soft delete a user.
     */
    public TrashLog softDeleteUser(Long userId, String reason) {
        User currentAdmin = securityUtils.getCurrentUser();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (user.isDeleted()) {
            throw new BadRequestException("User is already deleted");
        }

        // Create snapshot
        Map<String, Object> snapshot = createUserSnapshot(user);

        // Mark as deleted
        user.setDeletedAt(Instant.now());
        user.setDeletedBy(currentAdmin.getUsername());
        user.setDeletionReason(reason);
        user.setEnabled(false); // Disable the user
        userRepository.save(user);

        // Create trash log entry
        TrashLog trashLog = TrashLog.createFor(user, currentAdmin.getUsername(), reason, snapshot, user.getOrganization());
        trashLog = trashLogRepository.save(trashLog);

        log.info("SOFT_DELETE: Admin {} soft-deleted user {} (id: {}). Expires at: {}",
                currentAdmin.getUsername(), user.getUsername(), userId, trashLog.getExpiresAt());

        return trashLog;
    }

    /**
     * Soft delete a device.
     */
    public TrashLog softDeleteDevice(UUID deviceId, String reason) {
        User currentAdmin = securityUtils.getCurrentUser();
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));

        if (device.isDeleted()) {
            throw new BadRequestException("Device is already deleted");
        }

        // Create snapshot
        Map<String, Object> snapshot = createDeviceSnapshot(device);

        // Mark as deleted
        device.setDeletedAt(Instant.now());
        device.setDeletedBy(currentAdmin.getUsername());
        device.setDeletionReason(reason);
        device.setActive(false); // Disable the device
        deviceRepository.save(device);

        // Create trash log entry
        TrashLog trashLog = TrashLog.createFor(device, currentAdmin.getUsername(), reason, snapshot, device.getOrganization());
        trashLog = trashLogRepository.save(trashLog);

        log.info("SOFT_DELETE: Admin {} soft-deleted device {} (id: {}). Expires at: {}",
                currentAdmin.getUsername(), device.getExternalId(), deviceId, trashLog.getExpiresAt());

        return trashLog;
    }

    /**
     * Soft delete an organization.
     */
    public TrashLog softDeleteOrganization(Long organizationId, String reason) {
        User currentAdmin = securityUtils.getCurrentUser();
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + organizationId));

        if (org.isDeleted()) {
            throw new BadRequestException("Organization is already deleted");
        }

        // Check for active users/devices
        long userCount = userRepository.countByOrganizationIdAndDeletedAtIsNull(organizationId);
        if (userCount > 0) {
            throw new BadRequestException("Cannot delete organization with " + userCount + " active users. Delete or reassign users first.");
        }

        // Create snapshot
        Map<String, Object> snapshot = createOrganizationSnapshot(org);

        // Mark as deleted
        org.setDeletedAt(Instant.now());
        org.setDeletedBy(currentAdmin.getUsername());
        org.setDeletionReason(reason);
        org.setEnabled(false);
        organizationRepository.save(org);

        // Create trash log entry
        TrashLog trashLog = TrashLog.createFor(org, currentAdmin.getUsername(), reason, snapshot, null);
        trashLog = trashLogRepository.save(trashLog);

        log.info("SOFT_DELETE: Admin {} soft-deleted organization {} (id: {}). Expires at: {}",
                currentAdmin.getUsername(), org.getName(), organizationId, trashLog.getExpiresAt());

        return trashLog;
    }

    /**
     * Restore a soft-deleted item.
     */
    public void restore(Long trashLogId) {
        User currentAdmin = securityUtils.getCurrentUser();
        TrashLog trashLog = trashLogRepository.findRestorableById(trashLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Trash item not found or already restored: " + trashLogId));

        switch (trashLog.getEntityType()) {
            case "USER" -> restoreUser(trashLog, currentAdmin);
            case "DEVICE" -> restoreDevice(trashLog, currentAdmin);
            case "ORGANIZATION" -> restoreOrganization(trashLog, currentAdmin);
            default -> throw new BadRequestException("Unknown entity type: " + trashLog.getEntityType());
        }

        // Mark as restored
        trashLog.setRestoredAt(Instant.now());
        trashLog.setRestoredBy(currentAdmin.getUsername());
        trashLogRepository.save(trashLog);

        log.info("RESTORE: Admin {} restored {} {} (id: {})",
                currentAdmin.getUsername(), trashLog.getEntityType(), trashLog.getEntityName(), trashLog.getEntityId());
    }

    private void restoreUser(TrashLog trashLog, User currentAdmin) {
        Long userId = Long.parseLong(trashLog.getEntityId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User no longer exists: " + userId));

        user.setDeletedAt(null);
        user.setDeletedBy(null);
        user.setDeletionReason(null);
        user.setEnabled(true);
        userRepository.save(user);
    }

    private void restoreDevice(TrashLog trashLog, User currentAdmin) {
        UUID deviceId = UUID.fromString(trashLog.getEntityId());
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device no longer exists: " + deviceId));

        device.setDeletedAt(null);
        device.setDeletedBy(null);
        device.setDeletionReason(null);
        device.setActive(true);
        deviceRepository.save(device);
    }

    private void restoreOrganization(TrashLog trashLog, User currentAdmin) {
        Long orgId = Long.parseLong(trashLog.getEntityId());
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization no longer exists: " + orgId));

        org.setDeletedAt(null);
        org.setDeletedBy(null);
        org.setDeletionReason(null);
        org.setEnabled(true);
        organizationRepository.save(org);
    }

    /**
     * Permanently delete expired items (called by scheduled job).
     */
    public int permanentlyDeleteExpiredItems() {
        List<TrashLog> expiredItems = trashLogRepository.findExpiredItems(Instant.now());
        int deletedCount = 0;

        for (TrashLog item : expiredItems) {
            try {
                switch (item.getEntityType()) {
                    case "USER" -> permanentlyDeleteUser(item);
                    case "DEVICE" -> permanentlyDeleteDevice(item);
                    case "ORGANIZATION" -> permanentlyDeleteOrganization(item);
                }

                item.setPermanentlyDeletedAt(Instant.now());
                trashLogRepository.save(item);
                deletedCount++;

                log.info("PERMANENT_DELETE: {} {} (id: {}) permanently deleted after expiry",
                        item.getEntityType(), item.getEntityName(), item.getEntityId());
            } catch (Exception e) {
                log.error("Failed to permanently delete {} {}: {}",
                        item.getEntityType(), item.getEntityId(), e.getMessage());
            }
        }

        return deletedCount;
    }

    private void permanentlyDeleteUser(TrashLog trashLog) {
        Long userId = Long.parseLong(trashLog.getEntityId());
        userRepository.findById(userId).ifPresent(userRepository::delete);
    }

    private void permanentlyDeleteDevice(TrashLog trashLog) {
        UUID deviceId = UUID.fromString(trashLog.getEntityId());
        deviceRepository.findById(deviceId).ifPresent(deviceRepository::delete);
    }

    private void permanentlyDeleteOrganization(TrashLog trashLog) {
        Long orgId = Long.parseLong(trashLog.getEntityId());
        organizationRepository.findById(orgId).ifPresent(organizationRepository::delete);
    }

    /**
     * Get all active trash items.
     */
    @Transactional(readOnly = true)
    public List<TrashLog> getAllTrashItems() {
        return trashLogRepository.findAllActive();
    }

    /**
     * Get trash items by entity type.
     */
    @Transactional(readOnly = true)
    public List<TrashLog> getTrashItemsByType(String entityType) {
        return trashLogRepository.findActiveByEntityType(entityType.toUpperCase());
    }

    /**
     * Get trash statistics.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTrashStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalItems", trashLogRepository.countAllActive());
        stats.put("users", trashLogRepository.countActiveByEntityType("USER"));
        stats.put("devices", trashLogRepository.countActiveByEntityType("DEVICE"));
        stats.put("organizations", trashLogRepository.countActiveByEntityType("ORGANIZATION"));
        return stats;
    }

    // Snapshot creation methods
    private Map<String, Object> createUserSnapshot(User user) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", user.getId());
        snapshot.put("username", user.getUsername());
        snapshot.put("email", user.getEmail());
        snapshot.put("firstName", user.getFirstName());
        snapshot.put("lastName", user.getLastName());
        snapshot.put("organizationId", user.getOrganization() != null ? user.getOrganization().getId() : null);
        snapshot.put("organizationName", user.getOrganization() != null ? user.getOrganization().getName() : null);
        snapshot.put("roles", user.getRoles().stream().map(Role::getName).toList());
        snapshot.put("enabled", user.getEnabled());
        snapshot.put("emailVerified", user.getEmailVerified());
        return snapshot;
    }

    private Map<String, Object> createDeviceSnapshot(Device device) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", device.getId().toString());
        snapshot.put("externalId", device.getExternalId());
        snapshot.put("name", device.getName());
        snapshot.put("description", device.getDescription());
        snapshot.put("location", device.getLocation());
        snapshot.put("sensorType", device.getSensorType());
        snapshot.put("firmwareVersion", device.getFirmwareVersion());
        snapshot.put("status", device.getStatus() != null ? device.getStatus().name() : null);
        snapshot.put("organizationId", device.getOrganization() != null ? device.getOrganization().getId() : null);
        snapshot.put("organizationName", device.getOrganization() != null ? device.getOrganization().getName() : null);
        snapshot.put("active", device.getActive());
        snapshot.put("healthScore", device.getHealthScore());
        return snapshot;
    }

    private Map<String, Object> createOrganizationSnapshot(Organization org) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", org.getId());
        snapshot.put("name", org.getName());
        snapshot.put("description", org.getDescription());
        snapshot.put("enabled", org.isEnabled());
        return snapshot;
    }
}
