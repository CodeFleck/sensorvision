package io.indcloud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.exception.ResourceNotFoundException;
import io.indcloud.model.*;
import io.indcloud.repository.DeviceTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeviceTypeService {

    private final DeviceTypeRepository deviceTypeRepository;
    private final EventService eventService;

    /**
     * Get all device types for an organization
     */
    @Transactional(readOnly = true)
    public List<DeviceType> getAllDeviceTypes(Organization organization) {
        return deviceTypeRepository.findByOrganization(organization);
    }

    /**
     * Get active device types only
     */
    @Transactional(readOnly = true)
    public List<DeviceType> getActiveDeviceTypes(Organization organization) {
        return deviceTypeRepository.findByOrganizationAndIsActiveTrue(organization);
    }

    /**
     * Get device type by ID
     */
    @Transactional(readOnly = true)
    public DeviceType getDeviceType(Long id) {
        return deviceTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device type not found: " + id));
    }

    /**
     * Create a new device type
     */
    public DeviceType createDeviceType(Organization organization, String name, String description, String icon, List<DeviceTypeVariable> variables) {
        // Check if name already exists
        if (deviceTypeRepository.existsByOrganizationAndName(organization, name)) {
            throw new IllegalArgumentException("Device type with name '" + name + "' already exists");
        }

        DeviceType deviceType = DeviceType.builder()
                .organization(organization)
                .name(name)
                .description(description)
                .icon(icon)
                .isActive(true)
                .build();

        // Add variables
        if (variables != null) {
            variables.forEach(deviceType::addVariable);
        }

        DeviceType saved = deviceTypeRepository.save(deviceType);

        // Emit event
        eventService.createEvent(
                organization,
                Event.EventType.DEVICE_CREATED,
                Event.EventSeverity.INFO,
                "Device Type Created",
                String.format("Device type '%s' created with %d variables", name, variables != null ? variables.size() : 0)
        );

        log.info("Device type created: {} for organization: {}", name, organization.getName());

        return saved;
    }

    /**
     * Update device type
     */
    public DeviceType updateDeviceType(Long id, String name, String description, String icon, Boolean isActive) {
        DeviceType deviceType = getDeviceType(id);

        if (name != null && !name.equals(deviceType.getName())) {
            // Check if new name is available
            if (deviceTypeRepository.existsByOrganizationAndName(deviceType.getOrganization(), name)) {
                throw new IllegalArgumentException("Device type with name '" + name + "' already exists");
            }
            deviceType.setName(name);
        }

        if (description != null) {
            deviceType.setDescription(description);
        }

        if (icon != null) {
            deviceType.setIcon(icon);
        }

        if (isActive != null) {
            deviceType.setIsActive(isActive);
        }

        DeviceType updated = deviceTypeRepository.save(deviceType);

        // Emit event
        eventService.createEvent(
                deviceType.getOrganization(),
                Event.EventType.DEVICE_UPDATED,
                Event.EventSeverity.INFO,
                "Device Type Updated",
                String.format("Device type '%s' updated", updated.getName())
        );

        return updated;
    }

    /**
     * Delete device type
     */
    public void deleteDeviceType(Long id) {
        DeviceType deviceType = getDeviceType(id);

        // Emit event before deleting
        eventService.createEvent(
                deviceType.getOrganization(),
                Event.EventType.DEVICE_DELETED,
                Event.EventSeverity.WARNING,
                "Device Type Deleted",
                String.format("Device type '%s' deleted", deviceType.getName())
        );

        deviceTypeRepository.delete(deviceType);

        log.info("Device type deleted: {}", deviceType.getName());
    }

    /**
     * Add variable to device type
     */
    public DeviceType addVariable(Long deviceTypeId, DeviceTypeVariable variable) {
        DeviceType deviceType = getDeviceType(deviceTypeId);
        deviceType.addVariable(variable);
        return deviceTypeRepository.save(deviceType);
    }

    /**
     * Remove variable from device type
     */
    public DeviceType removeVariable(Long deviceTypeId, Long variableId) {
        DeviceType deviceType = getDeviceType(deviceTypeId);
        DeviceTypeVariable variable = deviceType.getVariables().stream()
                .filter(v -> v.getId().equals(variableId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Variable not found: " + variableId));

        deviceType.removeVariable(variable);
        return deviceTypeRepository.save(deviceType);
    }

    /**
     * Validate telemetry data against device type schema
     */
    public ValidationResult validateTelemetryData(Device device, Map<String, Object> telemetryData) {
        if (device.getDeviceType() == null) {
            // No device type, no validation
            return ValidationResult.valid();
        }

        DeviceType deviceType = device.getDeviceType();
        ValidationResult result = new ValidationResult();

        for (DeviceTypeVariable variable : deviceType.getVariables()) {
            Object value = telemetryData.get(variable.getName());

            // Check required fields
            if (variable.getRequired() && (value == null || value.toString().isEmpty())) {
                result.addError(variable.getName(), "Required field is missing");
                continue;
            }

            // Skip validation if value is not present and field is not required
            if (value == null) {
                continue;
            }

            // Validate data type
            if (!validateDataType(variable.getDataType(), value)) {
                result.addError(variable.getName(), "Invalid data type. Expected: " + variable.getDataType());
                continue;
            }

            // Validate range for numeric values
            if (variable.getDataType() == DeviceTypeVariable.VariableDataType.NUMBER) {
                BigDecimal numValue = new BigDecimal(value.toString());

                if (variable.getMinValue() != null && numValue.compareTo(variable.getMinValue()) < 0) {
                    result.addError(variable.getName(), "Value below minimum: " + variable.getMinValue());
                }

                if (variable.getMaxValue() != null && numValue.compareTo(variable.getMaxValue()) > 0) {
                    result.addError(variable.getName(), "Value above maximum: " + variable.getMaxValue());
                }
            }
        }

        return result;
    }

    /**
     * Validate data type
     */
    private boolean validateDataType(DeviceTypeVariable.VariableDataType dataType, Object value) {
        return switch (dataType) {
            case NUMBER -> value instanceof Number || isNumeric(value.toString());
            case BOOLEAN -> value instanceof Boolean || value.toString().equalsIgnoreCase("true") || value.toString().equalsIgnoreCase("false");
            case STRING -> value instanceof String;
            case LOCATION -> value instanceof Map; // Simplified validation
            case DATETIME -> value instanceof String; // Simplified validation
            case JSON -> value instanceof Map || value instanceof List;
        };
    }

    /**
     * Check if string is numeric
     */
    private boolean isNumeric(String str) {
        try {
            new BigDecimal(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final java.util.List<ValidationError> errors = new java.util.ArrayList<>();

        public static ValidationResult valid() {
            return new ValidationResult();
        }

        public void addError(String field, String message) {
            errors.add(new ValidationError(field, message));
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<ValidationError> getErrors() {
            return errors;
        }

        public record ValidationError(String field, String message) {}
    }
}
