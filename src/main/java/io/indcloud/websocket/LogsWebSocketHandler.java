package io.indcloud.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.dto.LogEntryDto;
import io.indcloud.service.LogStreamingService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket handler for streaming system logs to clients with ROLE_DEVELOPER.
 * Supports subscribing to multiple log sources: backend, mosquitto, postgres.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogsWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final LogStreamingService logStreamingService;

    // Track all active sessions
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    // Track which sources each session is subscribed to
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Role check should have been done in the handshake interceptor
        // If we get here without roles, something went wrong
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) session.getAttributes().get("roles");
        String username = (String) session.getAttributes().get("username");

        if (roles == null || !roles.contains("ROLE_DEVELOPER")) {
            log.warn("Rejecting logs WebSocket connection for user {} - missing ROLE_DEVELOPER", username);
            session.close(CloseStatus.POLICY_VIOLATION.withReason("ROLE_DEVELOPER required"));
            return;
        }

        sessions.add(session);
        sessionSubscriptions.put(session.getId(), new HashSet<>());

        log.info("Logs WebSocket connection established for user: {} (session: {})",
                username, session.getId());

        // Send initial connection acknowledgment
        sendMessage(session, Map.of(
                "type", "connected",
                "message", "Connected to logs stream. Send subscribe command to start receiving logs.",
                "availableSources", List.of("backend", "mosquitto", "postgres"),
                "dockerAvailable", logStreamingService.isDockerAvailable()
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);

        // Stop streaming for this session
        logStreamingService.stopStreaming(session.getId());
        sessionSubscriptions.remove(session.getId());

        String username = (String) session.getAttributes().get("username");

        if (status.getCode() != 1000 && status.getCode() != 1001) {
            log.warn("Logs WebSocket closed abnormally for user {}: {} ({})",
                    username, status.getCode(), status.getReason());
        } else {
            log.debug("Logs WebSocket closed normally for user: {}", username);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String username = (String) session.getAttributes().get("username");

        if (exception instanceof java.io.IOException ||
            exception instanceof java.nio.channels.ClosedChannelException) {
            log.debug("Logs WebSocket connection closed by client: {} ({})",
                    username, session.getId());
        } else {
            log.error("Logs WebSocket transport error for user {}: {}",
                    username, exception.getMessage());
        }

        sessions.remove(session);
        logStreamingService.stopStreaming(session.getId());
        sessionSubscriptions.remove(session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String action = (String) payload.get("action");

            switch (action) {
                case "subscribe" -> handleSubscribe(session, payload);
                case "unsubscribe" -> handleUnsubscribe(session, payload);
                case "history" -> handleHistoryRequest(session, payload);
                case "ping" -> sendMessage(session, Map.of("type", "pong"));
                default -> sendError(session, "Unknown action: " + action);
            }
        } catch (Exception e) {
            log.error("Error handling logs WebSocket message", e);
            sendError(session, "Invalid message format: " + e.getMessage());
        }
    }

    /**
     * Handle subscribe command to start receiving logs from specified sources.
     * Message format: {"action": "subscribe", "sources": ["backend", "mosquitto", "postgres"]}
     */
    @SuppressWarnings("unchecked")
    private void handleSubscribe(WebSocketSession session, Map<String, Object> payload) {
        List<String> sources = (List<String>) payload.get("sources");

        if (sources == null || sources.isEmpty()) {
            sendError(session, "No sources specified for subscription");
            return;
        }

        Set<String> validSources = new HashSet<>();
        for (String source : sources) {
            if (isValidSource(source)) {
                validSources.add(source.toLowerCase());
            } else {
                log.warn("Invalid log source requested: {}", source);
            }
        }

        if (validSources.isEmpty()) {
            sendError(session, "No valid sources specified");
            return;
        }

        // Update subscriptions
        sessionSubscriptions.put(session.getId(), validSources);

        // Start streaming
        logStreamingService.startStreaming(validSources, session.getId(),
                entry -> broadcastToSession(session, entry));

        String username = (String) session.getAttributes().get("username");
        log.info("User {} subscribed to log sources: {}", username, validSources);

        sendMessage(session, Map.of(
                "type", "subscribed",
                "sources", validSources
        ));
    }

    /**
     * Handle unsubscribe command to stop receiving logs.
     * Message format: {"action": "unsubscribe", "sources": ["backend"]} or {"action": "unsubscribe"}
     */
    @SuppressWarnings("unchecked")
    private void handleUnsubscribe(WebSocketSession session, Map<String, Object> payload) {
        List<String> sourcesToRemove = (List<String>) payload.get("sources");

        if (sourcesToRemove == null || sourcesToRemove.isEmpty()) {
            // Unsubscribe from all
            logStreamingService.stopStreaming(session.getId());
            sessionSubscriptions.put(session.getId(), new HashSet<>());
            sendMessage(session, Map.of("type", "unsubscribed", "sources", List.of()));
        } else {
            Set<String> current = sessionSubscriptions.getOrDefault(session.getId(), new HashSet<>());
            sourcesToRemove.forEach(s -> current.remove(s.toLowerCase()));

            // Restart streaming with remaining sources
            logStreamingService.stopStreaming(session.getId());
            if (!current.isEmpty()) {
                logStreamingService.startStreaming(current, session.getId(),
                        entry -> broadcastToSession(session, entry));
            }
            sessionSubscriptions.put(session.getId(), current);
            sendMessage(session, Map.of("type", "unsubscribed", "sources", current));
        }
    }

    /**
     * Handle request for historical logs.
     * Message format: {"action": "history", "source": "backend", "lines": 100}
     */
    private void handleHistoryRequest(WebSocketSession session, Map<String, Object> payload) {
        String source = (String) payload.get("source");
        int lines = payload.containsKey("lines") ? ((Number) payload.get("lines")).intValue() : 100;
        lines = Math.min(lines, 1000); // Cap at 1000 lines

        if (!"backend".equals(source)) {
            sendError(session, "History only available for backend logs");
            return;
        }

        List<LogEntryDto> history = logStreamingService.getRecentBackendLogs(lines);

        sendMessage(session, Map.of(
                "type", "history",
                "source", source,
                "logs", history
        ));
    }

    /**
     * Broadcast a log entry to a specific session.
     */
    private void broadcastToSession(WebSocketSession session, LogEntryDto entry) {
        if (!session.isOpen()) {
            return;
        }

        Set<String> subscriptions = sessionSubscriptions.get(session.getId());
        if (subscriptions == null || !subscriptions.contains(entry.source())) {
            return;
        }

        try {
            sendMessage(session, Map.of(
                    "type", "log",
                    "entry", entry
            ));
        } catch (Exception e) {
            log.debug("Failed to send log entry to session {}", session.getId());
        }
    }

    /**
     * Send a message to a WebSocket session.
     */
    private void sendMessage(WebSocketSession session, Object payload) {
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    String json = objectMapper.writeValueAsString(payload);
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            log.debug("Failed to send message to session {}: {}", session.getId(), e.getMessage());
        }
    }

    /**
     * Send an error message to a WebSocket session.
     */
    private void sendError(WebSocketSession session, String errorMessage) {
        sendMessage(session, Map.of(
                "type", "error",
                "message", errorMessage
        ));
    }

    /**
     * Check if a log source is valid.
     */
    private boolean isValidSource(String source) {
        return source != null &&
               (source.equalsIgnoreCase("backend") ||
                source.equalsIgnoreCase("mosquitto") ||
                source.equalsIgnoreCase("postgres"));
    }

    /**
     * Get the count of active sessions.
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
}
