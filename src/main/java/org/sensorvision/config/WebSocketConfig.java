package org.sensorvision.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.security.JwtService;
import org.sensorvision.websocket.TelemetryWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final TelemetryWebSocketHandler telemetryWebSocketHandler;
    private final JwtService jwtService;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(telemetryWebSocketHandler, "/ws/telemetry")
                .setAllowedOrigins("http://localhost:3003", "http://localhost:3002", "http://localhost:3001", "http://localhost:3000") // Allow React dev server
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        // Extract JWT token from query parameters
                        if (request instanceof ServletServerHttpRequest) {
                            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                            String token = servletRequest.getServletRequest().getParameter("token");

                            if (token != null && !token.isEmpty()) {
                                try {
                                    // Validate token
                                    if (jwtService.validateToken(token)) {
                                        // Extract organization ID from token
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
                        // Connections without organizationId won't receive any broadcasts
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                               WebSocketHandler wsHandler, Exception exception) {
                        // No post-handshake processing needed
                    }
                });
    }
}