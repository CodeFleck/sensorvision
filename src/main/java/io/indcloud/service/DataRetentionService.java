package io.indcloud.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.archive.ArchiveStorageAdapter;
import io.indcloud.archive.ArchiveStorageAdapterFactory;
import io.indcloud.model.*;
import io.indcloud.repository.DataArchiveExecutionRepository;
import io.indcloud.repository.DataRetentionPolicyRepository;
import io.indcloud.repository.TelemetryRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataRetentionService {

    private final DataRetentionPolicyRepository policyRepository;
    private final DataArchiveExecutionRepository executionRepository;
    private final TelemetryRecordRepository telemetryRepository;
    private final ArchiveStorageAdapterFactory storageAdapterFactory;

    /**
     * Execute archival for a specific policy
     */
    @Transactional
    public DataArchiveExecution executeArchival(DataRetentionPolicy policy) {
        log.info("Starting archival execution for organization {} (policy ID: {})",
                policy.getOrganization().getId(), policy.getId());

        DataArchiveExecution execution = new DataArchiveExecution();
        execution.setPolicy(policy);
        execution.setOrganization(policy.getOrganization());
        execution.setStatus(ArchiveExecutionStatus.RUNNING);
        execution.setStartedAt(Instant.now());

        // Calculate date range for archival
        Instant cutoffDate = Instant.now().minus(policy.getRetentionDays(), ChronoUnit.DAYS);
        LocalDate cutoffLocalDate = LocalDate.ofInstant(cutoffDate, ZoneOffset.UTC);

        execution.setArchiveFromDate(Instant.EPOCH); // Archive from beginning
        execution.setArchiveToDate(cutoffDate);

        execution = executionRepository.save(execution);

        try {
            // Find telemetry records older than retention period that haven't been archived
            List<TelemetryRecord> recordsToArchive = telemetryRepository
                    .findByOrganizationIdAndTimestampBeforeAndArchivedFalse(
                            policy.getOrganization().getId(),
                            cutoffDate
                    );

            log.info("Found {} records to archive for organization {}",
                    recordsToArchive.size(), policy.getOrganization().getId());

            if (recordsToArchive.isEmpty()) {
                execution.setStatus(ArchiveExecutionStatus.SUCCESS);
                execution.setRecordsArchived(0);
                execution.setArchiveSizeBytes(0L);
                execution.setCompletedAt(Instant.now());

                policy.setLastArchiveRun(Instant.now());
                policy.setLastArchiveStatus(ArchiveExecutionStatus.SUCCESS);
                policy.setLastArchiveError(null);
                policyRepository.save(policy);

                log.info("No records to archive, execution completed successfully");
                return executionRepository.save(execution);
            }

            if (!policy.getArchiveEnabled()) {
                // Just delete the old records without archiving
                log.info("Archive disabled, deleting {} old records", recordsToArchive.size());
                telemetryRepository.deleteAll(recordsToArchive);

                execution.setStatus(ArchiveExecutionStatus.SUCCESS);
                execution.setRecordsArchived(recordsToArchive.size());
                execution.setArchiveSizeBytes(0L);
                execution.setCompletedAt(Instant.now());

                policy.setLastArchiveRun(Instant.now());
                policy.setLastArchiveStatus(ArchiveExecutionStatus.SUCCESS);
                policy.setLastArchiveError(null);
                policyRepository.save(policy);

                return executionRepository.save(execution);
            }

            // Store archive using configured storage adapter
            ArchiveStorageAdapter storageAdapter = storageAdapterFactory.getAdapter(
                    policy.getArchiveStorageType()
            );

            String archivePath = storageAdapter.storeArchive(
                    recordsToArchive,
                    policy.getOrganization().getId(),
                    cutoffLocalDate.format(DateTimeFormatter.ISO_DATE),
                    policy.getArchiveStorageConfig()
            );

            long archiveSize = storageAdapter.getArchiveSize(
                    archivePath,
                    policy.getArchiveStorageConfig()
            );

            // Mark records as archived
            for (TelemetryRecord record : recordsToArchive) {
                record.setArchived(true);
            }
            telemetryRepository.saveAll(recordsToArchive);

            // Update execution record
            execution.setStatus(ArchiveExecutionStatus.SUCCESS);
            execution.setRecordsArchived(recordsToArchive.size());
            execution.setArchiveFilePath(archivePath);
            execution.setArchiveSizeBytes(archiveSize);
            execution.setCompletedAt(Instant.now());

            // Update policy statistics
            policy.setLastArchiveRun(Instant.now());
            policy.setLastArchiveStatus(ArchiveExecutionStatus.SUCCESS);
            policy.setLastArchiveError(null);
            policy.setTotalRecordsArchived(
                    policy.getTotalRecordsArchived() + recordsToArchive.size()
            );
            policy.setTotalArchiveSizeBytes(
                    policy.getTotalArchiveSizeBytes() + archiveSize
            );
            policyRepository.save(policy);

            log.info("Archival completed successfully: {} records, {} bytes, path: {}",
                    recordsToArchive.size(), archiveSize, archivePath);

            return executionRepository.save(execution);

        } catch (Exception e) {
            log.error("Archival execution failed for policy ID {}: {}",
                    policy.getId(), e.getMessage(), e);

            execution.setStatus(ArchiveExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setStackTrace(getStackTrace(e));
            execution.setCompletedAt(Instant.now());

            policy.setLastArchiveRun(Instant.now());
            policy.setLastArchiveStatus(ArchiveExecutionStatus.FAILED);
            policy.setLastArchiveError(e.getMessage());
            policyRepository.save(policy);

            return executionRepository.save(execution);
        }
    }

    /**
     * Execute archival for all enabled policies
     */
    public void executeAllArchival() {
        List<DataRetentionPolicy> enabledPolicies = policyRepository.findAllEnabled();

        log.info("Executing archival for {} enabled policies", enabledPolicies.size());

        for (DataRetentionPolicy policy : enabledPolicies) {
            try {
                executeArchival(policy);
            } catch (Exception e) {
                log.error("Failed to execute archival for policy ID {}: {}",
                        policy.getId(), e.getMessage(), e);
            }
        }

        log.info("Archival execution completed for all policies");
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
