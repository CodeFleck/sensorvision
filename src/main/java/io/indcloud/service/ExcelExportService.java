package io.indcloud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import io.indcloud.dto.DeviceExportRequest;
import io.indcloud.dto.EventExportRequest;
import io.indcloud.dto.TelemetryExportRequest;
import io.indcloud.exception.ResourceNotFoundException;
import io.indcloud.model.*;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.repository.EventRepository;
import io.indcloud.repository.TelemetryRecordRepository;
import io.indcloud.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExcelExportService {

    private final TelemetryRecordRepository telemetryRepository;
    private final DeviceRepository deviceRepository;
    private final EventRepository eventRepository;
    private final SecurityUtils securityUtils;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] exportTelemetryData(TelemetryExportRequest request) throws IOException {
        Organization org = securityUtils.getCurrentUserOrganization();

        // Verify device access
        Device device = deviceRepository.findById(request.deviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + request.deviceId()));

        if (!device.getOrganization().getId().equals(org.getId())) {
            throw new ResourceNotFoundException("Device not found: " + request.deviceId());
        }

        // Fetch telemetry data
        Pageable pageable = PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<TelemetryRecord> records;

        if (request.startTime() != null && request.endTime() != null) {
            records = telemetryRepository.findByDeviceIdAndTimestampBetween(
                    request.deviceId(),
                    request.startTime(),
                    request.endTime(),
                    pageable
            ).getContent();
        } else {
            records = telemetryRepository.findByDeviceId(request.deviceId(), pageable).getContent();
        }

        return createTelemetryExcel(records, device);
    }

    public byte[] exportDevices(DeviceExportRequest request) throws IOException {
        Organization org = securityUtils.getCurrentUserOrganization();

        List<Device> devices = deviceRepository.findByOrganizationId(org.getId());

        // Apply status filter if provided
        if (request.status() != null) {
            devices = devices.stream()
                    .filter(d -> d.getStatus() == request.status())
                    .collect(Collectors.toList());
        }

        return createDevicesExcel(devices);
    }

    public byte[] exportEvents(EventExportRequest request) throws IOException {
        Organization org = securityUtils.getCurrentUserOrganization();

        Pageable pageable = PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<Event> events;

        if (request.startTime() != null && request.endTime() != null) {
            events = eventRepository.findByOrganizationAndCreatedAtBetweenOrderByCreatedAtDesc(
                    org,
                    request.startTime(),
                    request.endTime(),
                    pageable
            ).getContent();
        } else {
            events = eventRepository.findByOrganizationOrderByCreatedAtDesc(org, pageable).getContent();
        }

        // Apply filters
        if (request.eventType() != null) {
            events = events.stream()
                    .filter(e -> e.getEventType() == request.eventType())
                    .collect(Collectors.toList());
        }

        if (request.severity() != null) {
            events = events.stream()
                    .filter(e -> e.getSeverity() == request.severity())
                    .collect(Collectors.toList());
        }

        return createEventsExcel(events);
    }

    private byte[] createTelemetryExcel(List<TelemetryRecord> records, Device device) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Telemetry Data");

            // Create header style
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Timestamp", "Device ID", "Device Name", "KW Consumption", "Voltage", "Current", "Power Factor", "Latitude", "Longitude", "Altitude"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fill data rows
            int rowNum = 1;
            for (TelemetryRecord record : records) {
                Row row = sheet.createRow(rowNum++);

                Cell timestampCell = row.createCell(0);
                timestampCell.setCellValue(record.getTimestamp().toString());
                timestampCell.setCellStyle(dateStyle);

                row.createCell(1).setCellValue(device.getId().toString());
                row.createCell(2).setCellValue(device.getName());
                row.createCell(3).setCellValue(record.getKwConsumption() != null ? record.getKwConsumption().doubleValue() : 0.0);
                row.createCell(4).setCellValue(record.getVoltage() != null ? record.getVoltage().doubleValue() : 0.0);
                row.createCell(5).setCellValue(record.getCurrent() != null ? record.getCurrent().doubleValue() : 0.0);
                row.createCell(6).setCellValue(record.getPowerFactor() != null ? record.getPowerFactor().doubleValue() : 0.0);
                row.createCell(7).setCellValue(record.getLatitude() != null ? record.getLatitude().doubleValue() : 0);
                row.createCell(8).setCellValue(record.getLongitude() != null ? record.getLongitude().doubleValue() : 0);
                row.createCell(9).setCellValue(record.getAltitude() != null ? record.getAltitude().doubleValue() : 0);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            log.info("Exported {} telemetry records for device {}", records.size(), device.getId());
            return out.toByteArray();
        }
    }

    private byte[] createDevicesExcel(List<Device> devices) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Devices");

            // Create header style
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Device ID", "Name", "Type", "Status", "Latitude", "Longitude", "Altitude", "Created At", "Updated At"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fill data rows
            int rowNum = 1;
            for (Device device : devices) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(device.getId().toString());
                row.createCell(1).setCellValue(device.getName());
                row.createCell(2).setCellValue(device.getSensorType() != null ? device.getSensorType() : "");
                row.createCell(3).setCellValue(device.getStatus().name());
                row.createCell(4).setCellValue(device.getLatitude() != null ? device.getLatitude().doubleValue() : 0);
                row.createCell(5).setCellValue(device.getLongitude() != null ? device.getLongitude().doubleValue() : 0);
                row.createCell(6).setCellValue(device.getAltitude() != null ? device.getAltitude().doubleValue() : 0);

                Cell createdAtCell = row.createCell(7);
                createdAtCell.setCellValue(device.getCreatedAt().toString());
                createdAtCell.setCellStyle(dateStyle);

                Cell updatedAtCell = row.createCell(8);
                updatedAtCell.setCellValue(device.getUpdatedAt().toString());
                updatedAtCell.setCellStyle(dateStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            log.info("Exported {} devices", devices.size());
            return out.toByteArray();
        }
    }

    private byte[] createEventsExcel(List<Event> events) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Events");

            // Create header style
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Timestamp", "Event Type", "Severity", "Title", "Description", "Device ID"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fill data rows
            int rowNum = 1;
            for (Event event : events) {
                Row row = sheet.createRow(rowNum++);

                Cell timestampCell = row.createCell(0);
                timestampCell.setCellValue(event.getCreatedAt().format(DATE_TIME_FORMATTER));
                timestampCell.setCellStyle(dateStyle);

                row.createCell(1).setCellValue(event.getEventType().name());
                row.createCell(2).setCellValue(event.getSeverity().name());
                row.createCell(3).setCellValue(event.getTitle());
                row.createCell(4).setCellValue(event.getDescription() != null ? event.getDescription() : "");
                row.createCell(5).setCellValue(event.getDeviceId() != null ? event.getDeviceId() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            log.info("Exported {} events", events.size());
            return out.toByteArray();
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
        return style;
    }
}
