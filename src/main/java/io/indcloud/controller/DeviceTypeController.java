package io.indcloud.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.indcloud.dto.*;
import io.indcloud.model.*;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.security.SecurityUtils;
import io.indcloud.service.DeviceAutoProvisioningService;
import io.indcloud.service.DeviceTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/device-types")
@RequiredArgsConstructor
@Tag(name = "Device Types", description = "Device type template management and auto-provisioning endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class DeviceTypeController {

    private final DeviceTypeService deviceTypeService;
    private final DeviceAutoProvisioningService autoProvisioningService;
    private final DeviceRepository deviceRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    @Operation(summary = "Get all device types", description = "Returns all device types for the current organization including system templates")
    public ResponseEntity<List<DeviceTypeResponse>> getAllDeviceTypes() {
        log.debug("REST request to get all device types");
        Organization org = securityUtils.getCurrentUserOrganization();
        List<DeviceType> deviceTypes = deviceTypeService.getActiveDeviceTypes(org);
        List<DeviceTypeResponse> responses = deviceTypes.stream()
                .map(DeviceTypeResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    @Operation(summary = "Get available templates", description = "Returns all available device type templates including system templates")
    public ResponseEntity<List<DeviceTypeResponse>> getAvailableTemplates() {
        log.debug("REST request to get available device type templates");
        Organization org = securityUtils.getCurrentUserOrganization();
        List<DeviceType> templates = deviceTypeService.getAllDeviceTypes(org);
        List<DeviceTypeResponse> responses = templates.stream()
                .filter(t -> t.getIsActive())
                .map(DeviceTypeResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    @Operation(summary = "Get device type by ID", description = "Returns a single device type with all its templates")
    public ResponseEntity<DeviceTypeResponse> getDeviceType(@PathVariable Long id) {
        log.debug("REST request to get device type: {}", id);
        DeviceType deviceType = deviceTypeService.getDeviceType(id);
        return ResponseEntity.ok(DeviceTypeResponse.from(deviceType));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Create device type", description = "Creates a new device type template")
    public ResponseEntity<DeviceTypeResponse> createDeviceType(@Valid @RequestBody DeviceTypeRequest request) {
        log.debug("REST request to create device type: {}", request.name());
        Organization org = securityUtils.getCurrentUserOrganization();

        List<DeviceTypeVariable> variables = request.variables() != null
                ? request.variables().stream().map(this::toVariable).collect(Collectors.toList())
                : List.of();

        DeviceType created = deviceTypeService.createDeviceType(
                org,
                request.name(),
                request.description(),
                request.icon(),
                variables
        );

        // Set additional fields
        if (request.color() != null) {
            created.setColor(request.color());
        }
        if (request.category() != null) {
            created.setTemplateCategory(request.category());
        }

        // Add rule templates
        if (request.ruleTemplates() != null) {
            for (DeviceTypeRuleTemplateRequest ruleReq : request.ruleTemplates()) {
                DeviceTypeRuleTemplate ruleTemplate = toRuleTemplate(ruleReq);
                created.addRuleTemplate(ruleTemplate);
            }
        }

        // Add dashboard templates
        if (request.dashboardTemplates() != null) {
            for (DeviceTypeDashboardTemplateRequest dashReq : request.dashboardTemplates()) {
                DeviceTypeDashboardTemplate dashTemplate = toDashboardTemplate(dashReq);
                created.addDashboardTemplate(dashTemplate);
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(DeviceTypeResponse.from(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Update device type", description = "Updates an existing device type (system templates cannot be modified)")
    public ResponseEntity<DeviceTypeResponse> updateDeviceType(
            @PathVariable Long id,
            @Valid @RequestBody DeviceTypeRequest request) {
        log.debug("REST request to update device type: {}", id);

        DeviceType deviceType = deviceTypeService.getDeviceType(id);
        if (!deviceType.isEditable()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        DeviceType updated = deviceTypeService.updateDeviceType(
                id,
                request.name(),
                request.description(),
                request.icon(),
                null // isActive unchanged
        );

        if (request.color() != null) {
            updated.setColor(request.color());
        }
        if (request.category() != null) {
            updated.setTemplateCategory(request.category());
        }

        return ResponseEntity.ok(DeviceTypeResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete device type", description = "Deletes a device type (admin only, system templates cannot be deleted)")
    public ResponseEntity<Void> deleteDeviceType(@PathVariable Long id) {
        log.debug("REST request to delete device type: {}", id);

        DeviceType deviceType = deviceTypeService.getDeviceType(id);
        if (!deviceType.isEditable()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        deviceTypeService.deleteDeviceType(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/apply/{deviceExternalId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Apply template to device", description = "Applies a device type template to a device, creating variables, rules, and dashboard")
    public ResponseEntity<DeviceTemplateApplicationResponse> applyTemplateToDevice(
            @PathVariable Long id,
            @PathVariable String deviceExternalId) {
        log.debug("REST request to apply device type {} to device {}", id, deviceExternalId);

        DeviceType deviceType = deviceTypeService.getDeviceType(id);
        Device device = deviceRepository.findByExternalId(deviceExternalId)
                .orElseThrow(() -> new io.indcloud.exception.ResourceNotFoundException("Device not found: " + deviceExternalId));

        String currentUser = securityUtils.getCurrentUser().getUsername();
        DeviceTemplateApplication application = autoProvisioningService.applyTemplate(device, deviceType, currentUser);

        return ResponseEntity.ok(DeviceTemplateApplicationResponse.from(application));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    @Operation(summary = "Get template categories", description = "Returns all available device type categories")
    public ResponseEntity<DeviceType.TemplateCategory[]> getCategories() {
        return ResponseEntity.ok(DeviceType.TemplateCategory.values());
    }

    // Helper methods to convert DTOs to entities

    private DeviceTypeVariable toVariable(DeviceTypeVariableRequest req) {
        return DeviceTypeVariable.builder()
                .name(req.name())
                .label(req.label())
                .unit(req.unit())
                .dataType(req.dataType() != null ? req.dataType() : DeviceTypeVariable.VariableDataType.NUMBER)
                .minValue(req.minValue())
                .maxValue(req.maxValue())
                .required(req.required() != null ? req.required() : false)
                .defaultValue(req.defaultValue())
                .description(req.description())
                .displayOrder(req.displayOrder())
                .build();
    }

    private DeviceTypeRuleTemplate toRuleTemplate(DeviceTypeRuleTemplateRequest req) {
        return DeviceTypeRuleTemplate.builder()
                .name(req.name())
                .description(req.description())
                .variableName(req.variableName())
                .operator(req.operator())
                .thresholdValue(req.thresholdValue())
                .severity(req.severity() != null ? req.severity() : DeviceTypeRuleTemplate.RuleSeverity.WARNING)
                .notificationMessage(req.notificationMessage())
                .enabled(req.enabled() != null ? req.enabled() : true)
                .displayOrder(req.displayOrder())
                .build();
    }

    private DeviceTypeDashboardTemplate toDashboardTemplate(DeviceTypeDashboardTemplateRequest req) {
        JsonNode configNode = null;
        if (req.config() != null) {
            configNode = objectMapper.valueToTree(req.config());
        }

        return DeviceTypeDashboardTemplate.builder()
                .widgetType(req.widgetType())
                .title(req.title())
                .variableName(req.variableName())
                .config(configNode)
                .gridX(req.gridX() != null ? req.gridX() : 0)
                .gridY(req.gridY() != null ? req.gridY() : 0)
                .gridWidth(req.gridWidth() != null ? req.gridWidth() : 4)
                .gridHeight(req.gridHeight() != null ? req.gridHeight() : 2)
                .displayOrder(req.displayOrder())
                .build();
    }

    /**
     * Response for template application result.
     */
    public record DeviceTemplateApplicationResponse(
            Long id,
            String deviceId,
            Long deviceTypeId,
            String deviceTypeName,
            int variablesCreated,
            int rulesCreated,
            boolean dashboardCreated,
            java.time.Instant appliedAt,
            String appliedBy
    ) {
        public static DeviceTemplateApplicationResponse from(DeviceTemplateApplication app) {
            return new DeviceTemplateApplicationResponse(
                    app.getId(),
                    app.getDeviceId().toString(),
                    app.getDeviceType().getId(),
                    app.getDeviceType().getName(),
                    app.getVariablesCreated(),
                    app.getRulesCreated(),
                    app.getDashboardCreated(),
                    app.getAppliedAt(),
                    app.getAppliedBy()
            );
        }
    }
}
