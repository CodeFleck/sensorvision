package org.sensorvision.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.DynamicTelemetryPointDto;
import org.sensorvision.dto.TelemetryPointDto;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        log.debug("WebSocket connection established: {} from {}",
                session.getId(), session.getRemoteAddress());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);

        // Only log abnormal closures (not normal disconnects)
        // 1000 = Normal closure, 1001 = Going away (page reload/navigation)
        if (status.getCode() != 1000 && status.getCode() != 1001) {
            log.warn("WebSocket connection closed abnormally: {} with status code: {} ({})",
                    session.getId(), status.getCode(), status.getReason());
        } else {
            log.debug("WebSocket connection closed normally: {} with status: {}",
                    session.getId(), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // Only log actual errors, not normal connection issues during close
        // IOException "connection aborted" often happens during normal page reload/navigation
        // ClosedChannelException happens during application shutdown
        if (exception != null && (exception instanceof java.io.IOException ||
                                   exception instanceof java.nio.channels.ClosedChannelException)) {
            String message = exception.getMessage();
            if (message != null && (message.contains("connection was aborted") ||
                                   message.contains("Connection reset"))) {
                log.debug("WebSocket connection closed by client: {}", session.getId());
            } else if (exception instanceof java.nio.channels.ClosedChannelException) {
                log.debug("WebSocket channel closed (likely during shutdown): {}", session.getId());
            } else {
                log.warn("WebSocket I/O error for session {}: {}",
                        session.getId(), exception.getMessage());
            }
        } else {
            log.error("WebSocket transport error for session {}: {}",
                    session.getId(),
                    exception != null ? exception.getClass().getName() : "null exception");
            if (exception != null) {
                log.error("WebSocket transport error details for session {}", session.getId(), exception);
            }
        }
        sessions.remove(session);
    }

    /**
     * Broadcast telemetry data to WebSocket clients in the same organization
     * Fixed to avoid race conditions by collecting failed sessions separately
     *
     * @param telemetryPoint The telemetry data to broadcast
     * @param organizationId The organization ID that owns this device/data
     */
    public void broadcastTelemetryData(TelemetryPointDto telemetryPoint, Long organizationId) {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            String message = objectMapper.writeValueAsString(telemetryPoint);
            TextMessage textMessage = new TextMessage(message);

            // Collect failed sessions separately to avoid concurrent modification
            List<WebSocketSession> failedSessions = new ArrayList<>();
            int sentCount = 0;

            for (WebSocketSession session : sessions) {
                try {
                    // Synchronize on session to prevent concurrent access
                    synchronized (session) {
                        if (session.isOpen()) {
                            // Only send to sessions with matching organization ID
                            Long sessionOrgId = (Long) session.getAttributes().get("organizationId");

                            if (sessionOrgId != null && sessionOrgId.equals(organizationId)) {
                                session.sendMessage(textMessage);
                                sentCount++;
                            }
                            // Sessions without organizationId are silently skipped (no data sent)
                        } else {
                            failedSessions.add(session);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to send message to WebSocket session {}: {}",
                            session.getId(), e.getMessage());
                    failedSessions.add(session);
                }
            }

            // Remove failed sessions after iteration completes
            if (!failedSessions.isEmpty()) {
                sessions.removeAll(failedSessions);
                log.debug("Removed {} failed WebSocket sessions", failedSessions.size());
            }

            log.debug("Broadcast telemetry for device {} to {} sessions in organization {}",
                    telemetryPoint.deviceId(), sentCount, organizationId);

        } catch (Exception e) {
            log.error("Failed to broadcast telemetry data: {}", e.getMessage());
        }
    }

    /**
     * Broadcast dynamic telemetry data (EAV pattern) to WebSocket clients in the same organization.
     * This sends all variables as a map, supporting any variable name.
     *
     * @param dynamicPoint The dynamic telemetry data with all variables
     * @param organizationId The organization ID that owns this device/data
     */
    public void broadcastDynamicTelemetryData(DynamicTelemetryPointDto dynamicPoint, Long organizationId) {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            // Wrap in a message envelope with type indicator
            Map<String, Object> envelope = new HashMap<>();
            envelope.put("type", "DYNAMIC_TELEMETRY");
            envelope.put("deviceId", dynamicPoint.deviceId());
            envelope.put("timestamp", dynamicPoint.timestamp());
            envelope.put("variables", dynamicPoint.variables());

            String message = objectMapper.writeValueAsString(envelope);
            TextMessage textMessage = new TextMessage(message);

            // Collect failed sessions separately to avoid concurrent modification
            List<WebSocketSession> failedSessions = new ArrayList<>();
            int sentCount = 0;

            for (WebSocketSession session : sessions) {
                try {
                    synchronized (session) {
                        if (session.isOpen()) {
                            Long sessionOrgId = (Long) session.getAttributes().get("organizationId");

                            if (sessionOrgId != null && sessionOrgId.equals(organizationId)) {
                                session.sendMessage(textMessage);
                                sentCount++;
                            }
                        } else {
                            failedSessions.add(session);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to send dynamic telemetry to WebSocket session {}: {}",
                            session.getId(), e.getMessage());
                    failedSessions.add(session);
                }
            }

            if (!failedSessions.isEmpty()) {
                sessions.removeAll(failedSessions);
            }

            log.debug("Broadcast dynamic telemetry ({} variables) for device {} to {} sessions",
                    dynamicPoint.variables().size(), dynamicPoint.deviceId(), sentCount);

        } catch (Exception e) {
            log.error("Failed to broadcast dynamic telemetry data: {}", e.getMessage());
        }
    }

    /**
     * Get current number of connected sessions
     */
    public int getConnectedSessionCount() {
        return sessions.size();
    }
}