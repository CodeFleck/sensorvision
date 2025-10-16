package org.sensorvision.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.DeviceImportRequest;
import org.sensorvision.dto.TelemetryImportRequest;
import org.sensorvision.model.Device;
import org.sensorvision.model.DeviceStatus;
import org.sensorvision.model.Organization;
import org.sensorvision.model.User;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.repository.OrganizationRepository;
import org.sensorvision.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportService {

    private final TelemetryIngestionService telemetryIngestionService;
    private final DeviceRepository deviceRepository;
    private final OrganizationRepository organizationRepository;
    private final ObjectMapper objectMapper;

    /**
     * Import telemetry data from CSV file
     * Expected CSV format: deviceId,timestamp,variable1,variable2,...
     * Example: meter-001,2024-01-01T12:00:00Z,kwConsumption,50.5,voltage,220.1
     */
    public ImportResult importTelemetryFromCsv(MultipartFile file) throws IOException {
        log.info("Starting CSV import from file: {}", file.getOriginalFilename());

        ImportResult result = new ImportResult();
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            // Parse header to determine variable names
            String[] headers = headerLine.split(",");
            if (headers.length < 3) {
                throw new IllegalArgumentException("CSV must have at least: deviceId,timestamp,variable1");
            }

            // Validate required columns
            if (!"deviceId".equalsIgnoreCase(headers[0].trim())) {
                throw new IllegalArgumentException("First column must be 'deviceId'");
            }
            if (!"timestamp".equalsIgnoreCase(headers[1].trim())) {
                throw new IllegalArgumentException("Second column must be 'timestamp'");
            }

            // Extract variable names from remaining columns
            List<String> variableNames = new ArrayList<>();
            for (int i = 2; i < headers.length; i++) {
                variableNames.add(headers[i].trim());
            }

            // Process data rows
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    TelemetryImportRequest importRequest = parseCsvLine(line, variableNames);
                    // Allow auto-provision for import - user is authenticated
                    telemetryIngestionService.ingest(convertToPayload(importRequest), true);
                    result.incrementSuccessCount();
                } catch (Exception e) {
                    String errorMsg = String.format("Line %d: %s - %s", lineNumber, e.getMessage(), line);
                    log.warn(errorMsg);
                    errors.add(errorMsg);
                    result.incrementFailureCount();
                }
            }
        }

        result.setErrors(errors);
        log.info("CSV import completed: {} success, {} failures", result.getSuccessCount(), result.getFailureCount());
        return result;
    }

    /**
     * Import telemetry data from JSON file
     * Expected JSON format: array of { deviceId, timestamp, variables: { var1: val1, var2: val2 } }
     */
    public ImportResult importTelemetryFromJson(MultipartFile file) throws IOException {
        log.info("Starting JSON import from file: {}", file.getOriginalFilename());

        ImportResult result = new ImportResult();
        List<String> errors = new ArrayList<>();

        try {
            TelemetryImportRequest[] records = objectMapper.readValue(
                file.getInputStream(),
                TelemetryImportRequest[].class
            );

            for (int i = 0; i < records.length; i++) {
                try {
                    TelemetryImportRequest record = records[i];
                    validateImportRequest(record);
                    // Allow auto-provision for import - user is authenticated
                    telemetryIngestionService.ingest(convertToPayload(record), true);
                    result.incrementSuccessCount();
                } catch (Exception e) {
                    String errorMsg = String.format("Record %d: %s", i + 1, e.getMessage());
                    log.warn(errorMsg, e);
                    errors.add(errorMsg);
                    result.incrementFailureCount();
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage(), e);
        }

        result.setErrors(errors);
        log.info("JSON import completed: {} success, {} failures", result.getSuccessCount(), result.getFailureCount());
        return result;
    }

    private TelemetryImportRequest parseCsvLine(String line, List<String> variableNames) {
        String[] values = line.split(",");

        if (values.length < 2 + variableNames.size()) {
            throw new IllegalArgumentException("Insufficient columns in CSV line");
        }

        String deviceId = values[0].trim();
        String timestampStr = values[1].trim();

        if (deviceId.isEmpty()) {
            throw new IllegalArgumentException("deviceId cannot be empty");
        }

        Instant timestamp;
        try {
            timestamp = Instant.parse(timestampStr);
        } catch (DateTimeParseException e) {
            // Try alternate format
            try {
                timestamp = DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(timestampStr, Instant::from);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid timestamp format: " + timestampStr);
            }
        }

        Map<String, Double> variables = new HashMap<>();
        for (int i = 0; i < variableNames.size(); i++) {
            String valueStr = values[i + 2].trim();
            if (!valueStr.isEmpty()) {
                try {
                    variables.put(variableNames.get(i), Double.parseDouble(valueStr));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid numeric value: " + valueStr);
                }
            }
        }

        return new TelemetryImportRequest(deviceId, timestamp, variables);
    }

    private void validateImportRequest(TelemetryImportRequest request) {
        if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
            throw new IllegalArgumentException("deviceId is required");
        }
        if (request.getVariables() == null || request.getVariables().isEmpty()) {
            throw new IllegalArgumentException("At least one variable is required");
        }
    }

    private org.sensorvision.mqtt.TelemetryPayload convertToPayload(TelemetryImportRequest request) {
        Instant timestamp = request.getTimestamp() != null ? request.getTimestamp() : Instant.now();

        Map<String, java.math.BigDecimal> variables = new HashMap<>();
        request.getVariables().forEach((key, value) ->
            variables.put(key, java.math.BigDecimal.valueOf(value))
        );

        return new org.sensorvision.mqtt.TelemetryPayload(
            request.getDeviceId(),
            timestamp,
            variables,
            Map.of()
        );
    }

    /**
     * Import devices from CSV file
     * Expected CSV format: externalId,name,location,sensorType,firmwareVersion,status,latitude,longitude,altitude
     */
    public ImportResult importDevicesFromCsv(MultipartFile file) throws IOException {
        log.info("Starting device CSV import from file: {}", file.getOriginalFilename());

        ImportResult result = new ImportResult();
        List<String> errors = new ArrayList<>();
        Organization organization = getCurrentOrganization();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }

            // Parse header
            String[] headers = headerLine.split(",");
            if (headers.length < 2) {
                throw new IllegalArgumentException("CSV must have at least: externalId,name");
            }

            // Validate required columns
            if (!"externalId".equalsIgnoreCase(headers[0].trim())) {
                throw new IllegalArgumentException("First column must be 'externalId'");
            }
            if (!"name".equalsIgnoreCase(headers[1].trim())) {
                throw new IllegalArgumentException("Second column must be 'name'");
            }

            // Process data rows
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    DeviceImportRequest importRequest = parseDeviceCsvLine(line);
                    createOrUpdateDevice(importRequest, organization);
                    result.incrementSuccessCount();
                } catch (Exception e) {
                    String errorMsg = String.format("Line %d: %s - %s", lineNumber, e.getMessage(), line);
                    log.warn(errorMsg);
                    errors.add(errorMsg);
                    result.incrementFailureCount();
                }
            }
        }

        result.setErrors(errors);
        log.info("Device CSV import completed: {} success, {} failures",
                 result.getSuccessCount(), result.getFailureCount());
        return result;
    }

    /**
     * Import devices from JSON file
     * Expected JSON format: array of device objects
     */
    public ImportResult importDevicesFromJson(MultipartFile file) throws IOException {
        log.info("Starting device JSON import from file: {}", file.getOriginalFilename());

        ImportResult result = new ImportResult();
        List<String> errors = new ArrayList<>();
        Organization organization = getCurrentOrganization();

        try {
            DeviceImportRequest[] records = objectMapper.readValue(
                file.getInputStream(),
                DeviceImportRequest[].class
            );

            for (int i = 0; i < records.length; i++) {
                try {
                    DeviceImportRequest record = records[i];
                    validateDeviceImportRequest(record);
                    createOrUpdateDevice(record, organization);
                    result.incrementSuccessCount();
                } catch (Exception e) {
                    String errorMsg = String.format("Record %d: %s", i + 1, e.getMessage());
                    log.warn(errorMsg, e);
                    errors.add(errorMsg);
                    result.incrementFailureCount();
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage(), e);
        }

        result.setErrors(errors);
        log.info("Device JSON import completed: {} success, {} failures",
                 result.getSuccessCount(), result.getFailureCount());
        return result;
    }

    private DeviceImportRequest parseDeviceCsvLine(String line) {
        String[] values = line.split(",");

        if (values.length < 2) {
            throw new IllegalArgumentException("Insufficient columns in CSV line");
        }

        DeviceImportRequest request = new DeviceImportRequest();
        request.setExternalId(getValue(values, 0));
        request.setName(getValue(values, 1));
        request.setLocation(getValue(values, 2));
        request.setSensorType(getValue(values, 3));
        request.setFirmwareVersion(getValue(values, 4));
        request.setStatus(getValue(values, 5));

        // Parse geolocation fields
        String latStr = getValue(values, 6);
        if (latStr != null && !latStr.isEmpty()) {
            try {
                request.setLatitude(new java.math.BigDecimal(latStr));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid latitude value: " + latStr);
            }
        }

        String lonStr = getValue(values, 7);
        if (lonStr != null && !lonStr.isEmpty()) {
            try {
                request.setLongitude(new java.math.BigDecimal(lonStr));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid longitude value: " + lonStr);
            }
        }

        String altStr = getValue(values, 8);
        if (altStr != null && !altStr.isEmpty()) {
            try {
                request.setAltitude(new java.math.BigDecimal(altStr));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid altitude value: " + altStr);
            }
        }

        return request;
    }

    private String getValue(String[] values, int index) {
        if (index < values.length) {
            String value = values[index].trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }

    private void validateDeviceImportRequest(DeviceImportRequest request) {
        if (request.getExternalId() == null || request.getExternalId().trim().isEmpty()) {
            throw new IllegalArgumentException("externalId is required");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
    }

    private void createOrUpdateDevice(DeviceImportRequest request, Organization organization) {
        Device device = deviceRepository.findByExternalId(request.getExternalId())
                .orElse(null);

        if (device == null) {
            // Create new device
            device = Device.builder()
                    .externalId(request.getExternalId())
                    .name(request.getName())
                    .location(request.getLocation())
                    .sensorType(request.getSensorType())
                    .firmwareVersion(request.getFirmwareVersion())
                    .organization(organization)
                    .build();
        } else {
            // Update existing device
            device.setName(request.getName());
            if (request.getLocation() != null) {
                device.setLocation(request.getLocation());
            }
            if (request.getSensorType() != null) {
                device.setSensorType(request.getSensorType());
            }
            if (request.getFirmwareVersion() != null) {
                device.setFirmwareVersion(request.getFirmwareVersion());
            }
        }

        // Set status if provided
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            try {
                device.setStatus(DeviceStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid device status '{}', keeping existing status", request.getStatus());
            }
        }

        // Set geolocation if provided
        if (request.getLatitude() != null) {
            device.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            device.setLongitude(request.getLongitude());
        }
        if (request.getAltitude() != null) {
            device.setAltitude(request.getAltitude());
        }
        if (request.getLatitude() != null || request.getLongitude() != null || request.getAltitude() != null) {
            device.setLocationUpdatedAt(java.time.LocalDateTime.now());
        }

        deviceRepository.save(device);
    }

    private Organization getCurrentOrganization() {
        User currentUser = SecurityUtils.getCurrentUser();
        Organization organization = currentUser.getOrganization();
        if (organization == null) {
            throw new IllegalStateException("User organization not found");
        }
        return organization;
    }

    public static class ImportResult {
        private int successCount = 0;
        private int failureCount = 0;
        private List<String> errors = new ArrayList<>();

        public void incrementSuccessCount() {
            successCount++;
        }

        public void incrementFailureCount() {
            failureCount++;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }

        public int getTotalCount() {
            return successCount + failureCount;
        }
    }
}
