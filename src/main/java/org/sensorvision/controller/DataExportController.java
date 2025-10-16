package org.sensorvision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.DeviceExportRequest;
import org.sensorvision.dto.EventExportRequest;
import org.sensorvision.dto.TelemetryExportRequest;
import org.sensorvision.dto.TelemetryPointDto;
import org.sensorvision.model.TelemetryRecord;
import org.sensorvision.repository.TelemetryRecordRepository;
import org.sensorvision.service.ExcelExportService;
import org.sensorvision.service.TelemetryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
@Tag(name = "Data Export", description = "APIs for exporting data in various formats (CSV, JSON, Excel)")
@PreAuthorize("isAuthenticated()")
public class DataExportController {

    private final TelemetryRecordRepository telemetryRecordRepository;
    private final ExcelExportService excelExportService;
    private final TelemetryService telemetryService;

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Export telemetry data as CSV
     */
    @GetMapping("/csv/{deviceId}")
    public ResponseEntity<String> exportCsv(
            @PathVariable String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        // Use TelemetryService which includes organization checks
        List<TelemetryPointDto> points = telemetryService.queryTelemetry(deviceId, from, to);

        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,Device ID,kW Consumption,Voltage,Current,Power Factor,Frequency\n");

        for (TelemetryPointDto point : points) {
            csv.append(DateTimeFormatter.ISO_INSTANT.format(point.timestamp())).append(",")
               .append(deviceId).append(",")
               .append(point.kwConsumption() != null ? point.kwConsumption() : "").append(",")
               .append(point.voltage() != null ? point.voltage() : "").append(",")
               .append(point.current() != null ? point.current() : "").append(",")
               .append(point.powerFactor() != null ? point.powerFactor() : "").append(",")
               .append(point.frequency() != null ? point.frequency() : "")
               .append("\n");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", deviceId + "_telemetry.csv");

        log.info("Exported {} telemetry records as CSV for device {}", points.size(), deviceId);

        return ResponseEntity.ok()
                .headers(headers)
                .body(csv.toString());
    }

    /**
     * Export telemetry data as JSON
     */
    @GetMapping("/json/{deviceId}")
    public ResponseEntity<List<TelemetryPointDto>> exportJson(
            @PathVariable String deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        // Use TelemetryService which includes organization checks
        List<TelemetryPointDto> points = telemetryService.queryTelemetry(deviceId, from, to);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", deviceId + "_telemetry.json");

        log.info("Exported {} telemetry records as JSON for device {}", points.size(), deviceId);

        return ResponseEntity.ok()
                .headers(headers)
                .body(points);
    }

    /**
     * Export telemetry data to Excel
     */
    @PostMapping("/telemetry/excel")
    @Operation(summary = "Export telemetry data to Excel", description = "Export device telemetry data to .xlsx format")
    public ResponseEntity<byte[]> exportTelemetryToExcel(@RequestBody TelemetryExportRequest request) throws IOException {
        log.info("Exporting telemetry data for device: {}", request.deviceId());

        byte[] excelData = excelExportService.exportTelemetryData(request);

        String filename = String.format("telemetry_%s_%s.xlsx",
                request.deviceId(),
                LocalDateTime.now().format(FILE_DATE_FORMAT));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(excelData.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    /**
     * Export devices to Excel
     */
    @PostMapping("/devices/excel")
    @Operation(summary = "Export devices to Excel", description = "Export device list to .xlsx format")
    public ResponseEntity<byte[]> exportDevicesToExcel(@RequestBody(required = false) DeviceExportRequest request) throws IOException {
        log.info("Exporting devices data");

        if (request == null) {
            request = new DeviceExportRequest(null, null, "excel");
        }

        byte[] excelData = excelExportService.exportDevices(request);

        String filename = String.format("devices_%s.xlsx",
                LocalDateTime.now().format(FILE_DATE_FORMAT));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(excelData.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    /**
     * Export events to Excel
     */
    @PostMapping("/events/excel")
    @Operation(summary = "Export events to Excel", description = "Export event logs to .xlsx format")
    public ResponseEntity<byte[]> exportEventsToExcel(@RequestBody(required = false) EventExportRequest request) throws IOException {
        log.info("Exporting events data");

        if (request == null) {
            request = new EventExportRequest(null, null, null, null, "excel");
        }

        byte[] excelData = excelExportService.exportEvents(request);

        String filename = String.format("events_%s.xlsx",
                LocalDateTime.now().format(FILE_DATE_FORMAT));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(excelData.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }
}
