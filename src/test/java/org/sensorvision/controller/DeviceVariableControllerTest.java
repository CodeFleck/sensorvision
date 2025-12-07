package org.sensorvision.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.controller.DeviceVariableController.UpdateVariableRequest;
import org.sensorvision.dto.DeviceVariableResponse;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.Device;
import org.sensorvision.model.DeviceStatus;
import org.sensorvision.model.Organization;
import org.sensorvision.model.Variable;
import org.sensorvision.model.Variable.DataSource;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.repository.VariableRepository;
import org.sensorvision.security.SecurityUtils;
import org.sensorvision.service.DynamicVariableService;
import org.springframework.http.ResponseEntity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeviceVariableController.
 * Tests controller logic and validation constraints.
 */
@ExtendWith(MockitoExtension.class)
class DeviceVariableControllerTest {

    @Mock
    private DynamicVariableService dynamicVariableService;

    @Mock
    private VariableRepository variableRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private DeviceVariableController controller;

    private Validator validator;

    private Organization testOrg;
    private Device testDevice;
    private Variable testVariable;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        testOrg = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        testDevice = Device.builder()
                .id(UUID.randomUUID())
                .externalId("test-device-001")
                .name("Test Device")
                .status(DeviceStatus.ONLINE)
                .organization(testOrg)
                .build();

        testVariable = Variable.builder()
                .id(1L)
                .device(testDevice)
                .organization(testOrg)
                .name("temperature")
                .displayName("Temperature")
                .dataSource(DataSource.AUTO)
                .dataType(Variable.DataType.NUMBER)
                .unit("°C")
                .decimalPlaces(2)
                .lastValue(new BigDecimal("23.5"))
                .lastValueAt(Instant.now())
                .build();
    }

    // ==================== Controller Method Tests ====================

    @Test
    void getDeviceVariables_shouldReturnVariablesList() {
        // Arrange
        when(deviceRepository.findById(testDevice.getId()))
                .thenReturn(Optional.of(testDevice));
        when(securityUtils.getCurrentUserOrganization())
                .thenReturn(testOrg);
        when(dynamicVariableService.getDeviceVariables(testDevice.getId()))
                .thenReturn(Arrays.asList(testVariable));

        // Act
        ResponseEntity<List<DeviceVariableResponse>> response = controller.getDeviceVariables(testDevice.getId());

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("temperature", response.getBody().get(0).name());
        assertEquals("Temperature", response.getBody().get(0).displayName());
    }

    @Test
    void getDeviceVariables_deviceNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        UUID nonExistentDeviceId = UUID.randomUUID();
        when(deviceRepository.findById(nonExistentDeviceId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                controller.getDeviceVariables(nonExistentDeviceId));
    }

    @Test
    void getDeviceVariables_differentOrganization_shouldThrowResourceNotFoundException() {
        // Arrange
        Organization differentOrg = Organization.builder()
                .id(999L)
                .name("Different Org")
                .build();

        when(deviceRepository.findById(testDevice.getId()))
                .thenReturn(Optional.of(testDevice));
        when(securityUtils.getCurrentUserOrganization())
                .thenReturn(differentOrg);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                controller.getDeviceVariables(testDevice.getId()));
    }

    @Test
    void updateVariable_withValidRequest_shouldUpdateAndReturnVariable() {
        // Arrange
        UpdateVariableRequest request = new UpdateVariableRequest(
                "Updated Temperature",
                "Room temperature sensor",
                "°F",
                "thermometer",
                "#FF5733",
                3,
                0.0,
                100.0
        );

        when(deviceRepository.findById(testDevice.getId()))
                .thenReturn(Optional.of(testDevice));
        when(securityUtils.getCurrentUserOrganization())
                .thenReturn(testOrg);
        when(variableRepository.findById(testVariable.getId()))
                .thenReturn(Optional.of(testVariable));
        when(variableRepository.save(any(Variable.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Act
        ResponseEntity<DeviceVariableResponse> response = controller.updateVariable(
                testDevice.getId(), testVariable.getId(), request);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Updated Temperature", response.getBody().displayName());
        assertEquals("Room temperature sensor", response.getBody().description());
        assertEquals("°F", response.getBody().unit());
        assertEquals("#FF5733", response.getBody().color());
        assertEquals(3, response.getBody().decimalPlaces());
    }

    @Test
    void updateVariable_variableFromDifferentDevice_shouldThrowResourceNotFoundException() {
        // Arrange
        Device differentDevice = Device.builder()
                .id(UUID.randomUUID())
                .externalId("different-device")
                .organization(testOrg)
                .build();

        Variable variableFromDifferentDevice = Variable.builder()
                .id(2L)
                .device(differentDevice)
                .organization(testOrg)
                .name("humidity")
                .build();

        UpdateVariableRequest request = new UpdateVariableRequest(
                "Test", null, null, null, null, null, null, null);

        when(deviceRepository.findById(testDevice.getId()))
                .thenReturn(Optional.of(testDevice));
        when(securityUtils.getCurrentUserOrganization())
                .thenReturn(testOrg);
        when(variableRepository.findById(2L))
                .thenReturn(Optional.of(variableFromDifferentDevice));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                controller.updateVariable(testDevice.getId(), 2L, request));
    }

    @Test
    void updateVariable_variableWithNullDevice_shouldThrowResourceNotFoundException() {
        // Arrange
        Variable orgLevelVariable = Variable.builder()
                .id(3L)
                .device(null)
                .organization(testOrg)
                .name("template_var")
                .build();

        UpdateVariableRequest request = new UpdateVariableRequest(
                "Test", null, null, null, null, null, null, null);

        when(deviceRepository.findById(testDevice.getId()))
                .thenReturn(Optional.of(testDevice));
        when(securityUtils.getCurrentUserOrganization())
                .thenReturn(testOrg);
        when(variableRepository.findById(3L))
                .thenReturn(Optional.of(orgLevelVariable));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                controller.updateVariable(testDevice.getId(), 3L, request));
    }

    // ==================== Validation Constraint Tests ====================

    @Test
    void updateVariableRequest_displayNameTooLong_shouldHaveViolation() {
        // Arrange
        String longDisplayName = "A".repeat(256);
        UpdateVariableRequest request = new UpdateVariableRequest(
                longDisplayName, null, null, null, null, null, null, null);

        // Act
        Set<ConstraintViolation<UpdateVariableRequest>> violations = validator.validate(request);

        // Assert
        assertEquals(1, violations.size());
        ConstraintViolation<UpdateVariableRequest> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("255"));
    }

    @Test
    void updateVariableRequest_descriptionTooLong_shouldHaveViolation() {
        // Arrange
        String longDescription = "A".repeat(501);
        UpdateVariableRequest request = new UpdateVariableRequest(
                null, longDescription, null, null, null, null, null, null);

        // Act
        Set<ConstraintViolation<UpdateVariableRequest>> violations = validator.validate(request);

        // Assert
        assertEquals(1, violations.size());
        ConstraintViolation<UpdateVariableRequest> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("500"));
    }

    @Test
    void updateVariableRequest_unitTooLong_shouldHaveViolation() {
        // Arrange
        String longUnit = "A".repeat(51);
        UpdateVariableRequest request = new UpdateVariableRequest(
                null, null, longUnit, null, null, null, null, null);

        // Act
        Set<ConstraintViolation<UpdateVariableRequest>> violations = validator.validate(request);

        // Assert
        assertEquals(1, violations.size());
        ConstraintViolation<UpdateVariableRequest> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("50"));
    }

    @Test
    void updateVariableRequest_iconTooLong_shouldHaveViolation() {
        // Arrange
        String longIcon = "A".repeat(101);
        UpdateVariableRequest request = new UpdateVariableRequest(
                null, null, null, longIcon, null, null, null, null);

        // Act
        Set<ConstraintViolation<UpdateVariableRequest>> violations = validator.validate(request);

        // Assert
        assertEquals(1, violations.size());
        ConstraintViolation<UpdateVariableRequest> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("100"));
    }

    @Test
    void updateVariableRequest_invalidColorFormat_shouldHaveViolation() {
        // Arrange - Invalid color format (not #RRGGBB)
        UpdateVariableRequest request = new UpdateVariableRequest(
                null, null, null, null, "red", null, null, null);

        // Act
        Set<ConstraintViolation<UpdateVariableRequest>> violations = validator.validate(request);

        // Assert
        assertEquals(1, violations.size());
        ConstraintViolation<UpdateVariableRequest> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("hex format"));
    }

    @Test
    void updateVariableRequest_validHexColor_shouldHaveNoViolation() {
        // Arrange
        UpdateVariableRequest request = new UpdateVariableRequest(
                null, null, null, null, "#FF5733", null, null, null);

        // Act
        Set<ConstraintViolation<UpdateVariableRequest>> violations = validator.validate(request);

        // Assert
        assertTrue(violations.isEmpty());
    }

    @Test
    void updateVariableRequest_lowercaseHexColor_shouldHaveNoViolation() {
        // Arrange
        UpdateVariableRequest request = new UpdateVariableRequest(
                null, null, null, null, "#ff5733", null, null, null);

        // Act
        Set<ConstraintViolation<UpdateVariableRequest>> violations = validator.validate(request);

        // Assert
        assertTrue(violations.isEmpty());
    }

    @Test
    void updateVariableRequest_emptyColor_shouldHaveNoViolation() {
        // Arrange - Empty string should be allowed
        UpdateVariableRequest request = new UpdateVariableRequest(
                null, null, null, null, "", null, null, null);

        // Act
        Set<ConstraintViolation<UpdateVariableRequest>> violations = validator.validate(request);

        // Assert
        assertTrue(violations.isEmpty());
    }

    @Test
    void updateVariableRequest_decimalPlacesNegative_shouldHaveViolation() {
        // Arrange
        UpdateVariableRequest request = new UpdateVariableRequest(
                null, null, null, null, null, -1, null, null);

        // Act
        Set<ConstraintViolation<UpdateVariableRequest>> violations = validator.validate(request);

        // Assert
        assertEquals(1, violations.size());
        ConstraintViolation<UpdateVariableRequest> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("at least 0"));
    }

    @Test
    void updateVariableRequest_decimalPlacesTooHigh_shouldHaveViolation() {
        // Arrange
        UpdateVariableRequest request = new UpdateVariableRequest(
                null, null, null, null, null, 11, null, null);

        // Act
        Set<ConstraintViolation<UpdateVariableRequest>> violations = validator.validate(request);

        // Assert
        assertEquals(1, violations.size());
        ConstraintViolation<UpdateVariableRequest> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("at most 10"));
    }

    @Test
    void updateVariableRequest_decimalPlacesValid_shouldHaveNoViolation() {
        // Arrange - Valid range 0-10
        for (int i = 0; i <= 10; i++) {
            UpdateVariableRequest request = new UpdateVariableRequest(
                    null, null, null, null, null, i, null, null);

            // Act
            Set<ConstraintViolation<UpdateVariableRequest>> violations = validator.validate(request);

            // Assert
            assertTrue(violations.isEmpty(), "Decimal places " + i + " should be valid");
        }
    }

    @Test
    void updateVariableRequest_allFieldsNull_shouldHaveNoViolation() {
        // Arrange - All optional fields as null
        UpdateVariableRequest request = new UpdateVariableRequest(
                null, null, null, null, null, null, null, null);

        // Act
        Set<ConstraintViolation<UpdateVariableRequest>> violations = validator.validate(request);

        // Assert
        assertTrue(violations.isEmpty());
    }

    @Test
    void updateVariableRequest_multipleViolations_shouldReportAll() {
        // Arrange
        String longDisplayName = "A".repeat(256);
        String longDescription = "B".repeat(501);
        UpdateVariableRequest request = new UpdateVariableRequest(
                longDisplayName, longDescription, null, null, "invalid", -1, null, null);

        // Act
        Set<ConstraintViolation<UpdateVariableRequest>> violations = validator.validate(request);

        // Assert
        assertEquals(4, violations.size());
    }
}
