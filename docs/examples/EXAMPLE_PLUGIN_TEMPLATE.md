# Example Plugin Template

**Purpose**: Copy-paste template for creating custom Industrial Cloud plugins
**Last Updated**: 2025-11-14

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Notification Plugin Template](#notification-plugin-template)
3. [Protocol Parser Template](#protocol-parser-template)
4. [Integration Plugin Template](#integration-plugin-template)
5. [Testing Template](#testing-template)
6. [Migration Template](#migration-template)

---

## Quick Start

### Step 1: Choose Your Plugin Type

- **Notification Plugin** - Send alerts to external services (Slack, email, SMS, etc.)
- **Protocol Parser** - Decode IoT protocol data (LoRaWAN, Modbus, custom protocols)
- **Integration Plugin** - Connect to third-party platforms (AWS IoT, Azure, webhooks)

### Step 2: Copy the Appropriate Template

Use the templates below as starting points for your plugin.

### Step 3: Customize and Test

1. Replace `MyPlugin` with your plugin name
2. Update configuration schema
3. Implement plugin logic
4. Add tests
5. Create migration
6. Submit PR

---

## Notification Plugin Template

### File Structure
```
src/main/java/org/indcloud/plugins/community/
└── myplugin/
    ├── MyNotificationPlugin.java
    └── MyNotificationClient.java

src/test/java/org/indcloud/plugins/community/
└── myplugin/
    └── MyNotificationPluginTest.java

src/main/resources/db/migration/
└── V52__Add_my_plugin.sql
```

### 1. Plugin Implementation

**File**: `src/main/java/org/indcloud/plugins/community/myplugin/MyNotificationPlugin.java`

```java
package org.indcloud.plugins.community.myplugin;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.indcloud.model.Alert;
import org.indcloud.service.NotificationService;

/**
 * My Custom Notification Plugin
 *
 * Sends alert notifications to [YOUR SERVICE NAME].
 *
 * @author Your Name
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MyNotificationPlugin {

    private final MyNotificationClient client;

    /**
     * Send notification for an alert
     *
     * @param alert Alert to send notification for
     * @param config Plugin configuration (from JSON Schema)
     */
    public void sendNotification(Alert alert, JsonNode config) {
        log.info("Sending notification for alert: {}", alert.getId());

        try {
            // Extract configuration
            String webhookUrl = config.get("webhookUrl").asText();
            String channel = config.has("channel") ? config.get("channel").asText() : null;
            boolean includeMetadata = config.has("includeMetadata")
                ? config.get("includeMetadata").asBoolean()
                : true;

            // Build notification payload
            NotificationPayload payload = buildPayload(alert, channel, includeMetadata);

            // Send notification
            client.sendNotification(webhookUrl, payload);

            log.info("Notification sent successfully for alert: {}", alert.getId());

        } catch (Exception e) {
            log.error("Failed to send notification for alert: {}", alert.getId(), e);
            throw new PluginExecutionException("Failed to send notification", e);
        }
    }

    /**
     * Test connection with provided configuration
     *
     * @param config Plugin configuration
     * @return true if connection successful
     */
    public boolean testConnection(JsonNode config) {
        try {
            String webhookUrl = config.get("webhookUrl").asText();
            return client.testConnection(webhookUrl);
        } catch (Exception e) {
            log.error("Connection test failed", e);
            return false;
        }
    }

    /**
     * Build notification payload from alert
     */
    private NotificationPayload buildPayload(Alert alert, String channel, boolean includeMetadata) {
        NotificationPayload payload = new NotificationPayload();

        // Basic info
        payload.setTitle(alert.getRuleName());
        payload.setMessage(alert.getMessage());
        payload.setSeverity(alert.getSeverity().toString());
        payload.setTimestamp(alert.getTriggeredAt());

        // Optional channel
        if (channel != null) {
            payload.setChannel(channel);
        }

        // Optional metadata
        if (includeMetadata && alert.getDevice() != null) {
            payload.setDeviceName(alert.getDevice().getName());
            payload.setDeviceId(alert.getDevice().getDeviceId());
            payload.setOrganization(alert.getDevice().getOrganization().getName());
        }

        return payload;
    }
}
```

### 2. HTTP Client

**File**: `src/main/java/org/indcloud/plugins/community/myplugin/MyNotificationClient.java`

```java
package org.indcloud.plugins.community.myplugin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for My Notification Service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MyNotificationClient {

    private final RestTemplate restTemplate;

    /**
     * Send notification to webhook URL
     */
    public void sendNotification(String webhookUrl, NotificationPayload payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<NotificationPayload> request = new HttpEntity<>(payload, headers);

        restTemplate.postForEntity(webhookUrl, request, String.class);

        log.debug("Notification sent to: {}", webhookUrl);
    }

    /**
     * Test webhook connection
     */
    public boolean testConnection(String webhookUrl) {
        try {
            // Send a test ping
            NotificationPayload testPayload = new NotificationPayload();
            testPayload.setTitle("Connection Test");
            testPayload.setMessage("Industrial Cloud plugin connection test");

            sendNotification(webhookUrl, testPayload);
            return true;
        } catch (Exception e) {
            log.error("Connection test failed for: {}", webhookUrl, e);
            return false;
        }
    }
}
```

### 3. Payload Model

**File**: `src/main/java/org/indcloud/plugins/community/myplugin/NotificationPayload.java`

```java
package org.indcloud.plugins.community.myplugin;

import lombok.Data;
import java.time.Instant;

/**
 * Notification payload for external service
 */
@Data
public class NotificationPayload {
    private String title;
    private String message;
    private String severity;
    private Instant timestamp;
    private String channel;
    private String deviceName;
    private String deviceId;
    private String organization;
}
```

### 4. Unit Tests

**File**: `src/test/java/org/indcloud/plugins/community/myplugin/MyNotificationPluginTest.java`

```java
package org.indcloud.plugins.community.myplugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.indcloud.model.*;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyNotificationPluginTest {

    @Mock
    private MyNotificationClient client;

    @InjectMocks
    private MyNotificationPlugin plugin;

    @Captor
    private ArgumentCaptor<NotificationPayload> payloadCaptor;

    private Alert testAlert;
    private JsonNode testConfig;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        // Create test alert
        Device device = new Device();
        device.setDeviceId("test-device-001");
        device.setName("Test Device");

        Organization org = Organization.builder()
            .id(1L)
            .name("Test Organization")
            .build();
        device.setOrganization(org);

        Rule rule = new Rule();
        rule.setId(1L);
        rule.setName("High Temperature Alert");

        testAlert = new Alert();
        testAlert.setId(1L);
        testAlert.setDevice(device);
        testAlert.setRule(rule);
        testAlert.setRuleName("High Temperature Alert");
        testAlert.setMessage("Temperature exceeded threshold: 85°C");
        testAlert.setSeverity(AlertSeverity.HIGH);
        testAlert.setTriggeredAt(Instant.now());

        // Create test configuration
        String configJson = """
            {
                "webhookUrl": "https://hooks.example.com/webhook/test",
                "channel": "#alerts",
                "includeMetadata": true
            }
            """;
        testConfig = objectMapper.readTree(configJson);
    }

    @Test
    void shouldSendNotificationSuccessfully() {
        // Act
        plugin.sendNotification(testAlert, testConfig);

        // Assert
        verify(client).sendNotification(
            eq("https://hooks.example.com/webhook/test"),
            payloadCaptor.capture()
        );

        NotificationPayload payload = payloadCaptor.getValue();
        assertThat(payload.getTitle()).isEqualTo("High Temperature Alert");
        assertThat(payload.getMessage()).contains("Temperature exceeded threshold");
        assertThat(payload.getSeverity()).isEqualTo("HIGH");
        assertThat(payload.getChannel()).isEqualTo("#alerts");
        assertThat(payload.getDeviceName()).isEqualTo("Test Device");
    }

    @Test
    void shouldSendNotificationWithoutMetadata() throws Exception {
        // Arrange
        String configJson = """
            {
                "webhookUrl": "https://hooks.example.com/webhook/test",
                "includeMetadata": false
            }
            """;
        JsonNode config = objectMapper.readTree(configJson);

        // Act
        plugin.sendNotification(testAlert, config);

        // Assert
        verify(client).sendNotification(any(), payloadCaptor.capture());
        NotificationPayload payload = payloadCaptor.getValue();
        assertThat(payload.getDeviceName()).isNull();
        assertThat(payload.getDeviceId()).isNull();
    }

    @Test
    void shouldTestConnectionSuccessfully() {
        // Arrange
        when(client.testConnection("https://hooks.example.com/webhook/test"))
            .thenReturn(true);

        // Act
        boolean result = plugin.testConnection(testConfig);

        // Assert
        assertThat(result).isTrue();
        verify(client).testConnection("https://hooks.example.com/webhook/test");
    }

    @Test
    void shouldHandleConnectionTestFailure() {
        // Arrange
        when(client.testConnection(any())).thenReturn(false);

        // Act
        boolean result = plugin.testConnection(testConfig);

        // Assert
        assertThat(result).isFalse();
    }
}
```

### 5. Database Migration

**File**: `src/main/resources/db/migration/V52__Add_my_plugin.sql`

```sql
-- Add My Custom Notification Plugin to Marketplace
-- Migration V52

INSERT INTO plugin_registry (
    plugin_key, name, description, category, version, author, author_url,
    icon_url, repository_url, documentation_url,
    min_indcloud_version, max_indcloud_version,
    is_official, is_verified, installation_count, rating_average, rating_count,
    plugin_provider, plugin_type, config_schema, tags, screenshots, changelog,
    published_at, created_at, updated_at
) VALUES (
    'my-notification-plugin',
    'My Notification Plugin',
    'Send alert notifications to [YOUR SERVICE NAME]. Supports custom channels, rich formatting, and metadata inclusion.',
    'NOTIFICATION',
    '1.0.0',
    'Your Name',
    'https://github.com/yourusername',
    'https://example.com/icon.png',
    'https://github.com/yourusername/indcloud',
    'https://github.com/yourusername/indcloud/blob/main/docs/MY_PLUGIN.md',
    '1.0.0',
    null,
    false,  -- Not official (community plugin)
    false,  -- Not verified yet
    0,
    0.0,
    0,
    'CUSTOM_PARSER',  -- Use existing provider or add new one
    'INTEGRATION',
    '{
        "type": "object",
        "required": ["webhookUrl"],
        "properties": {
            "webhookUrl": {
                "type": "string",
                "title": "Webhook URL",
                "description": "Your service webhook URL",
                "placeholder": "https://hooks.example.com/webhook/YOUR_ID",
                "format": "password"
            },
            "channel": {
                "type": "string",
                "title": "Channel/Room",
                "description": "Channel or room to send notifications to",
                "placeholder": "#alerts"
            },
            "includeMetadata": {
                "type": "boolean",
                "title": "Include Device Metadata",
                "description": "Include device name, ID, and organization in notifications",
                "default": true
            },
            "severity": {
                "type": "string",
                "title": "Minimum Severity",
                "description": "Only send notifications for alerts at or above this severity",
                "enum": ["LOW", "MEDIUM", "HIGH", "CRITICAL"],
                "default": "MEDIUM"
            }
        }
    }',
    ARRAY['notification', 'alerts', 'custom'],
    ARRAY['https://example.com/screenshot1.png'],
    'v1.0.0 - Initial release',
    NOW(),
    NOW(),
    NOW()
);
```

---

## Protocol Parser Template

### File Structure
```
src/main/java/org/indcloud/plugins/community/
└── myprotocol/
    ├── MyProtocolParser.java
    └── MyProtocolDecoder.java
```

### 1. Parser Implementation

**File**: `src/main/java/org/indcloud/plugins/community/myprotocol/MyProtocolParser.java`

```java
package org.indcloud.plugins.community.myprotocol;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * My Custom Protocol Parser
 *
 * Parses and decodes data from [YOUR PROTOCOL NAME].
 *
 * @author Your Name
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MyProtocolParser {

    private final MyProtocolDecoder decoder;

    /**
     * Parse payload from protocol message
     *
     * @param payload Raw binary payload
     * @param config Plugin configuration
     * @return Parsed telemetry variables
     */
    public Map<String, Object> parsePayload(byte[] payload, JsonNode config) {
        log.debug("Parsing payload of {} bytes", payload.length);

        try {
            // Validate payload
            if (!validatePayload(payload)) {
                throw new IllegalArgumentException("Invalid payload format");
            }

            // Extract configuration
            String payloadFormat = config.has("payloadFormat")
                ? config.get("payloadFormat").asText()
                : "default";
            boolean includeRaw = config.has("includeRaw")
                ? config.get("includeRaw").asBoolean()
                : false;

            // Decode payload based on format
            Map<String, Object> variables = decoder.decode(payload, payloadFormat);

            // Optionally include raw payload (hex encoded)
            if (includeRaw) {
                variables.put("_raw", bytesToHex(payload));
            }

            log.info("Successfully parsed payload: {} variables extracted", variables.size());
            return variables;

        } catch (Exception e) {
            log.error("Failed to parse payload", e);
            throw new PluginExecutionException("Failed to parse protocol payload", e);
        }
    }

    /**
     * Validate payload format
     */
    public boolean validatePayload(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return false;
        }

        // Add protocol-specific validation
        // Example: Check minimum length
        if (payload.length < 4) {
            log.warn("Payload too short: {} bytes", payload.length);
            return false;
        }

        // Example: Check magic header bytes
        if (payload[0] != (byte) 0xFF || payload[1] != (byte) 0xAA) {
            log.warn("Invalid header bytes");
            return false;
        }

        return true;
    }

    /**
     * Convert bytes to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
```

### 2. Decoder Implementation

**File**: `src/main/java/org/indcloud/plugins/community/myprotocol/MyProtocolDecoder.java`

```java
package org.indcloud.plugins.community.myprotocol;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * Decoder for My Protocol binary format
 */
@Slf4j
@Component
public class MyProtocolDecoder {

    /**
     * Decode binary payload to telemetry variables
     *
     * @param payload Binary payload
     * @param format Payload format (e.g., "default", "extended", "minimal")
     * @return Decoded variables
     */
    public Map<String, Object> decode(byte[] payload, String format) {
        Map<String, Object> variables = new HashMap<>();

        // Wrap payload in ByteBuffer for easier parsing
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        buffer.order(ByteOrder.LITTLE_ENDIAN); // Or BIG_ENDIAN depending on protocol

        try {
            switch (format) {
                case "default":
                    return decodeDefault(buffer);
                case "extended":
                    return decodeExtended(buffer);
                case "minimal":
                    return decodeMinimal(buffer);
                default:
                    log.warn("Unknown format: {}, using default", format);
                    return decodeDefault(buffer);
            }
        } catch (Exception e) {
            log.error("Error decoding payload with format: {}", format, e);
            throw new RuntimeException("Decoding failed", e);
        }
    }

    /**
     * Decode default payload format
     *
     * Example format:
     * [0-1]   Header (0xFF 0xAA)
     * [2-3]   Temperature (int16, °C * 100)
     * [4-5]   Humidity (uint16, % * 100)
     * [6-9]   Pressure (uint32, Pa)
     * [10-11] Battery (uint16, mV)
     */
    private Map<String, Object> decodeDefault(ByteBuffer buffer) {
        Map<String, Object> variables = new HashMap<>();

        // Skip header (already validated)
        buffer.getShort();

        // Temperature (int16, divide by 100 to get °C)
        short tempRaw = buffer.getShort();
        double temperature = tempRaw / 100.0;
        variables.put("temperature", temperature);

        // Humidity (uint16, divide by 100 to get %)
        int humidityRaw = buffer.getShort() & 0xFFFF; // unsigned
        double humidity = humidityRaw / 100.0;
        variables.put("humidity", humidity);

        // Pressure (uint32, Pa)
        long pressure = buffer.getInt() & 0xFFFFFFFFL; // unsigned
        variables.put("pressure", pressure);

        // Battery (uint16, mV)
        int battery = buffer.getShort() & 0xFFFF; // unsigned
        variables.put("battery_mv", battery);
        variables.put("battery_pct", calculateBatteryPercentage(battery));

        return variables;
    }

    /**
     * Decode extended payload format (with GPS)
     */
    private Map<String, Object> decodeExtended(ByteBuffer buffer) {
        // First decode default fields
        Map<String, Object> variables = decodeDefault(buffer);

        // Then add extended fields
        // Latitude (int32, degrees * 1e7)
        int latRaw = buffer.getInt();
        double latitude = latRaw / 1e7;
        variables.put("latitude", latitude);

        // Longitude (int32, degrees * 1e7)
        int lonRaw = buffer.getInt();
        double longitude = lonRaw / 1e7;
        variables.put("longitude", longitude);

        // Altitude (int16, meters)
        short altitude = buffer.getShort();
        variables.put("altitude", altitude);

        return variables;
    }

    /**
     * Decode minimal payload format (temperature only)
     */
    private Map<String, Object> decodeMinimal(ByteBuffer buffer) {
        Map<String, Object> variables = new HashMap<>();

        buffer.getShort(); // Skip header

        short tempRaw = buffer.getShort();
        double temperature = tempRaw / 100.0;
        variables.put("temperature", temperature);

        return variables;
    }

    /**
     * Calculate battery percentage from voltage
     */
    private double calculateBatteryPercentage(int millivolts) {
        // Example: LiPo battery (4200mV = 100%, 3000mV = 0%)
        double MIN_VOLTAGE = 3000.0;
        double MAX_VOLTAGE = 4200.0;

        if (millivolts >= MAX_VOLTAGE) return 100.0;
        if (millivolts <= MIN_VOLTAGE) return 0.0;

        return ((millivolts - MIN_VOLTAGE) / (MAX_VOLTAGE - MIN_VOLTAGE)) * 100.0;
    }
}
```

### 3. Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class MyProtocolParserTest {

    @InjectMocks
    private MyProtocolParser parser;

    @Mock
    private MyProtocolDecoder decoder;

    @Test
    void shouldParseValidPayload() throws Exception {
        // Arrange
        byte[] payload = new byte[]{
            (byte) 0xFF, (byte) 0xAA,  // Header
            0x09, (byte) 0xC4,          // Temperature: 25.00°C (0x09C4 = 2500)
            0x13, (byte) 0x88,          // Humidity: 50.00% (0x1388 = 5000)
            0x00, 0x01, (byte) 0x86, (byte) 0xA0,  // Pressure: 100000 Pa
            0x10, 0x68                  // Battery: 4200mV (0x1068)
        };

        JsonNode config = new ObjectMapper().readTree("{\"payloadFormat\": \"default\"}");

        Map<String, Object> expected = Map.of(
            "temperature", 25.0,
            "humidity", 50.0,
            "pressure", 100000L,
            "battery_mv", 4200,
            "battery_pct", 100.0
        );

        when(decoder.decode(payload, "default")).thenReturn(expected);

        // Act
        Map<String, Object> result = parser.parsePayload(payload, config);

        // Assert
        assertThat(result).containsEntry("temperature", 25.0);
        assertThat(result).containsEntry("humidity", 50.0);
    }

    @Test
    void shouldRejectInvalidPayload() {
        // Arrange
        byte[] invalidPayload = new byte[]{0x00, 0x00}; // Wrong header

        JsonNode config = new ObjectMapper().readTree("{}");

        // Act & Assert
        assertThatThrownBy(() -> parser.parsePayload(invalidPayload, config))
            .isInstanceOf(PluginExecutionException.class);
    }
}
```

---

## Integration Plugin Template

### HTTP Webhook Integration

**File**: `src/main/java/org/indcloud/plugins/community/myintegration/MyWebhookIntegration.java`

```java
package org.indcloud.plugins.community.myintegration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.indcloud.model.Device;
import org.indcloud.model.TelemetryRecord;
import org.indcloud.service.DeviceService;
import org.indcloud.service.TelemetryIngestionService;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * My Webhook Integration Plugin
 *
 * Receives webhooks from external systems and ingests telemetry data.
 *
 * @author Your Name
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MyWebhookIntegration {

    private final ObjectMapper objectMapper;
    private final DeviceService deviceService;
    private final TelemetryIngestionService telemetryService;

    /**
     * Process incoming webhook
     *
     * @param request HTTP request
     * @param config Plugin configuration
     */
    public void processWebhook(HttpServletRequest request, JsonNode config) {
        log.info("Processing webhook from: {}", request.getRemoteAddr());

        try {
            // Validate authentication (if configured)
            if (config.has("authToken")) {
                String authToken = config.get("authToken").asText();
                String providedToken = request.getHeader("Authorization");

                if (!authToken.equals(providedToken)) {
                    throw new SecurityException("Invalid authentication token");
                }
            }

            // Parse request body
            JsonNode payload = objectMapper.readTree(request.getInputStream());

            // Extract device ID from payload (based on configuration)
            String deviceIdField = config.get("deviceIdField").asText("device.id");
            String deviceId = extractField(payload, deviceIdField);

            // Get or create device
            Device device = deviceService.getDeviceByDeviceId(deviceId)
                .orElseGet(() -> createDevice(deviceId, config));

            // Extract telemetry data
            String dataField = config.get("dataField").asText("data");
            JsonNode data = extractFieldNode(payload, dataField);

            // Convert to variables map
            Map<String, Object> variables = objectMapper.convertValue(
                data,
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
            );

            // Extract timestamp (optional)
            Instant timestamp = extractTimestamp(payload, config);

            // Ingest telemetry
            telemetryService.ingestTelemetry(device, variables, timestamp);

            log.info("Successfully processed webhook for device: {}", deviceId);

        } catch (Exception e) {
            log.error("Failed to process webhook", e);
            throw new PluginExecutionException("Webhook processing failed", e);
        }
    }

    /**
     * Extract field from JSON using dot notation
     * Example: "device.id" extracts payload.device.id
     */
    private String extractField(JsonNode payload, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        JsonNode current = payload;

        for (String part : parts) {
            current = current.get(part);
            if (current == null) {
                throw new IllegalArgumentException("Field not found: " + fieldPath);
            }
        }

        return current.asText();
    }

    /**
     * Extract field node from JSON
     */
    private JsonNode extractFieldNode(JsonNode payload, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        JsonNode current = payload;

        for (String part : parts) {
            current = current.get(part);
            if (current == null) {
                throw new IllegalArgumentException("Field not found: " + fieldPath);
            }
        }

        return current;
    }

    /**
     * Extract timestamp from payload
     */
    private Instant extractTimestamp(JsonNode payload, JsonNode config) {
        if (!config.has("timestampField")) {
            return Instant.now();
        }

        String timestampField = config.get("timestampField").asText();
        try {
            String timestampStr = extractField(payload, timestampField);
            return Instant.parse(timestampStr);
        } catch (Exception e) {
            log.warn("Failed to parse timestamp, using current time", e);
            return Instant.now();
        }
    }

    /**
     * Create new device from webhook
     */
    private Device createDevice(String deviceId, JsonNode config) {
        log.info("Auto-creating device: {}", deviceId);

        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setName("Auto-provisioned: " + deviceId);
        device.setDescription("Created by webhook integration");

        return deviceService.createDevice(device);
    }
}
```

---

## Testing Template

### Integration Test

```java
@SpringBootTest
@Transactional
class MyPluginIntegrationTest {

    @Autowired
    private MyNotificationPlugin plugin;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    void shouldIntegrateWithDatabase() {
        // Create test data in database
        Device device = createTestDevice();
        deviceRepository.save(device);

        Alert alert = createTestAlert(device);
        alertRepository.save(alert);

        // Test plugin with real data
        JsonNode config = createConfig();
        plugin.sendNotification(alert, config);

        // Verify database state
        Alert savedAlert = alertRepository.findById(alert.getId()).orElseThrow();
        assertThat(savedAlert.getStatus()).isEqualTo(AlertStatus.NOTIFIED);
    }
}
```

---

## Migration Template

### Full Example with All Fields

```sql
-- V52__Add_my_custom_plugin.sql

INSERT INTO plugin_registry (
    -- Required fields
    plugin_key,
    name,
    description,
    category,
    version,
    author,
    plugin_provider,
    plugin_type,
    config_schema,

    -- Recommended fields
    author_url,
    repository_url,
    documentation_url,
    tags,

    -- Optional fields
    icon_url,
    screenshots,
    changelog,
    min_indcloud_version,

    -- System fields
    is_official,
    is_verified,
    installation_count,
    rating_average,
    rating_count,
    published_at,
    created_at,
    updated_at
) VALUES (
    -- Required
    'my-custom-plugin',
    'My Custom Plugin',
    'Detailed description of what your plugin does. Include key features and use cases.',
    'NOTIFICATION',  -- or PROTOCOL_PARSER, INTEGRATION, DATA_SOURCE
    '1.0.0',
    'Your Name or Organization',
    'CUSTOM_PARSER',
    'INTEGRATION',
    '{
        "type": "object",
        "required": ["webhookUrl"],
        "properties": {
            "webhookUrl": {
                "type": "string",
                "title": "Webhook URL",
                "description": "Your service webhook URL",
                "format": "password"
            },
            "enabled": {
                "type": "boolean",
                "title": "Enabled",
                "default": true
            }
        }
    }',

    -- Recommended
    'https://github.com/yourusername',
    'https://github.com/yourusername/indcloud',
    'https://github.com/yourusername/indcloud/blob/main/docs/MY_PLUGIN.md',
    ARRAY['webhook', 'integration', 'custom'],

    -- Optional
    'https://example.com/icon.svg',
    ARRAY['https://example.com/screenshot1.png', 'https://example.com/screenshot2.png'],
    'v1.0.0 - Initial release\nv1.0.1 - Bug fixes',
    '1.0.0',

    -- System
    false,  -- is_official (community plugin)
    false,  -- is_verified (not yet verified)
    0,      -- installation_count
    0.0,    -- rating_average
    0,      -- rating_count
    NOW(),  -- published_at
    NOW(),  -- created_at
    NOW()   -- updated_at
);
```

---

## Checklist

Before submitting your plugin:

- [ ] Plugin code follows Industrial Cloud coding standards
- [ ] All tests pass (unit + integration)
- [ ] Test coverage >80%
- [ ] No hardcoded credentials or secrets
- [ ] Configuration schema is valid JSON Schema
- [ ] Migration script runs without errors
- [ ] Documentation created (docs/plugins/YOUR_PLUGIN.md)
- [ ] Examples provided in documentation
- [ ] Security review passed (no SQL injection, XSS, etc.)
- [ ] Error handling implemented
- [ ] Logging added (info, warn, error levels)
- [ ] Plugin tested with real external service
- [ ] README updated (if applicable)

---

## Resources

- **Plugin Development Guide**: [PLUGIN_DEVELOPMENT_GUIDE.md](../PLUGIN_DEVELOPMENT_GUIDE.md)
- **API Documentation**: [PLUGIN_MARKETPLACE_API.md](../api/PLUGIN_MARKETPLACE_API.md)
- **JSON Schema Reference**: https://json-schema.org/
- **Industrial Cloud Repository**: https://github.com/CodeFleck/indcloud

---

## Getting Help

- **GitHub Issues**: https://github.com/CodeFleck/indcloud/issues
- **Discussions**: https://github.com/CodeFleck/indcloud/discussions
- **Email**: plugin-dev@indcloud.io

---

**Happy Plugin Development!** 🚀
