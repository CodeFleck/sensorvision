package org.sensorvision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sensorvision.dto.ReportExecutionResponse;
import org.sensorvision.dto.ScheduledReportRequest;
import org.sensorvision.dto.ScheduledReportResponse;
import org.sensorvision.model.ReportExecution;
import org.sensorvision.model.ScheduledReport;
import org.sensorvision.repository.ReportExecutionRepository;
import org.sensorvision.service.ScheduledReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Scheduled Reports", description = "Scheduled report generation and management")
@SecurityRequirement(name = "Bearer Authentication")
public class ScheduledReportController {

    private final ScheduledReportService scheduledReportService;
    private final ReportExecutionRepository reportExecutionRepository;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get all scheduled reports for the current organization")
    public ResponseEntity<List<ScheduledReportResponse>> getScheduledReports() {
        List<ScheduledReport> reports = scheduledReportService.getScheduledReports();
        List<ScheduledReportResponse> response = reports.stream()
                .map(ScheduledReportResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get a specific scheduled report by ID")
    public ResponseEntity<ScheduledReportResponse> getScheduledReport(@PathVariable Long id) {
        ScheduledReport report = scheduledReportService.getScheduledReport(id);
        return ResponseEntity.ok(ScheduledReportResponse.fromEntity(report));
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create a new scheduled report")
    public ResponseEntity<ScheduledReportResponse> createScheduledReport(
            @Valid @RequestBody ScheduledReportRequest request) {
        ScheduledReport report = scheduledReportService.createScheduledReport(request);
        return ResponseEntity.ok(ScheduledReportResponse.fromEntity(report));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update an existing scheduled report")
    public ResponseEntity<ScheduledReportResponse> updateScheduledReport(
            @PathVariable Long id,
            @Valid @RequestBody ScheduledReportRequest request) {
        ScheduledReport report = scheduledReportService.updateScheduledReport(id, request);
        return ResponseEntity.ok(ScheduledReportResponse.fromEntity(report));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Delete a scheduled report")
    public ResponseEntity<Void> deleteScheduledReport(@PathVariable Long id) {
        scheduledReportService.deleteScheduledReport(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Manually trigger a report execution")
    public ResponseEntity<ReportExecutionResponse> triggerReportExecution(@PathVariable Long id) {
        ReportExecution execution = scheduledReportService.triggerReportExecution(id);
        return ResponseEntity.ok(ReportExecutionResponse.fromEntity(execution));
    }

    @GetMapping("/{id}/executions")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get execution history for a scheduled report")
    public ResponseEntity<Page<ReportExecutionResponse>> getReportExecutions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ScheduledReport report = scheduledReportService.getScheduledReport(id);
        Page<ReportExecution> executions = reportExecutionRepository
                .findByScheduledReportOrderByStartedAtDesc(report, PageRequest.of(page, size));
        Page<ReportExecutionResponse> response = executions.map(ReportExecutionResponse::fromEntity);
        return ResponseEntity.ok(response);
    }
}
