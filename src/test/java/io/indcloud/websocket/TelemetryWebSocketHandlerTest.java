package io.indcloud.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TelemetryWebSocketHandler.
 * Tests session management and connection handling.
 */
@ExtendWith(MockitoExtension.class)
class TelemetryWebSocketHandlerTest {

    private TelemetryWebSocketHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new TelemetryWebSocketHandler(objectMapper);
    }

    @Test
    void afterConnectionEstablished_shouldAcceptConnection() throws Exception {
        // Given
        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("session-1");
        when(mockSession.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 12345));

        // When
        handler.afterConnectionEstablished(mockSession);

        // Then
        assertThat(handler.getConnectedSessionCount()).isEqualTo(1);
        verify(mockSession, never()).close(any());
    }

    @Test
    void afterConnectionClosed_shouldRemoveSession() throws Exception {
        // Given
        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("session-1");
        when(mockSession.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 12345));
        handler.afterConnectionEstablished(mockSession);
        assertThat(handler.getConnectedSessionCount()).isEqualTo(1);

        // When
        handler.afterConnectionClosed(mockSession, CloseStatus.NORMAL);

        // Then
        assertThat(handler.getConnectedSessionCount()).isEqualTo(0);
    }

    @Test
    void handleTransportError_shouldRemoveSession() throws Exception {
        // Given
        WebSocketSession mockSession = mock(WebSocketSession.class);
        when(mockSession.getId()).thenReturn("session-1");
        when(mockSession.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 12345));
        handler.afterConnectionEstablished(mockSession);
        assertThat(handler.getConnectedSessionCount()).isEqualTo(1);

        // When
        handler.handleTransportError(mockSession, new RuntimeException("Connection error"));

        // Then
        assertThat(handler.getConnectedSessionCount()).isEqualTo(0);
    }

    @Test
    void getConnectedSessionCount_shouldReturnCorrectCount() throws Exception {
        // Given
        assertThat(handler.getConnectedSessionCount()).isEqualTo(0);

        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);
        WebSocketSession session3 = mock(WebSocketSession.class);

        when(session1.getId()).thenReturn("session-1");
        when(session1.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 12345));
        when(session2.getId()).thenReturn("session-2");
        when(session2.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 12346));
        when(session3.getId()).thenReturn("session-3");
        when(session3.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 12347));

        // When/Then
        handler.afterConnectionEstablished(session1);
        assertThat(handler.getConnectedSessionCount()).isEqualTo(1);

        handler.afterConnectionEstablished(session2);
        assertThat(handler.getConnectedSessionCount()).isEqualTo(2);

        handler.afterConnectionEstablished(session3);
        assertThat(handler.getConnectedSessionCount()).isEqualTo(3);

        handler.afterConnectionClosed(session2, CloseStatus.NORMAL);
        assertThat(handler.getConnectedSessionCount()).isEqualTo(2);
    }

    @Test
    void broadcastTelemetryData_shouldNotFailWithNoSessions() {
        // Given - no sessions
        assertThat(handler.getConnectedSessionCount()).isEqualTo(0);

        // When - broadcast should not throw
        io.indcloud.dto.TelemetryPointDto telemetry = new io.indcloud.dto.TelemetryPointDto(
                "device-1", java.time.Instant.now(), 100.0, 220.0, 0.5, 0.95, 60.0);
        handler.broadcastTelemetryData(telemetry, 1L);

        // Then - no exception should be thrown
    }

    @Test
    void broadcastDynamicTelemetryData_shouldNotFailWithNoSessions() {
        // Given - no sessions
        assertThat(handler.getConnectedSessionCount()).isEqualTo(0);

        // When - broadcast should not throw
        java.util.Map<String, Double> variables = new java.util.HashMap<>();
        variables.put("temperature", 25.5);
        io.indcloud.dto.DynamicTelemetryPointDto dynamicPoint = new io.indcloud.dto.DynamicTelemetryPointDto(
                "device-1", java.time.Instant.now(), variables);
        handler.broadcastDynamicTelemetryData(dynamicPoint, 1L);

        // Then - no exception should be thrown
    }
}
