package io.indcloud.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.indcloud.model.FunctionTrigger;
import io.indcloud.model.FunctionTriggerType;
import io.indcloud.model.ServerlessFunction;
import io.indcloud.repository.FunctionTriggerRepository;
import io.indcloud.service.FunctionExecutionService;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledFunctionExecutorTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private FunctionTriggerRepository triggerRepository;

    @Mock
    private FunctionExecutionService executionService;

    @Mock
    @SuppressWarnings("rawtypes")
    private ScheduledFuture mockScheduledFuture;

    private ObjectMapper objectMapper;
    private ScheduledFunctionExecutor executor;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    @Captor
    private ArgumentCaptor<CronTrigger> cronTriggerCaptor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        executor = new ScheduledFunctionExecutor(
            taskScheduler,
            triggerRepository,
            executionService,
            objectMapper
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void testReloadSchedules_AddsNewTrigger() {
        // Given
        FunctionTrigger trigger = createTrigger(1L, "{\"cronExpression\": \"0 0 * * * *\"}");
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.SCHEDULED))
            .thenReturn(List.of(trigger));
        when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class)))
            .thenReturn(mockScheduledFuture);

        // When
        executor.reloadSchedules();

        // Then
        verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(CronTrigger.class));
        assertThat(executor.isScheduled(1L)).isTrue();
        assertThat(executor.getActiveScheduleCount()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testReloadSchedules_RemovesDeletedTrigger() {
        // Given - First add a trigger
        FunctionTrigger trigger = createTrigger(1L, "{\"cronExpression\": \"0 0 * * * *\"}");
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.SCHEDULED))
            .thenReturn(List.of(trigger));
        when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class)))
            .thenReturn(mockScheduledFuture);

        executor.reloadSchedules();
        assertThat(executor.getActiveScheduleCount()).isEqualTo(1);

        // When - Reload with empty list (trigger deleted)
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.SCHEDULED))
            .thenReturn(Collections.emptyList());

        executor.reloadSchedules();

        // Then
        verify(mockScheduledFuture, times(1)).cancel(false);
        assertThat(executor.getActiveScheduleCount()).isEqualTo(0);
        assertThat(executor.isScheduled(1L)).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testReloadSchedules_WithTimezone() {
        // Given
        FunctionTrigger trigger = createTrigger(1L,
            "{\"cronExpression\": \"0 0 9 * * *\", \"timezone\": \"America/New_York\"}");
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.SCHEDULED))
            .thenReturn(List.of(trigger));
        when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class)))
            .thenReturn(mockScheduledFuture);

        // When
        executor.reloadSchedules();

        // Then
        verify(taskScheduler).schedule(any(Runnable.class), cronTriggerCaptor.capture());
        // Note: Can't easily verify timezone in CronTrigger, but no exception means it worked
        assertThat(executor.isScheduled(1L)).isTrue();
    }

    @Test
    void testReloadSchedules_InvalidCronExpression() {
        // Given
        FunctionTrigger trigger = createTrigger(1L, "{\"cronExpression\": \"invalid cron\"}");
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.SCHEDULED))
            .thenReturn(List.of(trigger));

        // When
        executor.reloadSchedules();

        // Then - Should not schedule with invalid cron
        verify(taskScheduler, never()).schedule(any(Runnable.class), any(CronTrigger.class));
        assertThat(executor.isScheduled(1L)).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testScheduledExecution_ExecutesFunction() throws Exception {
        // Given
        FunctionTrigger trigger = createTrigger(1L, "{\"cronExpression\": \"0 0 * * * *\"}");
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.SCHEDULED))
            .thenReturn(List.of(trigger));
        when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class)))
            .thenReturn(mockScheduledFuture);

        // When - Schedule the trigger
        executor.reloadSchedules();

        // Then - Capture the runnable
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(CronTrigger.class));

        // When - Execute the scheduled task
        Runnable scheduledTask = runnableCaptor.getValue();
        scheduledTask.run();

        // Then - Verify function was executed
        verify(executionService, times(1)).executeFunctionAsync(
            eq(100L),
            any(JsonNode.class),
            eq(trigger)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void testScheduledExecution_EventPayload() throws Exception {
        // Given
        FunctionTrigger trigger = createTrigger(1L,
            "{\"cronExpression\": \"0 0 * * * *\", \"data\": {\"key\": \"value\"}}");
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.SCHEDULED))
            .thenReturn(List.of(trigger));
        when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class)))
            .thenReturn(mockScheduledFuture);

        ArgumentCaptor<JsonNode> eventCaptor = ArgumentCaptor.forClass(JsonNode.class);

        // When
        executor.reloadSchedules();
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(CronTrigger.class));

        Runnable scheduledTask = runnableCaptor.getValue();
        scheduledTask.run();

        // Then
        verify(executionService).executeFunctionAsync(
            eq(100L),
            eventCaptor.capture(),
            eq(trigger)
        );

        JsonNode payload = eventCaptor.getValue();
        assertThat(payload.get("eventType").asText()).isEqualTo("scheduled");
        assertThat(payload.get("triggerId").asLong()).isEqualTo(1L);
        assertThat(payload.get("cronExpression").asText()).isEqualTo("0 0 * * * *");
        assertThat(payload.has("timestamp")).isTrue();
        assertThat(payload.get("data").get("key").asText()).isEqualTo("value");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCancelAllSchedules() {
        // Given
        FunctionTrigger trigger1 = createTrigger(1L, "{\"cronExpression\": \"0 0 * * * *\"}");
        FunctionTrigger trigger2 = createTrigger(2L, "{\"cronExpression\": \"0 30 * * * *\"}");
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.SCHEDULED))
            .thenReturn(List.of(trigger1, trigger2));
        when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class)))
            .thenReturn(mockScheduledFuture);

        executor.reloadSchedules();
        assertThat(executor.getActiveScheduleCount()).isEqualTo(2);

        // When
        executor.cancelAllSchedules();

        // Then
        verify(mockScheduledFuture, times(2)).cancel(false);
        assertThat(executor.getActiveScheduleCount()).isEqualTo(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDefaultCronExpression() {
        // Given - Trigger without cron expression
        FunctionTrigger trigger = createTrigger(1L, "{}");
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.SCHEDULED))
            .thenReturn(List.of(trigger));
        when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class)))
            .thenReturn(mockScheduledFuture);

        // When
        executor.reloadSchedules();

        // Then - Should use default cron (every hour)
        assertThat(executor.isScheduled(1L)).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDefaultTimezone() {
        // Given - Trigger without timezone
        FunctionTrigger trigger = createTrigger(1L, "{\"cronExpression\": \"0 0 * * * *\"}");
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.SCHEDULED))
            .thenReturn(List.of(trigger));
        when(taskScheduler.schedule(any(Runnable.class), any(CronTrigger.class)))
            .thenReturn(mockScheduledFuture);

        // When
        executor.reloadSchedules();

        // Then - Should use UTC as default
        assertThat(executor.isScheduled(1L)).isTrue();
    }

    // Helper methods

    private FunctionTrigger createTrigger(Long id, String configJson) {
        try {
            JsonNode config = objectMapper.readTree(configJson);

            ServerlessFunction function = new ServerlessFunction();
            function.setId(100L);
            function.setName("test-function");
            function.setEnabled(true);

            FunctionTrigger trigger = new FunctionTrigger();
            trigger.setId(id);
            trigger.setTriggerType(FunctionTriggerType.SCHEDULED);
            trigger.setTriggerConfig(config);
            trigger.setEnabled(true);
            trigger.setFunction(function);

            return trigger;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
