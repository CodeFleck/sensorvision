package org.sensorvision.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.LogEntryDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Service for streaming logs from various sources:
 * - Backend application logs (file-based)
 * - Docker container logs (mosquitto, postgres)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogStreamingService {

    private final LogSanitizationService sanitizationService;

    @Value("${log-viewer.backend-log-path:logs/sensorvision.log}")
    private String backendLogPath;

    @Value("${log-viewer.mosquitto-container:sensorvision-mosquitto}")
    private String mosquittoContainer;

    @Value("${log-viewer.postgres-container:sensorvision-postgres}")
    private String postgresContainer;

    @Value("${log-viewer.tail-interval-ms:500}")
    private long tailIntervalMs;

    private DockerClient dockerClient;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, Long> filePositions = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> activeFileTails = new ConcurrentHashMap<>();
    private final Map<String, Closeable> activeDockerStreams = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            // Initialize Docker client
            DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost("unix:///var/run/docker.sock")
                    .build();

            ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .connectionTimeout(Duration.ofSeconds(30))
                    .responseTimeout(Duration.ofSeconds(45))
                    .build();

            dockerClient = DockerClientImpl.getInstance(config, httpClient);
            log.info("Docker client initialized successfully");
        } catch (Exception e) {
            log.warn("Failed to initialize Docker client - container logs will be unavailable: {}", e.getMessage());
            dockerClient = null;
        }
    }

    @PreDestroy
    public void shutdown() {
        // Stop all active tails
        activeFileTails.values().forEach(future -> future.cancel(true));
        activeFileTails.clear();

        // Close all active docker streams
        activeDockerStreams.values().forEach(closeable -> {
            try {
                closeable.close();
            } catch (IOException e) {
                log.debug("Error closing docker stream", e);
            }
        });
        activeDockerStreams.clear();

        scheduler.shutdownNow();

        if (dockerClient != null) {
            try {
                dockerClient.close();
            } catch (IOException e) {
                log.debug("Error closing docker client", e);
            }
        }
    }

    /**
     * Start streaming logs from the specified sources.
     *
     * @param sources  the log sources to stream ("backend", "mosquitto", "postgres")
     * @param sessionId unique identifier for this streaming session
     * @param callback callback to receive log entries
     */
    public void startStreaming(Set<String> sources, String sessionId, Consumer<LogEntryDto> callback) {
        for (String source : sources) {
            switch (source.toLowerCase()) {
                case "backend" -> startBackendLogTail(sessionId, callback);
                case "mosquitto" -> startContainerLogStream(sessionId, mosquittoContainer, "mosquitto", callback);
                case "postgres" -> startContainerLogStream(sessionId, postgresContainer, "postgres", callback);
                default -> log.warn("Unknown log source: {}", source);
            }
        }
    }

    /**
     * Stop streaming logs for a session.
     *
     * @param sessionId the session ID to stop streaming for
     */
    public void stopStreaming(String sessionId) {
        // Stop file tails
        ScheduledFuture<?> fileTail = activeFileTails.remove(sessionId + "-backend");
        if (fileTail != null) {
            fileTail.cancel(true);
        }

        // Stop docker streams
        for (String source : new String[]{"mosquitto", "postgres"}) {
            Closeable stream = activeDockerStreams.remove(sessionId + "-" + source);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    log.debug("Error closing docker stream for session {}", sessionId, e);
                }
            }
        }

        // Clean up file position tracking
        filePositions.remove(sessionId + "-backend");
    }

    /**
     * Start tailing the backend log file.
     */
    private void startBackendLogTail(String sessionId, Consumer<LogEntryDto> callback) {
        String key = sessionId + "-backend";
        Path logPath = Paths.get(backendLogPath);

        if (!Files.exists(logPath)) {
            log.warn("Backend log file not found: {}", backendLogPath);
            return;
        }

        // Start from end of file for new sessions
        try {
            long initialPosition = Files.size(logPath);
            filePositions.put(key, initialPosition);
        } catch (IOException e) {
            log.error("Failed to get initial file size", e);
            filePositions.put(key, 0L);
        }

        ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(() -> {
            try {
                tailBackendLog(key, logPath, callback);
            } catch (Exception e) {
                log.error("Error tailing backend log", e);
            }
        }, 0, tailIntervalMs, TimeUnit.MILLISECONDS);

        activeFileTails.put(key, task);
        log.debug("Started backend log tail for session {}", sessionId);
    }

    /**
     * Read new lines from the backend log file.
     */
    private void tailBackendLog(String key, Path logPath, Consumer<LogEntryDto> callback) {
        long currentPosition = filePositions.getOrDefault(key, 0L);

        try (RandomAccessFile raf = new RandomAccessFile(logPath.toFile(), "r")) {
            long fileLength = raf.length();

            // Handle log rotation (file got smaller)
            if (fileLength < currentPosition) {
                currentPosition = 0;
            }

            if (fileLength > currentPosition) {
                raf.seek(currentPosition);

                // Read bytes and decode as UTF-8
                byte[] buffer = new byte[(int) (fileLength - currentPosition)];
                int bytesRead = raf.read(buffer);

                if (bytesRead > 0) {
                    String content = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    String[] lines = content.split("\\R"); // Split on any line ending

                    for (String line : lines) {
                        if (!line.trim().isEmpty()) {
                            // Sanitize the log line
                            String sanitized = sanitizationService.sanitize(line);
                            LogEntryDto entry = LogEntryDto.fromRawLog("backend", sanitized);
                            callback.accept(entry);
                        }
                    }
                }
                filePositions.put(key, raf.getFilePointer());
            }
        } catch (IOException e) {
            log.error("Error reading backend log file", e);
        }
    }

    /**
     * Start streaming logs from a Docker container.
     */
    private void startContainerLogStream(String sessionId, String containerName, String source,
                                         Consumer<LogEntryDto> callback) {
        if (dockerClient == null) {
            log.warn("Docker client not available, cannot stream {} logs", source);
            return;
        }

        String key = sessionId + "-" + source;

        try {
            LogContainerCmd logCmd = dockerClient.logContainerCmd(containerName)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .withTail(100)  // Limit initial history to last 100 lines for performance
                    .withTimestamps(true);

            ResultCallback.Adapter<Frame> resultCallback = new ResultCallback.Adapter<>() {
                @Override
                public void onNext(Frame frame) {
                    try {
                        String logLine = new String(frame.getPayload(), StandardCharsets.UTF_8).trim();
                        if (!logLine.isEmpty()) {
                            // Sanitize the log line
                            String sanitized = sanitizationService.sanitize(logLine);
                            LogEntryDto entry = createLogEntry(source, sanitized);
                            callback.accept(entry);
                        }
                    } catch (Exception e) {
                        log.debug("Error processing docker log frame", e);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    log.error("Error in {} log stream: {}", source, throwable.getMessage());
                }
            };

            logCmd.exec(resultCallback);
            activeDockerStreams.put(key, resultCallback);
            log.debug("Started {} container log stream for session {}", source, sessionId);

        } catch (Exception e) {
            log.error("Failed to start {} container log stream: {}", source, e.getMessage());
        }
    }

    /**
     * Create a LogEntryDto from a container log line.
     */
    private LogEntryDto createLogEntry(String source, String logLine) {
        return switch (source) {
            case "mosquitto" -> LogEntryDto.fromMosquittoLog(logLine);
            case "postgres" -> LogEntryDto.fromPostgresLog(logLine);
            default -> LogEntryDto.fromRawLog(source, logLine);
        };
    }

    /**
     * Get recent historical logs (last N lines) from the backend log file.
     *
     * @param maxLines maximum number of lines to return
     * @return list of sanitized log entries
     */
    public java.util.List<LogEntryDto> getRecentBackendLogs(int maxLines) {
        java.util.List<LogEntryDto> logs = new java.util.ArrayList<>();
        Path logPath = Paths.get(backendLogPath);

        if (!Files.exists(logPath)) {
            return logs;
        }

        try {
            java.util.List<String> allLines = Files.readAllLines(logPath);
            int startIndex = Math.max(0, allLines.size() - maxLines);

            for (int i = startIndex; i < allLines.size(); i++) {
                String line = allLines.get(i);
                if (!line.trim().isEmpty()) {
                    String sanitized = sanitizationService.sanitize(line);
                    logs.add(LogEntryDto.fromRawLog("backend", sanitized));
                }
            }
        } catch (IOException e) {
            log.error("Failed to read recent backend logs", e);
        }

        return logs;
    }

    /**
     * Check if Docker client is available.
     */
    public boolean isDockerAvailable() {
        return dockerClient != null;
    }
}
