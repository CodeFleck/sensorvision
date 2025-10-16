package org.sensorvision.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.ScheduledReportRequest;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.*;
import org.sensorvision.repository.ReportExecutionRepository;
import org.sensorvision.repository.ScheduledReportRepository;
import org.sensorvision.security.SecurityUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledReportService {

    private final ScheduledReportRepository scheduledReportRepository;
    private final ReportExecutionRepository reportExecutionRepository;
    private final ReportGenerationService reportGenerationService;
    private final EmailNotificationService emailNotificationService;

    @Transactional
    public ScheduledReport createScheduledReport(ScheduledReportRequest request) {
        User currentUser = SecurityUtils.getCurrentUser();
        Organization organization = currentUser.getOrganization();

        ScheduledReport report = ScheduledReport.builder()
                .name(request.getName())
                .description(request.getDescription())
                .organization(organization)
                .createdBy(currentUser)
                .reportType(request.getReportType())
                .exportFormat(request.getExportFormat())
                .scheduleFrequency(request.getScheduleFrequency())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .emailRecipients(request.getEmailRecipients())
                .reportParameters(request.getReportParameters())
                .nextRunAt(calculateNextRunTime(request.getScheduleFrequency()))
                .build();

        ScheduledReport saved = scheduledReportRepository.save(report);
        log.info("Created scheduled report: {} for organization: {}", saved.getName(), organization.getName());
        return saved;
    }

    @Transactional
    public ScheduledReport updateScheduledReport(Long reportId, ScheduledReportRequest request) {
        Organization currentOrg = SecurityUtils.getCurrentUser().getOrganization();

        ScheduledReport report = scheduledReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled report not found: " + reportId));

        // Verify ownership
        if (!report.getOrganization().getId().equals(currentOrg.getId())) {
            throw new IllegalArgumentException("Cannot modify report from different organization");
        }

        report.setName(request.getName());
        report.setDescription(request.getDescription());
        report.setReportType(request.getReportType());
        report.setExportFormat(request.getExportFormat());
        report.setScheduleFrequency(request.getScheduleFrequency());
        report.setEnabled(request.getEnabled() != null ? request.getEnabled() : report.getEnabled());
        report.setEmailRecipients(request.getEmailRecipients());
        report.setReportParameters(request.getReportParameters());

        // Recalculate next run time if frequency changed
        report.setNextRunAt(calculateNextRunTime(request.getScheduleFrequency()));

        return scheduledReportRepository.save(report);
    }

    @Transactional
    public void deleteScheduledReport(Long reportId) {
        Organization currentOrg = SecurityUtils.getCurrentUser().getOrganization();

        ScheduledReport report = scheduledReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled report not found: " + reportId));

        if (!report.getOrganization().getId().equals(currentOrg.getId())) {
            throw new IllegalArgumentException("Cannot delete report from different organization");
        }

        scheduledReportRepository.delete(report);
        log.info("Deleted scheduled report: {}", reportId);
    }

    @Transactional(readOnly = true)
    public List<ScheduledReport> getScheduledReports() {
        Organization currentOrg = SecurityUtils.getCurrentUser().getOrganization();
        return scheduledReportRepository.findByOrganization(currentOrg);
    }

    @Transactional(readOnly = true)
    public ScheduledReport getScheduledReport(Long reportId) {
        Organization currentOrg = SecurityUtils.getCurrentUser().getOrganization();

        ScheduledReport report = scheduledReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled report not found: " + reportId));

        if (!report.getOrganization().getId().equals(currentOrg.getId())) {
            throw new IllegalArgumentException("Cannot access report from different organization");
        }

        return report;
    }

    /**
     * Scheduled task that runs every hour to check for due reports
     */
    @Scheduled(cron = "0 0 * * * *") // Run at the start of every hour
    @Transactional
    public void executeScheduledReports() {
        log.info("Checking for due scheduled reports...");

        List<ScheduledReport> dueReports = scheduledReportRepository.findDueReports(LocalDateTime.now());

        if (dueReports.isEmpty()) {
            log.debug("No reports due for execution");
            return;
        }

        log.info("Found {} reports to execute", dueReports.size());

        for (ScheduledReport report : dueReports) {
            try {
                executeReportAsync(report);
            } catch (Exception e) {
                log.error("Error scheduling report execution for report: {}", report.getId(), e);
            }
        }
    }

    @Async
    @Transactional
    public void executeReportAsync(ScheduledReport report) {
        log.info("Executing scheduled report: {} (ID: {})", report.getName(), report.getId());

        ReportExecution execution = ReportExecution.builder()
                .scheduledReport(report)
                .startedAt(LocalDateTime.now())
                .status(ReportExecution.ExecutionStatus.RUNNING)
                .build();

        execution = reportExecutionRepository.save(execution);

        try {
            // Generate the report
            byte[] reportData = reportGenerationService.generateReport(report);

            // In a production system, you would upload this to S3 or similar storage
            // For now, we'll just log the size and mark as completed
            execution.setFileSizeBytes((long) reportData.length);
            execution.setStatus(ReportExecution.ExecutionStatus.COMPLETED);
            execution.setCompletedAt(LocalDateTime.now());

            // Count records based on report type
            execution.setRecordCount(estimateRecordCount(reportData, report.getExportFormat()));

            // Send email if recipients are configured
            if (report.getEmailRecipients() != null && !report.getEmailRecipients().isEmpty()) {
                sendReportEmail(report, reportData, execution);
            }

            // Update report's last run and next run times
            report.setLastRunAt(execution.getStartedAt());
            report.setNextRunAt(calculateNextRunTime(report.getScheduleFrequency()));
            scheduledReportRepository.save(report);

            log.info("Successfully executed report: {} (Size: {} bytes, Records: {})",
                    report.getName(), execution.getFileSizeBytes(), execution.getRecordCount());

        } catch (Exception e) {
            log.error("Failed to execute report: {}", report.getId(), e);
            execution.setStatus(ReportExecution.ExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setCompletedAt(LocalDateTime.now());
        }

        reportExecutionRepository.save(execution);
    }

    /**
     * Manually trigger a report execution
     */
    @Transactional
    public ReportExecution triggerReportExecution(Long reportId) {
        Organization currentOrg = SecurityUtils.getCurrentUser().getOrganization();

        ScheduledReport report = scheduledReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled report not found: " + reportId));

        if (!report.getOrganization().getId().equals(currentOrg.getId())) {
            throw new IllegalArgumentException("Cannot execute report from different organization");
        }

        log.info("Manually triggering report execution: {}", reportId);
        executeReportAsync(report);

        // Return a placeholder execution record (the actual execution is async)
        return ReportExecution.builder()
                .scheduledReport(report)
                .startedAt(LocalDateTime.now())
                .status(ReportExecution.ExecutionStatus.RUNNING)
                .build();
    }

    private void sendReportEmail(ScheduledReport report, byte[] reportData, ReportExecution execution) {
        try {
            String[] recipients = report.getEmailRecipients().split(",");
            String fileName = generateFileName(report);

            for (String recipient : recipients) {
                recipient = recipient.trim();
                if (!recipient.isEmpty()) {
                    emailNotificationService.sendReportEmail(
                            recipient,
                            report.getName(),
                            report.getDescription(),
                            fileName,
                            reportData,
                            execution
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed to send report email for report: {}", report.getId(), e);
        }
    }

    private String generateFileName(ScheduledReport report) {
        String timestamp = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = reportGenerationService.getFileExtension(report.getExportFormat());
        return String.format("%s_%s%s", report.getName().replaceAll("\\s+", "_"), timestamp, extension);
    }

    private LocalDateTime calculateNextRunTime(ScheduledReport.ScheduleFrequency frequency) {
        LocalDateTime now = LocalDateTime.now();
        return switch (frequency) {
            case DAILY -> now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case WEEKLY -> now.plusWeeks(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            case MONTHLY -> now.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        };
    }

    private int estimateRecordCount(byte[] data, ScheduledReport.ExportFormat format) {
        if (format == ScheduledReport.ExportFormat.CSV) {
            // Count newlines as rough estimate
            int count = 0;
            for (byte b : data) {
                if (b == '\n') count++;
            }
            return Math.max(0, count - 1); // Subtract header row
        }
        return 0; // For JSON and Excel, would need more sophisticated parsing
    }
}
