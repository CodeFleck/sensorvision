package io.indcloud.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.security.JwtService;
import io.indcloud.websocket.LogsWebSocketHandler;
import io.indcloud.websocket.TelemetryWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final TelemetryWebSocketHandler telemetryWebSocketHandler;
    private final LogsWebSocketHandler logsWebSocketHandler;
    private final JwtService jwtService;

    private static final String[] ALLOWED_ORIGINS = {
            // Local development
            "http://localhost:3003",
            "http://localhost:3002",
            "http://localhost:3001",
            "http://localhost:3000",
            // Legacy/staging IP-based access
            "http://35.88.65.186.nip.io:8080",
            "http://35.88.65.186:8080",
            // Production domain
            "http://indcloud.io",
            "https://indcloud.io",
            "http://www.indcloud.io",
            "https://www.indcloud.io",
            "http://api.indcloud.io",
            "https://api.indcloud.io",
            "http://54.149.190.208",
            "http://54.149.190.208:8080"
    };

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Telemetry WebSocket - allows unauthenticated connections for backward compatibility
        registry.addHandler(telemetryWebSocketHandler, "/ws/telemetry")
                .setAllowedOrigins(ALLOWED_ORIGINS)
                .addInterceptors(createTelemetryHandshakeInterceptor());

        // Logs WebSocket - requires ROLE_DEVELOPER authentication
        registry.addHandler(logsWebSocketHandler, "/ws/logs")
                .setAllowedOrigins(ALLOWED_ORIGINS)
                .addInterceptors(createLogsHandshakeInterceptor());
    }

    /**
     * Creates the handshake interceptor for telemetry WebSocket.
     * Allows unauthenticated connections for backward compatibility.
     */
    private HandshakeInterceptor createTelemetryHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                           WebSocketHandler wsHandler, Map<String, Object> attributes) {
                if (request instanceof ServletServerHttpRequest servletRequest) {
                    String token = servletRequest.getServletRequest().getParameter("token");

                    if (token != null && !token.isEmpty()) {
                        try {
                            if (jwtService.validateToken(token)) {
                                Long organizationId = jwtService.getOrganizationIdFromToken(token);
                                if (organizationId != null) {
                                    attributes.put("organizationId", organizationId);
                                    log.debug("WebSocket authenticated for organization: {}", organizationId);
                                }
                            } else {
                                log.warn("Invalid JWT token in WebSocket handshake");
                            }
                        } catch (Exception e) {
                            log.error("Failed to validate WebSocket JWT token", e);
                        }
                    } else {
                        log.debug("WebSocket connection without authentication token");
                    }
                }
                // Allow connection even without token for backward compatibility
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Exception exception) {
                // No post-handshake processing needed
            }
        };
    }

    /**
     * Creates the handshake interceptor for logs WebSocket.
     * Requires valid JWT with ROLE_DEVELOPER to connect.
     */
    private HandshakeInterceptor createLogsHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                           WebSocketHandler wsHandler, Map<String, Object> attributes) {
                if (!(request instanceof ServletServerHttpRequest servletRequest)) {
                    log.warn("Logs WebSocket: Non-servlet request rejected");
                    return false;
                }

                String token = servletRequest.getServletRequest().getParameter("token");

                if (token == null || token.isEmpty()) {
                    log.warn("Logs WebSocket: Connection rejected - no authentication token");
                    return false;
                }

                try {
                    if (!jwtService.validateToken(token)) {
                        log.warn("Logs WebSocket: Connection rejected - invalid token");
                        return false;
                    }

                    // Check for ROLE_DEVELOPER
                    List<String> roles = jwtService.getRolesFromToken(token);
                    if (roles == null || !roles.contains("ROLE_DEVELOPER")) {
                        String username = jwtService.getUsernameFromToken(token);
                        log.warn("Logs WebSocket: Connection rejected for user {} - ROLE_DEVELOPER required", username);
                        return false;
                    }

                    // Store user info in session attributes
                    String username = jwtService.getUsernameFromToken(token);
                    Long userId = jwtService.getUserIdFromToken(token);
                    Long organizationId = jwtService.getOrganizationIdFromToken(token);

                    attributes.put("username", username);
                    attributes.put("userId", userId);
                    attributes.put("organizationId", organizationId);
                    attributes.put("roles", roles);

                    log.info("Logs WebSocket: User {} authenticated with roles {}", username, roles);
                    return true;

                } catch (Exception e) {
                    log.error("Logs WebSocket: Failed to validate token", e);
                    return false;
                }
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Exception exception) {
                // No post-handshake processing needed
            }
        };
    }
}