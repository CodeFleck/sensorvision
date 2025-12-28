package io.indcloud.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API controller for checking connectivity to external services.
 * Used by the Integration Wizard to help users diagnose connection issues.
 *
 * All endpoints are public (no authentication required) since they're used
 * during device onboarding before the device has credentials.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/connectivity")
public class ConnectivityController {

    private final String mqttBrokerUrl;

    @Autowired
    public ConnectivityController(
            @Value("${mqtt.broker.url:tcp://localhost:1883}") String mqttBrokerUrl) {
        this.mqttBrokerUrl = mqttBrokerUrl;
    }

    /**
     * Check MQTT broker connectivity.
     * GET /api/v1/connectivity/mqtt
     *
     * Tests if the MQTT broker is reachable from the backend server.
     * This helps users determine if their device connectivity issues are:
     * - Server-side (broker is down) - we can detect this
     * - Client-side (firewall, network) - user needs to check their network
     *
     * @return Status of MQTT broker connectivity with connection details
     */
    @GetMapping("/mqtt")
    public ResponseEntity<Map<String, Object>> checkMqttConnectivity() {
        Map<String, Object> response = new HashMap<>();

        response.put("brokerUrl", mqttBrokerUrl);

        try {
            // Parse the MQTT URL to extract host and port
            // Format: tcp://hostname:port or ssl://hostname:port
            URI uri = new URI(mqttBrokerUrl.replace("tcp://", "http://").replace("ssl://", "https://"));
            String host = uri.getHost();
            int port = uri.getPort() != -1 ? uri.getPort() : 1883;

            response.put("host", host);
            response.put("port", port);

            // Attempt TCP connection to verify broker is reachable
            boolean reachable = testTcpConnection(host, port, 5000);

            if (reachable) {
                response.put("status", "online");
                response.put("message", "MQTT broker is reachable at " + host + ":" + port);
                response.put("reachable", true);

                // Provide guidance for users
                response.put("guidance",
                    "The MQTT broker is online. If your device cannot connect, check: " +
                    "1) Your device has internet access, " +
                    "2) Port " + port + " is not blocked by your firewall, " +
                    "3) Your device code has the correct broker address.");

                log.debug("MQTT connectivity check: broker reachable at {}:{}", host, port);
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "unreachable");
                response.put("message", "MQTT broker is not responding at " + host + ":" + port);
                response.put("reachable", false);
                response.put("guidance",
                    "The MQTT broker appears to be offline or unreachable. " +
                    "Please contact support if this persists.");

                log.warn("MQTT connectivity check: broker unreachable at {}:{}", host, port);
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            log.error("MQTT connectivity check failed: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", "Failed to check MQTT connectivity: " + e.getMessage());
            response.put("reachable", false);
            response.put("guidance", "An error occurred while checking connectivity. Please try again.");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Check HTTP API connectivity.
     * GET /api/v1/connectivity/http
     *
     * Simple endpoint that confirms the HTTP API is responding.
     * The Integration Wizard already tests this via the ingest endpoint,
     * but this provides a lightweight health check.
     *
     * @return Status of HTTP API
     */
    @GetMapping("/http")
    public ResponseEntity<Map<String, Object>> checkHttpConnectivity() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "online");
        response.put("message", "HTTP API is responding");
        response.put("reachable", true);
        response.put("guidance",
            "The HTTP API is online. If your device cannot connect, check: " +
            "1) Your device has internet access, " +
            "2) Your API token is correct (X-API-Key header), " +
            "3) The device ID in the URL matches your token.");
        return ResponseEntity.ok(response);
    }

    /**
     * Test TCP connection to a host:port with timeout.
     */
    private boolean testTcpConnection(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            log.debug("TCP connection test failed for {}:{} - {}", host, port, e.getMessage());
            return false;
        }
    }
}
