package org.sensorvision.controller;

import org.sensorvision.dto.ApiResponse;
import org.sensorvision.dto.OrganizationDto;
import org.sensorvision.exception.BadRequestException;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.Organization;
import org.sensorvision.model.TrashLog;
import org.sensorvision.repository.OrganizationRepository;
import org.sensorvision.repository.UserRepository;
import org.sensorvision.service.TrashService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/organizations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrganizationController {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final TrashService trashService;

    public AdminOrganizationController(OrganizationRepository organizationRepository,
                                       UserRepository userRepository,
                                       TrashService trashService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.trashService = trashService;
    }

    @GetMapping
    public ResponseEntity<List<OrganizationDto>> getAllOrganizations() {
        // Only return non-deleted organizations
        List<Organization> organizations = organizationRepository.findAllActive();
        List<OrganizationDto> organizationDtos = organizations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(organizationDtos);
    }

    @GetMapping("/{organizationId}")
    public ResponseEntity<OrganizationDto> getOrganization(@PathVariable Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));
        return ResponseEntity.ok(convertToDto(organization));
    }

    @PutMapping("/{organizationId}/enable")
    public ResponseEntity<ApiResponse<OrganizationDto>> enableOrganization(@PathVariable Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        organization.setEnabled(true);
        organization = organizationRepository.save(organization);

        return ResponseEntity.ok(ApiResponse.success(
                convertToDto(organization),
                "Organization enabled successfully"
        ));
    }

    @PutMapping("/{organizationId}/disable")
    public ResponseEntity<ApiResponse<OrganizationDto>> disableOrganization(@PathVariable Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        organization.setEnabled(false);
        organization = organizationRepository.save(organization);

        return ResponseEntity.ok(ApiResponse.success(
                convertToDto(organization),
                "Organization disabled successfully"
        ));
    }

    @PutMapping("/{organizationId}")
    public ResponseEntity<ApiResponse<OrganizationDto>> updateOrganization(
            @PathVariable Long organizationId,
            @RequestBody OrganizationUpdateRequest request) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        if (request.getName() != null) {
            organization.setName(request.getName());
        }
        if (request.getDescription() != null) {
            organization.setDescription(request.getDescription());
        }

        organization = organizationRepository.save(organization);

        return ResponseEntity.ok(ApiResponse.success(
                convertToDto(organization),
                "Organization updated successfully"
        ));
    }

    @DeleteMapping("/{organizationId}")
    public ResponseEntity<ApiResponse<SoftDeleteResponse>> deleteOrganization(
            @PathVariable Long organizationId,
            @RequestParam(required = false) String reason) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        // Don't allow deleting already deleted organizations
        if (organization.isDeleted()) {
            throw new BadRequestException("Organization is already deleted");
        }

        // Use soft delete (TrashService checks for active users)
        TrashLog trashLog = trashService.softDeleteOrganization(organizationId, reason);

        SoftDeleteResponse response = new SoftDeleteResponse(
                trashLog.getId(),
                trashLog.getEntityType(),
                trashLog.getEntityName(),
                trashLog.getExpiresAt().toString(),
                trashLog.getDaysRemaining()
        );

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Organization moved to trash. You can restore it within " + TrashLog.RETENTION_DAYS + " days."
        ));
    }

    /**
     * Response for soft delete operations, including undo info.
     */
    public record SoftDeleteResponse(
            Long trashId,
            String entityType,
            String entityName,
            String expiresAt,
            long daysRemaining
    ) {}

    private OrganizationDto convertToDto(Organization organization) {
        OrganizationDto dto = new OrganizationDto();
        dto.setId(organization.getId());
        dto.setName(organization.getName());
        dto.setDescription(organization.getDescription());
        dto.setEnabled(organization.isEnabled());
        dto.setCreatedAt(organization.getCreatedAt() != null ?
            LocalDateTime.ofInstant(organization.getCreatedAt(), java.time.ZoneId.systemDefault()) : null);
        dto.setUpdatedAt(organization.getUpdatedAt() != null ?
            LocalDateTime.ofInstant(organization.getUpdatedAt(), java.time.ZoneId.systemDefault()) : null);

        // Count users in organization
        long userCount = userRepository.countByOrganizationId(organization.getId());
        dto.setUserCount(userCount);

        return dto;
    }

    // Inner class for update request
    public static class OrganizationUpdateRequest {
        private String name;
        private String description;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
