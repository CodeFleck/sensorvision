package io.indcloud.service.ml;

import io.indcloud.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MLTrainingJobMonitor.
 * Tests the background polling service for training job status synchronization.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MLTrainingJobMonitor Tests")
class MLTrainingJobMonitorTest {

    @Mock
    private MLTrainingJobService trainingJobService;

    private MLTrainingJobMonitor monitor;

    private Organization testOrg;
    private MLModel testModel;
    private MLTrainingJob runningJob;
    private UUID jobId;
    private Long orgId = 1L;

    @BeforeEach
    void setUp() {
        monitor = new MLTrainingJobMonitor(trainingJobService);
        ReflectionTestUtils.setField(monitor, "staleThresholdHours", 24);

        testOrg = new Organization();
        testOrg.setId(orgId);

        UUID modelId = UUID.randomUUID();
        testModel = MLModel.builder()
                .id(modelId)
                .organization(testOrg)
                .name("Test Model")
                .modelType(MLModelType.ANOMALY_DETECTION)
                .status(MLModelStatus.TRAINING)
                .build();

        jobId = UUID.randomUUID();
        runningJob = MLTrainingJob.builder()
                .id(jobId)
                .model(testModel)
                .organization(testOrg)
                .jobType(MLTrainingJobType.INITIAL_TRAINING)
                .status(MLTrainingJobStatus.RUNNING)
                .progressPercent(25)
                .externalJobId(UUID.randomUUID())
                .startedAt(Instant.now().minus(Duration.ofMinutes(10)))
                .createdAt(Instant.now().minus(Duration.ofMinutes(15)))
                .build();
    }

    @Nested
    @DisplayName("pollActiveJobs Tests")
    class PollActiveJobsTests {

        @Test
        @DisplayName("Should poll all active jobs")
        void shouldPollAllActiveJobs() {
            MLTrainingJob pendingJob = MLTrainingJob.builder()
                    .id(UUID.randomUUID())
                    .model(testModel)
                    .organization(testOrg)
                    .status(MLTrainingJobStatus.PENDING)
                    .externalJobId(UUID.randomUUID())
                    .createdAt(Instant.now())
                    .build();

            when(trainingJobService.getActiveJobs())
                    .thenReturn(List.of(runningJob, pendingJob));
            when(trainingJobService.syncJobStatus(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            monitor.pollActiveJobs();

            // Should sync both jobs
            verify(trainingJobService, times(2)).syncJobStatus(any());
            verify(trainingJobService).syncJobStatus(runningJob);
            verify(trainingJobService).syncJobStatus(pendingJob);
        }

        @Test
        @DisplayName("Should skip polling when no active jobs")
        void shouldSkipPollingWhenNoActiveJobs() {
            when(trainingJobService.getActiveJobs())
                    .thenReturn(Collections.emptyList());

            monitor.pollActiveJobs();

            verify(trainingJobService).getActiveJobs();
            verify(trainingJobService, never()).syncJobStatus(any());
        }

        @Test
        @DisplayName("Should continue polling other jobs even if one fails")
        void shouldContinuePollingOnIndividualJobError() {
            MLTrainingJob secondJob = MLTrainingJob.builder()
                    .id(UUID.randomUUID())
                    .model(testModel)
                    .organization(testOrg)
                    .status(MLTrainingJobStatus.RUNNING)
                    .externalJobId(UUID.randomUUID())
                    .startedAt(Instant.now())
                    .createdAt(Instant.now())
                    .build();

            when(trainingJobService.getActiveJobs())
                    .thenReturn(List.of(runningJob, secondJob));

            // First job throws error
            when(trainingJobService.syncJobStatus(runningJob))
                    .thenThrow(new RuntimeException("Network error"));
            // Second job succeeds
            when(trainingJobService.syncJobStatus(secondJob))
                    .thenReturn(secondJob);

            // Should not throw
            monitor.pollActiveJobs();

            // Both jobs should be attempted
            verify(trainingJobService).syncJobStatus(runningJob);
            verify(trainingJobService).syncJobStatus(secondJob);
        }

        @Test
        @DisplayName("Should prevent concurrent poll execution")
        void shouldPreventConcurrentPollExecution() throws InterruptedException {
            AtomicInteger concurrentCalls = new AtomicInteger(0);
            AtomicInteger maxConcurrent = new AtomicInteger(0);

            when(trainingJobService.getActiveJobs()).thenAnswer(inv -> {
                int current = concurrentCalls.incrementAndGet();
                maxConcurrent.updateAndGet(max -> Math.max(max, current));

                // Simulate slow operation
                Thread.sleep(100);

                concurrentCalls.decrementAndGet();
                return List.of(runningJob);
            });
            when(trainingJobService.syncJobStatus(any()))
                    .thenReturn(runningJob);

            ExecutorService executor = Executors.newFixedThreadPool(3);
            CountDownLatch latch = new CountDownLatch(3);

            for (int i = 0; i < 3; i++) {
                executor.submit(() -> {
                    monitor.pollActiveJobs();
                    latch.countDown();
                });
            }

            latch.await(2, TimeUnit.SECONDS);
            executor.shutdown();

            // Only one should execute at a time
            assertThat(maxConcurrent.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should release lock even on exception")
        void shouldReleaseLockOnException() {
            when(trainingJobService.getActiveJobs())
                    .thenThrow(new RuntimeException("Database error"));

            // First call fails
            try {
                monitor.pollActiveJobs();
            } catch (Exception ignored) {}

            // Reset mock to succeed
            reset(trainingJobService);
            when(trainingJobService.getActiveJobs())
                    .thenReturn(List.of(runningJob));
            when(trainingJobService.syncJobStatus(any()))
                    .thenReturn(runningJob);

            // Second call should work (lock was released)
            monitor.pollActiveJobs();

            verify(trainingJobService).syncJobStatus(runningJob);
        }
    }

    @Nested
    @DisplayName("Stale Job Detection Tests")
    class StaleJobDetectionTests {

        @Test
        @DisplayName("Should detect stale running job based on startedAt")
        void shouldDetectStaleJobByStartedAt() {
            // Job running for 25 hours (threshold is 24)
            runningJob.setStartedAt(Instant.now().minus(Duration.ofHours(25)));
            runningJob.setStatus(MLTrainingJobStatus.RUNNING);

            when(trainingJobService.getActiveJobs())
                    .thenReturn(List.of(runningJob));

            monitor.pollActiveJobs();

            // Should attempt to cancel stale job
            verify(trainingJobService).cancelJob(jobId, orgId);
            // Should NOT sync status for stale job
            verify(trainingJobService, never()).syncJobStatus(any());
        }

        @Test
        @DisplayName("Should use createdAt if startedAt is null for stale detection")
        void shouldUseCreatedAtIfStartedAtNull() {
            runningJob.setStartedAt(null);
            runningJob.setCreatedAt(Instant.now().minus(Duration.ofHours(25)));
            runningJob.setStatus(MLTrainingJobStatus.RUNNING);

            when(trainingJobService.getActiveJobs())
                    .thenReturn(List.of(runningJob));

            monitor.pollActiveJobs();

            verify(trainingJobService).cancelJob(jobId, orgId);
        }

        @Test
        @DisplayName("Should not consider PENDING jobs as stale")
        void shouldNotConsiderPendingJobsAsStale() {
            runningJob.setStatus(MLTrainingJobStatus.PENDING);
            runningJob.setCreatedAt(Instant.now().minus(Duration.ofHours(25)));

            when(trainingJobService.getActiveJobs())
                    .thenReturn(List.of(runningJob));
            when(trainingJobService.syncJobStatus(any()))
                    .thenReturn(runningJob);

            monitor.pollActiveJobs();

            // Should sync, not cancel
            verify(trainingJobService).syncJobStatus(runningJob);
            verify(trainingJobService, never()).cancelJob(any(), any());
        }

        @Test
        @DisplayName("Should not consider recently started jobs as stale")
        void shouldNotConsiderRecentJobsAsStale() {
            runningJob.setStartedAt(Instant.now().minus(Duration.ofHours(1)));
            runningJob.setStatus(MLTrainingJobStatus.RUNNING);

            when(trainingJobService.getActiveJobs())
                    .thenReturn(List.of(runningJob));
            when(trainingJobService.syncJobStatus(any()))
                    .thenReturn(runningJob);

            monitor.pollActiveJobs();

            // Should sync normally
            verify(trainingJobService).syncJobStatus(runningJob);
            verify(trainingJobService, never()).cancelJob(any(), any());
        }

        @Test
        @DisplayName("Should handle cancel failure gracefully for stale jobs")
        void shouldHandleCancelFailureGracefully() {
            runningJob.setStartedAt(Instant.now().minus(Duration.ofHours(25)));
            runningJob.setStatus(MLTrainingJobStatus.RUNNING);

            when(trainingJobService.getActiveJobs())
                    .thenReturn(List.of(runningJob));
            when(trainingJobService.cancelJob(jobId, orgId))
                    .thenThrow(new RuntimeException("Cancel failed"));

            // Should not throw
            monitor.pollActiveJobs();

            verify(trainingJobService).cancelJob(jobId, orgId);
        }

        @Test
        @DisplayName("Should respect configurable stale threshold")
        void shouldRespectConfigurableStaleThreshold() {
            // Set shorter threshold for this test
            ReflectionTestUtils.setField(monitor, "staleThresholdHours", 1);

            // Job running for 2 hours
            runningJob.setStartedAt(Instant.now().minus(Duration.ofHours(2)));
            runningJob.setStatus(MLTrainingJobStatus.RUNNING);

            when(trainingJobService.getActiveJobs())
                    .thenReturn(List.of(runningJob));

            monitor.pollActiveJobs();

            // Should be stale with 1-hour threshold
            verify(trainingJobService).cancelJob(jobId, orgId);
        }
    }

    @Nested
    @DisplayName("Status Change Logging Tests")
    class StatusChangeLoggingTests {

        @Test
        @DisplayName("Should detect and process status changes")
        void shouldDetectAndProcessStatusChanges() {
            MLTrainingJob completedJob = MLTrainingJob.builder()
                    .id(jobId)
                    .model(testModel)
                    .organization(testOrg)
                    .status(MLTrainingJobStatus.COMPLETED)
                    .durationSeconds(3600)
                    .build();

            when(trainingJobService.getActiveJobs())
                    .thenReturn(List.of(runningJob));
            when(trainingJobService.syncJobStatus(runningJob))
                    .thenReturn(completedJob);

            monitor.pollActiveJobs();

            verify(trainingJobService).syncJobStatus(runningJob);
            // Status changed from RUNNING to COMPLETED - verify logging occurred
            // (actual log verification would require logback test appender)
        }
    }

    @Nested
    @DisplayName("Manual Trigger Tests")
    class ManualTriggerTests {

        @Test
        @DisplayName("Should allow manual poll trigger")
        void shouldAllowManualPollTrigger() {
            when(trainingJobService.getActiveJobs())
                    .thenReturn(List.of(runningJob));
            when(trainingJobService.syncJobStatus(any()))
                    .thenReturn(runningJob);

            monitor.triggerPoll();

            verify(trainingJobService).getActiveJobs();
            verify(trainingJobService).syncJobStatus(runningJob);
        }

        @Test
        @DisplayName("Should report polling status correctly")
        void shouldReportPollingStatusCorrectly() {
            // Initially not polling
            assertThat(monitor.isPollingInProgress()).isFalse();

            when(trainingJobService.getActiveJobs()).thenAnswer(inv -> {
                // Check status during poll
                assertThat(monitor.isPollingInProgress()).isTrue();
                return Collections.emptyList();
            });

            monitor.pollActiveJobs();

            // After poll
            assertThat(monitor.isPollingInProgress()).isFalse();
        }
    }

    @Nested
    @DisplayName("Integration Behavior Tests")
    class IntegrationBehaviorTests {

        @Test
        @DisplayName("Should handle multiple jobs with mixed statuses")
        void shouldHandleMultipleJobsWithMixedStatuses() {
            MLTrainingJob pendingJob = MLTrainingJob.builder()
                    .id(UUID.randomUUID())
                    .model(testModel)
                    .organization(testOrg)
                    .status(MLTrainingJobStatus.PENDING)
                    .externalJobId(UUID.randomUUID())
                    .createdAt(Instant.now())
                    .build();

            // Stale job
            MLTrainingJob staleJob = MLTrainingJob.builder()
                    .id(UUID.randomUUID())
                    .model(testModel)
                    .organization(testOrg)
                    .status(MLTrainingJobStatus.RUNNING)
                    .externalJobId(UUID.randomUUID())
                    .startedAt(Instant.now().minus(Duration.ofHours(25)))
                    .createdAt(Instant.now().minus(Duration.ofHours(25)))
                    .build();

            when(trainingJobService.getActiveJobs())
                    .thenReturn(List.of(runningJob, pendingJob, staleJob));

            // Normal jobs succeed
            when(trainingJobService.syncJobStatus(runningJob))
                    .thenReturn(runningJob);
            when(trainingJobService.syncJobStatus(pendingJob))
                    .thenReturn(pendingJob);

            monitor.pollActiveJobs();

            // Running and pending should be synced
            verify(trainingJobService).syncJobStatus(runningJob);
            verify(trainingJobService).syncJobStatus(pendingJob);

            // Stale should be cancelled
            verify(trainingJobService).cancelJob(staleJob.getId(), orgId);
        }

        @Test
        @DisplayName("Should handle empty external job ID")
        void shouldHandleEmptyExternalJobId() {
            runningJob.setExternalJobId(null);

            when(trainingJobService.getActiveJobs())
                    .thenReturn(List.of(runningJob));
            when(trainingJobService.syncJobStatus(runningJob))
                    .thenReturn(runningJob);

            monitor.pollActiveJobs();

            // Should still call sync (service handles null external ID)
            verify(trainingJobService).syncJobStatus(runningJob);
        }
    }
}
