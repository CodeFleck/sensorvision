package org.sensorvision.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.TelemetryRecord;
import org.sensorvision.repository.TelemetryRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance batch processor for telemetry data in the pilot program.
 * Optimizes database writes through batching and async processing.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PilotTelemetryBatchProcessor {

    private final TelemetryRecordRepository telemetryRepository;
    private final PilotPerformanceService performanceService;

    @Value("${pilot.telemetry.batch-size:100}")
    private int batchSize;

    @Value("${pilot.telemetry.batch-timeout:5000}")
    private long batchTimeoutMs;

    @Value("${pilot.telemetry.max-concurrent-batches:5}")
    private int maxConcurrentBatches;

    private final ConcurrentLinkedQueue<TelemetryRecord> pendingRecords = new ConcurrentLinkedQueue<>();
    private final AtomicInteger activeBatches = new AtomicInteger(0);
    private volatile Instant lastBatchTime = Instant.now();

    /**
     * Add telemetry record to batch processing queue
     */
    public void addTelemetryRecord(TelemetryRecord record) {
        pendingRecords.offer(record);
        
        // Trigger batch processing if conditions are met
        if (shouldProcessBatch()) {
            processBatchAsync();
        }
    }

    /**
     * Force processing of all pending records
     */
    public CompletableFuture<Void> flushPendingRecords() {
        log.info("Flushing {} pending telemetry records", pendingRecords.size());
        return processBatchAsync();
    }

    /**
     * Check if batch should be processed
     */
    private boolean shouldProcessBatch() {
        int pendingCount = pendingRecords.size();
        long timeSinceLastBatch = Instant.now().toEpochMilli() - lastBatchTime.toEpochMilli();
        int currentActiveBatches = activeBatches.get();
        
        return (pendingCount >= batchSize || 
                (pendingCount > 0 && timeSinceLastBatch >= batchTimeoutMs)) &&
                currentActiveBatches < maxConcurrentBatches;
    }

    /**
     * Process batch asynchronously
     */
    @Async("telemetryProcessingExecutor")
    public CompletableFuture<Void> processBatchAsync() {
        if (activeBatches.incrementAndGet() > maxConcurrentBatches) {
            activeBatches.decrementAndGet();
            return CompletableFuture.completedFuture(null);
        }

        try {
            List<TelemetryRecord> batch = extractBatch();
            if (!batch.isEmpty()) {
                processBatch(batch);
            }
        } catch (Exception e) {
            log.error("Error processing telemetry batch", e);
            performanceService.recordError("telemetry-batch-processing", e.getMessage());
        } finally {
            activeBatches.decrementAndGet();
            lastBatchTime = Instant.now();
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Extract batch of records from queue
     */
    private List<TelemetryRecord> extractBatch() {
        List<TelemetryRecord> batch = new ArrayList<>();
        
        for (int i = 0; i < batchSize && !pendingRecords.isEmpty(); i++) {
            TelemetryRecord record = pendingRecords.poll();
            if (record != null) {
                batch.add(record);
            }
        }
        
        return batch;
    }

    /**
     * Process batch of telemetry records
     */
    @Transactional
    public void processBatch(List<TelemetryRecord> batch) {
        if (batch.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        
        try {
            // Batch insert for optimal performance
            telemetryRepository.saveAll(batch);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Record performance metrics
            performanceService.recordTelemetryBatch(batch.size(), executionTime);
            
            log.debug("Processed telemetry batch: {} records in {}ms", batch.size(), executionTime);
            
            // Log performance warning if batch is slow
            if (executionTime > 1000) {
                log.warn("Slow telemetry batch processing: {} records took {}ms", batch.size(), executionTime);
            }
            
        } catch (Exception e) {
            log.error("Failed to process telemetry batch of {} records", batch.size(), e);
            performanceService.recordError("telemetry-batch-save", e.getMessage());
            throw e;
        }
    }

    /**
     * Get current batch processing statistics
     */
    public BatchStatistics getBatchStatistics() {
        return BatchStatistics.builder()
                .pendingRecords(pendingRecords.size())
                .activeBatches(activeBatches.get())
                .batchSize(batchSize)
                .batchTimeoutMs(batchTimeoutMs)
                .maxConcurrentBatches(maxConcurrentBatches)
                .lastBatchTime(lastBatchTime)
                .build();
    }

    /**
     * Batch processing statistics
     */
    @lombok.Builder
    @lombok.Data
    public static class BatchStatistics {
        private int pendingRecords;
        private int activeBatches;
        private int batchSize;
        private long batchTimeoutMs;
        private int maxConcurrentBatches;
        private Instant lastBatchTime;
    }
}