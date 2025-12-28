package io.indcloud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.dto.VariableRequest;
import io.indcloud.dto.VariableResponse;
import io.indcloud.exception.BadRequestException;
import io.indcloud.exception.ResourceNotFoundException;
import io.indcloud.model.Event;
import io.indcloud.model.Organization;
import io.indcloud.model.Variable;
import io.indcloud.repository.VariableRepository;
import io.indcloud.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VariableService {

    private final VariableRepository variableRepository;
    private final EventService eventService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<VariableResponse> getAllVariables() {
        Organization org = securityUtils.getCurrentUserOrganization();
        return variableRepository.findByOrganizationId(org.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VariableResponse getVariable(Long id) {
        Variable variable = findVariableById(id);
        return toResponse(variable);
    }

    public VariableResponse createVariable(VariableRequest request) {
        Organization org = securityUtils.getCurrentUserOrganization();

        // Check for duplicate name within organization
        if (variableRepository.existsByOrganizationIdAndName(org.getId(), request.name())) {
            throw new BadRequestException("Variable with name '" + request.name() + "' already exists");
        }

        Variable variable = Variable.builder()
                .organization(org)
                .name(request.name())
                .displayName(request.displayName())
                .description(request.description())
                .unit(request.unit())
                .dataType(request.dataType())
                .icon(request.icon())
                .color(request.color())
                .minValue(request.minValue())
                .maxValue(request.maxValue())
                .decimalPlaces(request.decimalPlaces() != null ? request.decimalPlaces() : 2)
                .isSystemVariable(false)
                .build();

        Variable saved = variableRepository.save(variable);

        // Emit event
        eventService.createEvent(
                org,
                Event.EventType.DEVICE_UPDATED,
                Event.EventSeverity.INFO,
                "Variable '" + saved.getName() + "' created",
                null
        );

        log.info("Created variable: {} for organization: {}", saved.getName(), org.getId());
        return toResponse(saved);
    }

    public VariableResponse updateVariable(Long id, VariableRequest request) {
        Variable variable = findVariableById(id);

        // System variables can only have metadata updated, not name
        if (variable.getIsSystemVariable() && !variable.getName().equals(request.name())) {
            throw new BadRequestException("Cannot change name of system variable");
        }

        // Check for duplicate name (excluding current variable)
        variableRepository.findByOrganizationIdAndName(
                variable.getOrganization().getId(),
                request.name()
        ).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BadRequestException("Variable with name '" + request.name() + "' already exists");
            }
        });

        variable.setName(request.name());
        variable.setDisplayName(request.displayName());
        variable.setDescription(request.description());
        variable.setUnit(request.unit());
        variable.setDataType(request.dataType());
        variable.setIcon(request.icon());
        variable.setColor(request.color());
        variable.setMinValue(request.minValue());
        variable.setMaxValue(request.maxValue());
        variable.setDecimalPlaces(request.decimalPlaces() != null ? request.decimalPlaces() : variable.getDecimalPlaces());

        Variable updated = variableRepository.save(variable);

        // Emit event
        eventService.createEvent(
                variable.getOrganization(),
                Event.EventType.DEVICE_UPDATED,
                Event.EventSeverity.INFO,
                "Variable '" + updated.getName() + "' updated",
                null
        );

        log.info("Updated variable: {}", id);
        return toResponse(updated);
    }

    public void deleteVariable(Long id) {
        Variable variable = findVariableById(id);

        // Prevent deletion of system variables
        if (variable.getIsSystemVariable()) {
            throw new BadRequestException("Cannot delete system variable");
        }

        String variableName = variable.getName();
        Organization org = variable.getOrganization();

        variableRepository.delete(variable);

        // Emit event
        eventService.createEvent(
                org,
                Event.EventType.DEVICE_UPDATED,
                Event.EventSeverity.INFO,
                "Variable '" + variableName + "' deleted",
                null
        );

        log.info("Deleted variable: {}", id);
    }

    private Variable findVariableById(Long id) {
        Variable variable = variableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Variable not found: " + id));

        // Verify organization access
        Organization currentOrg = securityUtils.getCurrentUserOrganization();
        if (!variable.getOrganization().getId().equals(currentOrg.getId())) {
            throw new ResourceNotFoundException("Variable not found: " + id);
        }

        return variable;
    }

    private VariableResponse toResponse(Variable variable) {
        return new VariableResponse(
                variable.getId(),
                variable.getName(),
                variable.getDisplayName(),
                variable.getDescription(),
                variable.getUnit(),
                variable.getDataType(),
                variable.getIcon(),
                variable.getColor(),
                variable.getMinValue(),
                variable.getMaxValue(),
                variable.getDecimalPlaces(),
                variable.getIsSystemVariable(),
                variable.getCreatedAt(),
                variable.getUpdatedAt()
        );
    }
}
