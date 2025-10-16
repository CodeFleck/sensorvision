package org.sensorvision.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        log.info("WebSocket connection established: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        log.info("WebSocket connection closed: {} with status: {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session);
    }

    /**
     * Broadcast telemetry data to all connected WebSocket clients
     * Fixed to avoid race conditions by collecting failed sessions separately
     */
    public void broadcastTelemetryData(TelemetryPointDto telemetryPoint) {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            String message = objectMapper.writeValueAsString(telemetryPoint);
            TextMessage textMessage = new TextMessage(message);

            // Collect failed sessions separately to avoid concurrent modification
            List<WebSocketSession> failedSessions = new ArrayList<>();

            for (WebSocketSession session : sessions) {
                try {
                    // Synchronize on session to prevent concurrent access
                    synchronized (session) {
                        if (session.isOpen()) {
                            session.sendMessage(textMessage);
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

        } catch (Exception e) {
            log.error("Failed to broadcast telemetry data: {}", e.getMessage());
        }
    }

    /**
     * Get current number of connected sessions
     */
    public int getConnectedSessionCount() {
        return sessions.size();
    }
}