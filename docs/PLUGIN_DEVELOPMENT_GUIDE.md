# Plugin Development Guide

Complete guide for developing plugins for the Industrial Cloud Plugin Marketplace.

---

## Table of Contents

1. [Overview](#overview)
2. [Plugin Types](#plugin-types)
3. [Plugin Architecture](#plugin-architecture)
4. [Configuration Schema](#configuration-schema)
5. [Creating Your First Plugin](#creating-your-first-plugin)
6. [Testing](#testing)
7. [Publishing](#publishing)
8. [Best Practices](#best-practices)

---

## Overview

The Industrial Cloud Plugin Marketplace allows developers to extend the platform with:
- **Protocol Parsers** - Decode data from IoT protocols (LoRaWAN, Modbus, Sigfox, etc.)
- **Notification Channels** - Send alerts to external services (Slack, Discord, PagerDuty, etc.)
- **Data Sources** - Ingest data from external systems (MQTT brokers, REST APIs, databases)
- **Integrations** - Connect to third-party platforms (AWS IoT, Azure IoT, Google Cloud IoT)
- **Analytics** - Advanced data processing and analysis
- **Transformations** - Custom data transformations and calculations

---

## Plugin Types

### 1. Protocol Parser
Parse and decode proprietary IoT protocol data.

**Example Use Cases:**
- LoRaWAN payload decoding
- Modbus register parsing
- Sigfox message interpretation

**Key Methods:**
- `parsePayload(byte[] payload, JsonNode config)` - Decode binary data
- `validateDevice(String deviceId)` - Validate device credentials
- `handleDownlink(String deviceId, JsonNode data)` - Send commands to device

### 2. Notification Channel
Send alert notifications to external services.

**Example Use Cases:**
- Slack messages
- Discord embeds
- Email notifications
- SMS alerts

**Key Methods:**
- `sendNotification(Alert alert, JsonNode config)` - Send notification
- `testConnection(JsonNode config)` - Test configuration
- `formatMessage(Alert alert)` - Format alert message

### 3. Data Source Integration
Poll or receive data from external sources.

**Example Use Cases:**
- HTTP webhooks
- MQTT bridge
- REST API polling
- Database queries

**Key Methods:**
- `fetchData(JsonNode config)` - Poll for new data
- `processWebhook(HttpRequest request)` - Handle webhook
- `transformData(JsonNode rawData)` - Transform to telemetry format

### 4. Third-Party Integration
Connect to external IoT platforms.

**Example Use Cases:**
- AWS IoT Core
- Azure IoT Hub
- Google Cloud IoT
- Particle Cloud

**Key Methods:**
- `syncDevices(JsonNode config)` - Sync device registry
- `forwardTelemetry(TelemetryRecord record)` - Forward data
- `receiveCommands()` - Receive remote commands

---

## Plugin Architecture

### Database Schema

```sql
CREATE TABLE plugin_registry (
    id BIGSERIAL PRIMARY KEY,
    plugin_key VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,  -- PROTOCOL_PARSER, NOTIFICATION, etc.
    version VARCHAR(20) NOT NULL,
    author VARCHAR(200),
    author_url VARCHAR(500),
    icon_url VARCHAR(500),
    repository_url VARCHAR(500),
    documentation_url VARCHAR(500),
    min_indcloud_version VARCHAR(20),
    max_indcloud_version VARCHAR(20),
    is_official BOOLEAN DEFAULT false,
    is_verified BOOLEAN DEFAULT false,
    installation_count INTEGER DEFAULT 0,
    rating_average DECIMAL(3,2) DEFAULT 0.0,
    rating_count INTEGER DEFAULT 0,
    plugin_provider VARCHAR(50) NOT NULL,
    plugin_type VARCHAR(50) NOT NULL,
    config_schema JSONB,  -- JSON Schema for configuration
    tags TEXT[],
    screenshots TEXT[],
    changelog TEXT,
    published_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

### Java Interface

```java
public interface Industrial CloudPlugin {
    /**
     * Plugin metadata
     */
    String getKey();
    String getName();
    String getVersion();
    PluginCategory getCategory();

    /**
     * Lifecycle methods
     */
    void initialize(JsonNode configuration) throws PluginException;
    void start() throws PluginException;
    void stop() throws PluginException;
    void destroy();

    /**
     * Configuration
     */
    JsonNode getDefaultConfiguration();
    void updateConfiguration(JsonNode configuration) throws PluginException;
    boolean validateConfiguration(JsonNode configuration);

    /**
     * Health check
     */
    PluginHealthStatus getHealthStatus();
}
```

---

## Configuration Schema

Plugins use JSON Schema to define their configuration UI and validation.

### Basic Example

```json
{
  "type": "object",
  "required": ["apiKey", "region"],
  "properties": {
    "apiKey": {
      "type": "string",
      "title": "API Key",
      "description": "Your API key for authentication",
      "format": "password"
    },
    "region": {
      "type": "string",
      "title": "Region",
      "description": "Service region",
      "enum": ["us-east-1", "eu-west-1", "ap-southeast-1"],
      "default": "us-east-1"
    },
    "enabled": {
      "type": "boolean",
      "title": "Enabled",
      "description": "Enable this integration",
      "default": true
    },
    "timeout": {
      "type": "integer",
      "title": "Timeout (seconds)",
      "description": "Request timeout",
      "minimum": 1,
      "maximum": 300,
      "default": 30
    }
  }
}
```

### Supported Field Types

| Type | Format | Description | Example |
|------|--------|-------------|---------|
| `string` | `text` | Short text input | Name, ID |
| `string` | `textarea` | Long text input | Description, JSON |
| `string` | `password` | Password/secret | API keys, tokens |
| `string` | `email` | Email address | user@example.com |
| `string` | `url` | URL | https://example.com |
| `number` | - | Decimal number | 3.14, 42.0 |
| `integer` | - | Whole number | 1, 100, -5 |
| `boolean` | - | Checkbox | true, false |
| `string` (enum) | - | Dropdown select | One of defined values |
| `object` | - | Nested object | Complex structure |
| `array` | - | List of values | [1, 2, 3] |

### Advanced Schema Features

**Nested Objects:**
```json
{
  "type": "object",
  "properties": {
    "connection": {
      "type": "object",
      "title": "Connection Settings",
      "properties": {
        "host": { "type": "string" },
        "port": { "type": "integer" }
      }
    }
  }
}
```

**Conditional Fields:**
```json
{
  "type": "object",
  "properties": {
    "authType": {
      "type": "string",
      "enum": ["none", "basic", "oauth"]
    },
    "username": {
      "type": "string",
      "title": "Username",
      "description": "Required for basic auth"
    }
  },
  "dependencies": {
    "authType": {
      "oneOf": [
        {
          "properties": {
            "authType": { "enum": ["basic"] }
          },
          "required": ["username", "password"]
        }
      ]
    }
  }
}
```

---

## Creating Your First Plugin

### Step 1: Define Plugin Metadata

Create a plugin registry entry:

```sql
INSERT INTO plugin_registry (
    plugin_key, name, description, category, version, author,
    is_official, is_verified,
    plugin_provider, plugin_type, config_schema, tags
) VALUES (
    'my-custom-plugin',
    'My Custom Plugin',
    'Description of what your plugin does',
    'INTEGRATION',  -- or NOTIFICATION, PROTOCOL_PARSER, etc.
    '1.0.0',
    'Your Name',
    false,  -- Official plugins only by Industrial Cloud team
    false,  -- Verified by Industrial Cloud team
    'CUSTOM_PARSER',
    'INTEGRATION',
    '{
        "type": "object",
        "required": ["apiKey"],
        "properties": {
            "apiKey": {
                "type": "string",
                "title": "API Key",
                "format": "password"
            }
        }
    }',
    ARRAY['custom', 'integration']
);
```

### Step 2: Implement Plugin Logic

**For Protocol Parsers:**

```java
@Component
public class MyProtocolParser implements ProtocolParser {

    @Override
    public TelemetryData parse(byte[] payload, JsonNode config) {
        // Decode binary payload
        String apiKey = config.get("apiKey").asText();

        // Your parsing logic here
        Map<String, Object> variables = new HashMap<>();
        variables.put("temperature", extractTemperature(payload));
        variables.put("humidity", extractHumidity(payload));

        return new TelemetryData(variables);
    }

    @Override
    public boolean validatePayload(byte[] payload) {
        // Validate payload format
        return payload != null && payload.length > 0;
    }
}
```

**For Notification Channels:**

```java
@Component
public class MyNotificationChannel implements NotificationChannel {

    private final RestTemplate restTemplate;

    @Override
    public void sendNotification(Alert alert, JsonNode config) {
        String webhookUrl = config.get("webhookUrl").asText();

        // Format notification
        NotificationPayload payload = new NotificationPayload();
        payload.setTitle(alert.getRuleName());
        payload.setMessage(alert.getMessage());
        payload.setSeverity(alert.getSeverity());

        // Send to external service
        restTemplate.postForEntity(webhookUrl, payload, String.class);
    }

    @Override
    public boolean testConnection(JsonNode config) {
        try {
            String webhookUrl = config.get("webhookUrl").asText();
            restTemplate.getForEntity(webhookUrl, String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### Step 3: Register Plugin Provider

Add your plugin to `PluginProvider` enum:

```java
public enum PluginProvider {
    LORAWAN_TTN,
    HTTP_WEBHOOK,
    MODBUS_TCP,
    SIGFOX,
    MY_CUSTOM_PLUGIN  // Add your plugin here
}
```

### Step 4: Register with Spring

```java
@Configuration
public class PluginConfiguration {

    @Bean
    public PluginRegistry registerMyPlugin(
            MyProtocolParser parser,
            PluginRegistryService registryService) {

        // Register plugin on startup
        return registryService.registerPlugin(
            createPluginMetadata(parser)
        );
    }
}
```

---

## Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class MyPluginTest {

    @InjectMocks
    private MyProtocolParser parser;

    @Test
    void shouldParseValidPayload() {
        // Arrange
        byte[] payload = new byte[]{0x01, 0x02, 0x03};
        JsonNode config = createTestConfig();

        // Act
        TelemetryData result = parser.parse(payload, config);

        // Assert
        assertThat(result.getVariables()).containsKey("temperature");
        assertThat(result.getVariables().get("temperature")).isEqualTo(25.5);
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Transactional
class MyPluginIntegrationTest {

    @Autowired
    private PluginInstallationService installationService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    void shouldInstallAndActivatePlugin() {
        // Arrange
        Organization org = organizationRepository.findAll().get(0);
        JsonNode config = createPluginConfig();

        // Act
        InstalledPlugin installed = installationService.installPlugin(
            "my-custom-plugin", org, config
        );
        InstalledPlugin activated = installationService.activatePlugin(
            "my-custom-plugin", org
        );

        // Assert
        assertThat(activated.getStatus()).isEqualTo(PluginInstallationStatus.ACTIVE);
    }
}
```

---

## Publishing

### Community Plugin Submission

1. **Fork the Repository**
   ```bash
   git clone https://github.com/CodeFleck/indcloud.git
   cd indcloud
   git checkout -b plugin/my-custom-plugin
   ```

2. **Add Plugin Code**
   - Place plugin code in `src/main/java/org/indcloud/plugins/community/`
   - Add tests in `src/test/java/org/indcloud/plugins/community/`

3. **Create Migration**
   ```sql
   -- V52__Add_my_custom_plugin.sql
   INSERT INTO plugin_registry (...) VALUES (...);
   ```

4. **Document Your Plugin**
   Create `docs/plugins/MY_PLUGIN.md` with:
   - Overview and features
   - Configuration guide
   - API reference
   - Examples

5. **Submit Pull Request**
   ```bash
   git add .
   git commit -m "Add My Custom Plugin"
   git push origin plugin/my-custom-plugin
   ```

   Then open a PR at: https://github.com/CodeFleck/indcloud/pulls

### Official Plugin Criteria

To become an **official plugin** (with the blue badge):
- ✅ Code review passed
- ✅ Comprehensive tests (>80% coverage)
- ✅ Complete documentation
- ✅ Security audit passed
- ✅ Performance benchmarks met
- ✅ Follows coding standards

To become a **verified plugin** (with the green checkmark):
- ✅ Official plugin criteria met
- ✅ Used in production by 10+ organizations
- ✅ Average rating >4.0 stars
- ✅ Active maintenance (< 30 days since last update)

---

## Best Practices

### Security

1. **Never log sensitive data** (API keys, tokens, passwords)
   ```java
   // ❌ BAD
   logger.info("API Key: " + apiKey);

   // ✅ GOOD
   logger.info("Connecting with provided credentials");
   ```

2. **Validate all input**
   ```java
   if (config.get("apiKey") == null || config.get("apiKey").asText().isEmpty()) {
       throw new PluginConfigurationException("API Key is required");
   }
   ```

3. **Use HTTPS for external calls**
   ```java
   if (!url.startsWith("https://")) {
       throw new PluginConfigurationException("Only HTTPS URLs are allowed");
   }
   ```

### Performance

1. **Use connection pooling** for HTTP clients
2. **Implement caching** for frequently accessed data
3. **Set reasonable timeouts** (default: 30 seconds)
4. **Handle rate limiting** gracefully
5. **Use async processing** for heavy operations

### Error Handling

```java
try {
    // Plugin logic
} catch (HttpClientErrorException e) {
    logger.error("HTTP error communicating with external service", e);
    throw new PluginExecutionException("Failed to send notification", e);
} catch (Exception e) {
    logger.error("Unexpected error in plugin", e);
    throw new PluginExecutionException("Plugin execution failed", e);
}
```

### Configuration

1. **Provide sensible defaults**
2. **Validate configuration on save**
3. **Support configuration migration** for version updates
4. **Document all configuration fields**

### Documentation

Each plugin should include:
- **README.md** - Quick start guide
- **CONFIGURATION.md** - Detailed configuration reference
- **EXAMPLES.md** - Real-world usage examples
- **TROUBLESHOOTING.md** - Common issues and solutions
- **CHANGELOG.md** - Version history

---

## Example: Complete Slack Plugin

See the full implementation in the repository:
- Code: `src/main/java/org/indcloud/plugins/slack/`
- Tests: `src/test/java/org/indcloud/plugins/slack/`
- Docs: `docs/plugins/SLACK.md`

Key files:
- `SlackNotificationPlugin.java` - Main plugin class
- `SlackWebhookClient.java` - Slack API client
- `SlackMessageFormatter.java` - Message formatting
- `SlackPluginTest.java` - Unit tests
- `SlackPluginIntegrationTest.java` - Integration tests

---

## Support

- **Documentation**: https://github.com/CodeFleck/indcloud/tree/main/docs
- **Issues**: https://github.com/CodeFleck/indcloud/issues
- **Discussions**: https://github.com/CodeFleck/indcloud/discussions
- **Email**: support@indcloud.io

---

## License

Plugins inherit the Industrial Cloud license. Community plugins may use compatible open-source licenses.

---

**Happy Plugin Development!** 🚀
