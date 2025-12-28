package io.indcloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.*;
import io.indcloud.repository.AlertRepository;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.repository.TelemetryRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.poi.ss.usermodel.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportGenerationService {

    private final TelemetryRecordRepository telemetryRepository;
    private final DeviceRepository deviceRepository;
    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional(readOnly = true)
    public byte[] generateReport(ScheduledReport report) throws IOException {
        log.info("Generating report: {} for organization: {}",
                report.getName(), report.getOrganization().getId());

        return switch (report.getExportFormat()) {
            case CSV -> generateCsvReport(report);
            case JSON -> generateJsonReport(report);
            case EXCEL -> generateExcelReport(report);
        };
    }

    private byte[] generateCsvReport(ScheduledReport report) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(osw)) {

            switch (report.getReportType()) {
                case TELEMETRY_DATA -> writeTelemetryDataCsv(report, writer);
                case DEVICE_STATUS -> writeDeviceStatusCsv(report, writer);
                case ALERT_SUMMARY -> writeAlertSummaryCsv(report, writer);
                case ANALYTICS_SUMMARY -> writeAnalyticsSummaryCsv(report, writer);
            }

            writer.flush();
            return outputStream.toByteArray();
        }
    }

    private byte[] generateJsonReport(ScheduledReport report) throws IOException {
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("reportName", report.getName());
        reportData.put("reportType", report.getReportType());
        reportData.put("generatedAt", LocalDateTime.now().format(DATE_FORMATTER));
        reportData.put("organization", report.getOrganization().getName());

        List<?> data = switch (report.getReportType()) {
            case TELEMETRY_DATA -> getTelemetryData(report);
            case DEVICE_STATUS -> getDeviceStatusData(report);
            case ALERT_SUMMARY -> getAlertSummaryData(report);
            case ANALYTICS_SUMMARY -> getAnalyticsSummaryData(report);
        };

        reportData.put("data", data);
        reportData.put("recordCount", data.size());

        return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(reportData);
    }

    private byte[] generateExcelReport(ScheduledReport report) throws IOException {
        log.info("Generating Excel report for: {}", report.getName());

        try (Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(report.getReportType().toString());

            // Create header row with styling
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);

            switch (report.getReportType()) {
                case TELEMETRY_DATA -> writeTelemetryDataExcel(report, sheet, headerStyle);
                case DEVICE_STATUS -> writeDeviceStatusExcel(report, sheet, headerStyle);
                case ALERT_SUMMARY -> writeAlertSummaryExcel(report, sheet, headerStyle);
                case ANALYTICS_SUMMARY -> writeAnalyticsSummaryExcel(report, sheet, headerStyle);
            }

            // Auto-size columns
            for (int i = 0; i < sheet.getRow(0).getLastCellNum(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        return style;
    }

    private void writeTelemetryDataExcel(ScheduledReport report, Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.getRow(0);
        String[] headers = {"Timestamp", "Device ID", "Device Name", "KW Consumption", "Voltage", "Current"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        List<TelemetryRecord> records = getTelemetryData(report);
        int rowNum = 1;
        for (TelemetryRecord record : records) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(LocalDateTime.ofInstant(record.getTimestamp(), ZoneId.systemDefault())
                    .format(DATE_FORMATTER));
            row.createCell(1).setCellValue(record.getDevice().getExternalId());
            row.createCell(2).setCellValue(record.getDevice().getName());
            row.createCell(3).setCellValue(record.getKwConsumption() != null ? record.getKwConsumption().doubleValue() : 0.0);
            row.createCell(4).setCellValue(record.getVoltage() != null ? record.getVoltage().doubleValue() : 0.0);
            row.createCell(5).setCellValue(record.getCurrent() != null ? record.getCurrent().doubleValue() : 0.0);
        }
    }

    private void writeDeviceStatusExcel(ScheduledReport report, Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.getRow(0);
        String[] headers = {"Device ID", "Device Name", "Status", "Location", "Sensor Type", "Firmware Version", "Last Seen"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        List<Device> devices = getDeviceStatusData(report);
        int rowNum = 1;
        for (Device device : devices) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(device.getExternalId());
            row.createCell(1).setCellValue(device.getName());
            row.createCell(2).setCellValue(device.getStatus() != null ? device.getStatus().toString() : "");
            row.createCell(3).setCellValue(device.getLocation() != null ? device.getLocation() : "");
            row.createCell(4).setCellValue(device.getSensorType() != null ? device.getSensorType() : "");
            row.createCell(5).setCellValue(device.getFirmwareVersion() != null ? device.getFirmwareVersion() : "");
            if (device.getLastSeenAt() != null) {
                row.createCell(6).setCellValue(LocalDateTime.ofInstant(device.getLastSeenAt(), ZoneId.systemDefault())
                        .format(DATE_FORMATTER));
            } else {
                row.createCell(6).setCellValue("");
            }
        }
    }

    private void writeAlertSummaryExcel(ScheduledReport report, Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.getRow(0);
        String[] headers = {"Triggered At", "Device ID", "Device Name", "Rule Name", "Severity", "Message", "Value", "Acknowledged"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        List<Alert> alerts = getAlertSummaryData(report);
        int rowNum = 1;
        for (Alert alert : alerts) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(alert.getTriggeredAt().format(DATE_FORMATTER));
            row.createCell(1).setCellValue(alert.getDevice().getExternalId());
            row.createCell(2).setCellValue(alert.getDevice().getName());
            row.createCell(3).setCellValue(alert.getRule() != null ? alert.getRule().getName() : "N/A");
            row.createCell(4).setCellValue(alert.getSeverity() != null ? alert.getSeverity().toString() : "");
            row.createCell(5).setCellValue(alert.getMessage());
            row.createCell(6).setCellValue(alert.getTriggeredValue() != null ? alert.getTriggeredValue().doubleValue() : 0.0);
            row.createCell(7).setCellValue(alert.getAcknowledged() != null ? alert.getAcknowledged().toString() : "false");
        }
    }

    private void writeAnalyticsSummaryExcel(ScheduledReport report, Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.getRow(0);
        String[] headers = {"Device ID", "Device Name", "Avg KW", "Max KW", "Min KW", "Total Records"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        List<Device> devices = getDeviceStatusData(report);
        Instant endTime = Instant.now();
        Instant startTime = getStartTimeForFrequencyInstant(report.getScheduleFrequency(), endTime);

        int rowNum = 1;
        for (Device device : devices) {
            List<TelemetryRecord> records = telemetryRepository
                    .findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
                            device.getExternalId(), startTime, endTime);

            if (!records.isEmpty()) {
                double avgKw = records.stream()
                        .filter(r -> r.getKwConsumption() != null)
                        .mapToDouble(r -> r.getKwConsumption().doubleValue())
                        .average()
                        .orElse(0.0);

                double maxKw = records.stream()
                        .filter(r -> r.getKwConsumption() != null)
                        .mapToDouble(r -> r.getKwConsumption().doubleValue())
                        .max()
                        .orElse(0.0);

                double minKw = records.stream()
                        .filter(r -> r.getKwConsumption() != null)
                        .mapToDouble(r -> r.getKwConsumption().doubleValue())
                        .min()
                        .orElse(0.0);

                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(device.getExternalId());
                row.createCell(1).setCellValue(device.getName());
                row.createCell(2).setCellValue(avgKw);
                row.createCell(3).setCellValue(maxKw);
                row.createCell(4).setCellValue(minKw);
                row.createCell(5).setCellValue(records.size());
            }
        }
    }

    private void writeTelemetryDataCsv(ScheduledReport report, PrintWriter writer) {
        writer.println("Timestamp,Device ID,Device Name,KW Consumption,Voltage,Current");

        List<TelemetryRecord> records = getTelemetryData(report);
        for (TelemetryRecord record : records) {
            String timestamp = LocalDateTime.ofInstant(record.getTimestamp(), ZoneId.systemDefault())
                    .format(DATE_FORMATTER);
            writer.printf("%s,%s,%s,%.2f,%.2f,%.2f%n",
                    timestamp,
                    record.getDevice().getExternalId(),
                    record.getDevice().getName(),
                    record.getKwConsumption() != null ? record.getKwConsumption().doubleValue() : 0.0,
                    record.getVoltage() != null ? record.getVoltage().doubleValue() : 0.0,
                    record.getCurrent() != null ? record.getCurrent().doubleValue() : 0.0
            );
        }
    }

    private void writeDeviceStatusCsv(ScheduledReport report, PrintWriter writer) {
        writer.println("Device ID,Device Name,Status,Location,Sensor Type,Firmware Version,Last Seen");

        List<Device> devices = getDeviceStatusData(report);
        for (Device device : devices) {
            String lastSeen = "";
            if (device.getLastSeenAt() != null) {
                lastSeen = LocalDateTime.ofInstant(device.getLastSeenAt(), ZoneId.systemDefault())
                        .format(DATE_FORMATTER);
            }
            writer.printf("%s,%s,%s,%s,%s,%s,%s%n",
                    device.getExternalId(),
                    device.getName(),
                    device.getStatus(),
                    device.getLocation() != null ? device.getLocation() : "",
                    device.getSensorType() != null ? device.getSensorType() : "",
                    device.getFirmwareVersion() != null ? device.getFirmwareVersion() : "",
                    lastSeen
            );
        }
    }

    private void writeAlertSummaryCsv(ScheduledReport report, PrintWriter writer) {
        writer.println("Triggered At,Device ID,Device Name,Rule Name,Severity,Message,Value,Acknowledged");

        List<Alert> alerts = getAlertSummaryData(report);
        for (Alert alert : alerts) {
            writer.printf("%s,%s,%s,%s,%s,%s,%.2f,%s%n",
                    alert.getTriggeredAt().format(DATE_FORMATTER),
                    alert.getDevice().getExternalId(),
                    alert.getDevice().getName(),
                    alert.getRule() != null ? alert.getRule().getName() : "N/A",
                    alert.getSeverity(),
                    escapeQuotes(alert.getMessage()),
                    alert.getTriggeredValue() != null ? alert.getTriggeredValue().doubleValue() : 0.0,
                    alert.getAcknowledged()
            );
        }
    }

    private void writeAnalyticsSummaryCsv(ScheduledReport report, PrintWriter writer) {
        writer.println("Device ID,Device Name,Avg KW,Max KW,Min KW,Total Records");

        List<Device> devices = getDeviceStatusData(report);
        Instant endTime = Instant.now();
        Instant startTime = getStartTimeForFrequencyInstant(report.getScheduleFrequency(), endTime);

        for (Device device : devices) {
            List<TelemetryRecord> records = telemetryRepository
                    .findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
                            device.getExternalId(), startTime, endTime);

            if (!records.isEmpty()) {
                double avgKw = records.stream()
                        .filter(r -> r.getKwConsumption() != null)
                        .mapToDouble(r -> r.getKwConsumption().doubleValue())
                        .average()
                        .orElse(0.0);

                double maxKw = records.stream()
                        .filter(r -> r.getKwConsumption() != null)
                        .mapToDouble(r -> r.getKwConsumption().doubleValue())
                        .max()
                        .orElse(0.0);

                double minKw = records.stream()
                        .filter(r -> r.getKwConsumption() != null)
                        .mapToDouble(r -> r.getKwConsumption().doubleValue())
                        .min()
                        .orElse(0.0);

                writer.printf("%s,%s,%.2f,%.2f,%.2f,%d%n",
                        device.getExternalId(),
                        device.getName(),
                        avgKw,
                        maxKw,
                        minKw,
                        records.size()
                );
            }
        }
    }

    private List<TelemetryRecord> getTelemetryData(ScheduledReport report) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = getStartTimeForFrequency(report.getScheduleFrequency(), endTime);

        return telemetryRepository.findByDeviceOrganizationAndTimestampBetween(
                report.getOrganization(),
                startTime,
                endTime
        );
    }

    private List<Device> getDeviceStatusData(ScheduledReport report) {
        return deviceRepository.findByOrganization(report.getOrganization());
    }

    private List<Alert> getAlertSummaryData(ScheduledReport report) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = getStartTimeForFrequency(report.getScheduleFrequency(), endTime);

        return alertRepository.findByDeviceOrganizationAndTriggeredAtBetween(
                report.getOrganization(),
                startTime,
                endTime
        );
    }

    private List<Map<String, Object>> getAnalyticsSummaryData(ScheduledReport report) {
        List<Device> devices = getDeviceStatusData(report);
        Instant endTime = Instant.now();
        Instant startTime = getStartTimeForFrequencyInstant(report.getScheduleFrequency(), endTime);

        return devices.stream()
                .map(device -> {
                    List<TelemetryRecord> records = telemetryRepository
                            .findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
                                    device.getExternalId(), startTime, endTime);

                    Map<String, Object> deviceAnalytics = new HashMap<>();
                    deviceAnalytics.put("deviceId", device.getExternalId());
                    deviceAnalytics.put("deviceName", device.getName());
                    deviceAnalytics.put("recordCount", records.size());

                    if (!records.isEmpty()) {
                        deviceAnalytics.put("avgKw", records.stream()
                                .filter(r -> r.getKwConsumption() != null)
                                .mapToDouble(r -> r.getKwConsumption().doubleValue())
                                .average()
                                .orElse(0.0));
                        deviceAnalytics.put("maxKw", records.stream()
                                .filter(r -> r.getKwConsumption() != null)
                                .mapToDouble(r -> r.getKwConsumption().doubleValue())
                                .max()
                                .orElse(0.0));
                        deviceAnalytics.put("minKw", records.stream()
                                .filter(r -> r.getKwConsumption() != null)
                                .mapToDouble(r -> r.getKwConsumption().doubleValue())
                                .min()
                                .orElse(0.0));
                    }

                    return deviceAnalytics;
                })
                .toList();
    }

    private LocalDateTime getStartTimeForFrequency(ScheduledReport.ScheduleFrequency frequency,
                                                     LocalDateTime endTime) {
        return switch (frequency) {
            case DAILY -> endTime.minusDays(1);
            case WEEKLY -> endTime.minusWeeks(1);
            case MONTHLY -> endTime.minusMonths(1);
        };
    }

    private Instant getStartTimeForFrequencyInstant(ScheduledReport.ScheduleFrequency frequency,
                                                      Instant endTime) {
        return switch (frequency) {
            case DAILY -> endTime.minus(1, java.time.temporal.ChronoUnit.DAYS);
            case WEEKLY -> endTime.minus(7, java.time.temporal.ChronoUnit.DAYS);
            case MONTHLY -> endTime.minus(30, java.time.temporal.ChronoUnit.DAYS);
        };
    }

    private String escapeQuotes(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public String getFileExtension(ScheduledReport.ExportFormat format) {
        return switch (format) {
            case CSV -> ".csv";
            case JSON -> ".json";
            case EXCEL -> ".xlsx";
        };
    }
}
