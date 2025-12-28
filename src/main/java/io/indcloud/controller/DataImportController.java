package io.indcloud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.service.DataImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/import")
@RequiredArgsConstructor
@Tag(name = "Data Import", description = "APIs for bulk data import from CSV/JSON files")
public class DataImportController {

    private final DataImportService dataImportService;

    @PostMapping(value = "/telemetry/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Import telemetry data from CSV",
               description = "Bulk import telemetry data from CSV file. " +
                             "Format: deviceId,timestamp,variable1,variable2,... " +
                             "Example: meter-001,2024-01-01T12:00:00Z,50.5,220.1")
    public ResponseEntity<Map<String, Object>> importTelemetryFromCsv(
            @RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "File is empty"
                ));
            }

            if (!file.getOriginalFilename().endsWith(".csv")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "File must be a CSV file"
                ));
            }

            DataImportService.ImportResult result = dataImportService.importTelemetryFromCsv(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalRecords", result.getTotalCount());
            response.put("successCount", result.getSuccessCount());
            response.put("failureCount", result.getFailureCount());
            response.put("errors", result.getErrors());

            if (result.getFailureCount() > 0) {
                response.put("message", String.format(
                    "Imported with partial success: %d succeeded, %d failed",
                    result.getSuccessCount(), result.getFailureCount()
                ));
            } else {
                response.put("message", String.format(
                    "Successfully imported %d records",
                    result.getSuccessCount()
                ));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error importing CSV file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Import failed: " + e.getMessage()
            ));
        }
    }

    @PostMapping(value = "/telemetry/json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Import telemetry data from JSON",
               description = "Bulk import telemetry data from JSON file. " +
                             "Format: array of { deviceId, timestamp, variables: { var1: val1, var2: val2 } }")
    public ResponseEntity<Map<String, Object>> importTelemetryFromJson(
            @RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "File is empty"
                ));
            }

            if (!file.getOriginalFilename().endsWith(".json")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "File must be a JSON file"
                ));
            }

            DataImportService.ImportResult result = dataImportService.importTelemetryFromJson(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalRecords", result.getTotalCount());
            response.put("successCount", result.getSuccessCount());
            response.put("failureCount", result.getFailureCount());
            response.put("errors", result.getErrors());

            if (result.getFailureCount() > 0) {
                response.put("message", String.format(
                    "Imported with partial success: %d succeeded, %d failed",
                    result.getSuccessCount(), result.getFailureCount()
                ));
            } else {
                response.put("message", String.format(
                    "Successfully imported %d records",
                    result.getSuccessCount()
                ));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error importing JSON file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Import failed: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/telemetry/csv/template")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Download CSV template",
               description = "Get a sample CSV template for telemetry import")
    public ResponseEntity<String> downloadCsvTemplate() {
        String template = "deviceId,timestamp,kwConsumption,voltage,current,powerFactor,frequency\n" +
                         "meter-001,2024-01-01T12:00:00Z,50.5,220.1,0.57,0.92,50.02\n" +
                         "meter-002,2024-01-01T12:00:00Z,45.3,219.8,0.51,0.90,49.98\n";

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=telemetry_import_template.csv")
                .body(template);
    }

    @GetMapping("/telemetry/json/template")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Download JSON template",
               description = "Get a sample JSON template for telemetry import")
    public ResponseEntity<String> downloadJsonTemplate() {
        String template = "[\n" +
                         "  {\n" +
                         "    \"deviceId\": \"meter-001\",\n" +
                         "    \"timestamp\": \"2024-01-01T12:00:00Z\",\n" +
                         "    \"variables\": {\n" +
                         "      \"kwConsumption\": 50.5,\n" +
                         "      \"voltage\": 220.1,\n" +
                         "      \"current\": 0.57,\n" +
                         "      \"powerFactor\": 0.92,\n" +
                         "      \"frequency\": 50.02\n" +
                         "    }\n" +
                         "  },\n" +
                         "  {\n" +
                         "    \"deviceId\": \"meter-002\",\n" +
                         "    \"timestamp\": \"2024-01-01T12:00:00Z\",\n" +
                         "    \"variables\": {\n" +
                         "      \"kwConsumption\": 45.3,\n" +
                         "      \"voltage\": 219.8,\n" +
                         "      \"current\": 0.51,\n" +
                         "      \"powerFactor\": 0.90,\n" +
                         "      \"frequency\": 49.98\n" +
                         "    }\n" +
                         "  }\n" +
                         "]";

        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .header("Content-Disposition", "attachment; filename=telemetry_import_template.json")
                .body(template);
    }

    @PostMapping(value = "/devices/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Import devices from CSV",
               description = "Bulk import devices from CSV file. " +
                             "Format: externalId,name,location,sensorType,firmwareVersion,status,latitude,longitude,altitude " +
                             "Example: meter-001,Smart Meter 001,Building A,SMART_METER,v2.1.0,ACTIVE,40.7128,-74.0060,10.5")
    public ResponseEntity<Map<String, Object>> importDevicesFromCsv(
            @RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "File is empty"
                ));
            }

            if (!file.getOriginalFilename().endsWith(".csv")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "File must be a CSV file"
                ));
            }

            DataImportService.ImportResult result = dataImportService.importDevicesFromCsv(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalRecords", result.getTotalCount());
            response.put("successCount", result.getSuccessCount());
            response.put("failureCount", result.getFailureCount());
            response.put("errors", result.getErrors());

            if (result.getFailureCount() > 0) {
                response.put("message", String.format(
                    "Imported with partial success: %d succeeded, %d failed",
                    result.getSuccessCount(), result.getFailureCount()
                ));
            } else {
                response.put("message", String.format(
                    "Successfully imported %d devices",
                    result.getSuccessCount()
                ));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error importing devices from CSV file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Import failed: " + e.getMessage()
            ));
        }
    }

    @PostMapping(value = "/devices/json", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Import devices from JSON",
               description = "Bulk import devices from JSON file. " +
                             "Format: array of { externalId, name, location, sensorType, firmwareVersion, status, latitude, longitude, altitude }")
    public ResponseEntity<Map<String, Object>> importDevicesFromJson(
            @RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "File is empty"
                ));
            }

            if (file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".json")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "File must be a JSON file"
                ));
            }

            DataImportService.ImportResult result = dataImportService.importDevicesFromJson(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalRecords", result.getTotalCount());
            response.put("successCount", result.getSuccessCount());
            response.put("failureCount", result.getFailureCount());
            response.put("errors", result.getErrors());

            if (result.getFailureCount() > 0) {
                response.put("message", String.format(
                    "Imported with partial success: %d succeeded, %d failed",
                    result.getSuccessCount(), result.getFailureCount()
                ));
            } else {
                response.put("message", String.format(
                    "Successfully imported %d devices",
                    result.getSuccessCount()
                ));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error importing devices from JSON file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Import failed: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/devices/csv/template")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Download device CSV template",
               description = "Get a sample CSV template for device import")
    public ResponseEntity<String> downloadDeviceCsvTemplate() {
        String template = "externalId,name,location,sensorType,firmwareVersion,status,latitude,longitude,altitude\n" +
                         "meter-001,Smart Meter 001,Building A Room 101,SMART_METER,v2.1.0,ACTIVE,40.7128,-74.0060,10.5\n" +
                         "meter-002,Smart Meter 002,Building B Room 205,SMART_METER,v2.1.0,ACTIVE,40.7589,-73.9851,15.2\n";

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=device_import_template.csv")
                .body(template);
    }

    @GetMapping("/devices/json/template")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Download device JSON template",
               description = "Get a sample JSON template for device import")
    public ResponseEntity<String> downloadDeviceJsonTemplate() {
        String template = "[\n" +
                         "  {\n" +
                         "    \"externalId\": \"meter-001\",\n" +
                         "    \"name\": \"Smart Meter 001\",\n" +
                         "    \"location\": \"Building A Room 101\",\n" +
                         "    \"sensorType\": \"SMART_METER\",\n" +
                         "    \"firmwareVersion\": \"v2.1.0\",\n" +
                         "    \"status\": \"ACTIVE\",\n" +
                         "    \"latitude\": 40.7128,\n" +
                         "    \"longitude\": -74.0060,\n" +
                         "    \"altitude\": 10.5\n" +
                         "  },\n" +
                         "  {\n" +
                         "    \"externalId\": \"meter-002\",\n" +
                         "    \"name\": \"Smart Meter 002\",\n" +
                         "    \"location\": \"Building B Room 205\",\n" +
                         "    \"sensorType\": \"SMART_METER\",\n" +
                         "    \"firmwareVersion\": \"v2.1.0\",\n" +
                         "    \"status\": \"ACTIVE\",\n" +
                         "    \"latitude\": 40.7589,\n" +
                         "    \"longitude\": -73.9851,\n" +
                         "    \"altitude\": 15.2\n" +
                         "  }\n" +
                         "]";

        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .header("Content-Disposition", "attachment; filename=device_import_template.json")
                .body(template);
    }
}
