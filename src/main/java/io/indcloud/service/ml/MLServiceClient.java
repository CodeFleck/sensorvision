package io.indcloud.service.ml;

import io.indcloud.dto.ml.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Client for communicating with the Python ML microservice.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MLServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    @Value("${ml.service.timeout:30}")
    private int timeoutSeconds;

    private WebClient getWebClient() {
        return webClientBuilder
                .baseUrl(mlServiceUrl)
                .build();
    }

    /**
     * Check ML service health.
     */
    public Mono<Boolean> isHealthy() {
        return getWebClient()
                .get()
                .uri("/health")
                .retrieve()
                .bodyToMono(Void.class)
                .map(v -> true)
                .onErrorReturn(false)
                .timeout(Duration.ofSeconds(5));
    }

    /**
     * Run anomaly detection on telemetry data.
     */
    public Mono<AnomalyDetectionResultDto> detectAnomaly(InferenceRequestDto request) {
        log.debug("Requesting anomaly detection for device {}", request.getDeviceId());

        return getWebClient()
                .post()
                .uri("/api/v1/inference/anomaly")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AnomalyDetectionResultDto.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryable))
                .doOnError(e -> log.error("Anomaly detection failed for device {}: {}",
                        request.getDeviceId(), e.getMessage()));
    }

    /**
     * Run batch anomaly detection for multiple devices.
     */
    public Mono<BatchInferenceResponseDto> detectAnomaliesBatch(BatchInferenceRequestDto request) {
        log.info("Requesting batch anomaly detection for {} devices", request.getDeviceIds().size());

        return getWebClient()
                .post()
                .uri("/api/v1/inference/anomaly/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(BatchInferenceResponseDto.class)
                .timeout(Duration.ofSeconds(timeoutSeconds * 2))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
                        .filter(this::isRetryable))
                .doOnError(e -> log.error("Batch anomaly detection failed: {}", e.getMessage()));
    }

    /**
     * Predict maintenance needs for a device.
     */
    public Mono<PredictiveMaintenanceResultDto> predictMaintenance(InferenceRequestDto request) {
        log.debug("Requesting maintenance prediction for device {}", request.getDeviceId());

        return getWebClient()
                .post()
                .uri("/api/v1/inference/maintenance")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PredictiveMaintenanceResultDto.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryable));
    }

    /**
     * Forecast energy consumption.
     */
    public Mono<InferenceResponseDto> forecastEnergy(InferenceRequestDto request, String horizon) {
        log.debug("Requesting energy forecast for device {}, horizon={}", request.getDeviceId(), horizon);

        return getWebClient()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/inference/energy")
                        .queryParam("horizon", horizon)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(InferenceResponseDto.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryable));
    }

    /**
     * Estimate Remaining Useful Life.
     */
    public Mono<InferenceResponseDto> estimateRUL(InferenceRequestDto request) {
        log.debug("Requesting RUL estimation for device {}", request.getDeviceId());

        return getWebClient()
                .post()
                .uri("/api/v1/inference/rul")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(InferenceResponseDto.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryable));
    }

    /**
     * Create a training job.
     */
    public Mono<TrainingJobResponseDto> createTrainingJob(TrainingJobCreateDto request) {
        log.info("Creating training job for model {}", request.getModelId());

        return getWebClient()
                .post()
                .uri("/api/v1/training/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(TrainingJobResponseDto.class)
                .timeout(Duration.ofSeconds(timeoutSeconds));
    }

    /**
     * Get training job status.
     */
    public Mono<TrainingJobResponseDto> getTrainingJob(UUID jobId) {
        return getWebClient()
                .get()
                .uri("/api/v1/training/jobs/{jobId}", jobId)
                .retrieve()
                .bodyToMono(TrainingJobResponseDto.class)
                .timeout(Duration.ofSeconds(timeoutSeconds));
    }

    /**
     * Cancel a training job.
     */
    public Mono<TrainingJobResponseDto> cancelTrainingJob(UUID jobId) {
        log.info("Cancelling training job {}", jobId);

        return getWebClient()
                .post()
                .uri("/api/v1/training/jobs/{jobId}/cancel", jobId)
                .retrieve()
                .bodyToMono(TrainingJobResponseDto.class)
                .timeout(Duration.ofSeconds(timeoutSeconds));
    }

    /**
     * List ML models.
     */
    public Mono<List<MLModelResponseDto>> listModels(Long organizationId) {
        return getWebClient()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/models")
                        .queryParam("organization_id", organizationId)
                        .build())
                .retrieve()
                .bodyToFlux(MLModelResponseDto.class)
                .collectList()
                .timeout(Duration.ofSeconds(timeoutSeconds));
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            int status = ex.getStatusCode().value();
            // Don't retry client errors (4xx) except 429 (rate limit)
            return status >= 500 || status == 429;
        }
        // Retry network errors
        return true;
    }
}
