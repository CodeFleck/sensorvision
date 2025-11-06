# SensorVision Java SDK

[![Maven Central](https://img.shields.io/maven-central/v/io.sensorvision/sensorvision-sdk.svg)](https://central.sonatype.com/artifact/io.sensorvision/sensorvision-sdk)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

Official Java SDK for [SensorVision](https://github.com/CodeFleck/sensorvision) - The IoT platform that scales with you.

Build enterprise IoT applications with type-safe Java APIs, automatic retry logic, and comprehensive error handling. Perfect for industrial automation, Android IoT devices, and enterprise Java applications.

## Features

- 🚀 **Simple API** - Clean, fluent builder pattern for easy configuration
- 📡 **Automatic Retry** - Configurable exponential backoff for network resilience
- 🛡️ **Type Safety** - Full Java type safety with comprehensive exception handling
- ⚡ **Modern Java** - Built for Java 17+ with modern best practices
- 📊 **Logging** - SLF4J integration for flexible logging
- 🧪 **Well Tested** - Comprehensive test suite with MockWebServer
- 📦 **Minimal Dependencies** - OkHttp + Jackson for HTTP and JSON

## Installation

### Maven

```xml
<dependency>
    <groupId>io.sensorvision</groupId>
    <artifactId>sensorvision-sdk</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

```gradle
implementation 'io.sensorvision:sensorvision-sdk:0.1.0'
```

### Manual Installation

Download the latest JAR from the [releases page](https://github.com/CodeFleck/sensorvision/releases).

## Quick Start

```java
import io.sensorvision.sdk.SensorVisionClient;
import io.sensorvision.sdk.model.IngestionResponse;

import java.util.Map;

public class Example {
    public static void main(String[] args) {
        // Create client
        SensorVisionClient client = new SensorVisionClient.Builder()
            .apiUrl("http://localhost:8080")
            .apiKey("your-device-token")
            .build();

        // Send telemetry data
        try {
            Map<String, Object> data = Map.of(
                "temperature", 23.5,
                "humidity", 65.2,
                "pressure", 1013.25
            );

            IngestionResponse response = client.sendData("sensor-001", data);
            System.out.println(response.getMessage()); // "Data ingested successfully"
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Configuration

### Builder Options

```java
SensorVisionClient client = new SensorVisionClient.Builder()
    .apiUrl("http://localhost:8080")          // Required: API base URL
    .apiKey("your-device-token")              // Required: Device authentication token
    .timeout(30000)                           // Optional: Request timeout (ms), default: 30000
    .retryAttempts(3)                         // Optional: Number of retries, default: 3
    .retryDelay(1000)                         // Optional: Delay between retries (ms), default: 1000
    .build();
```

## API Reference

### SensorVisionClient

#### `sendData(String deviceId, Map<String, Object> data)`

Send telemetry data for a device.

**Parameters:**
- `deviceId` - Unique identifier for the device (max 255 characters)
- `data` - Map of variable names to numeric/boolean values

**Returns:** `IngestionResponse` containing success message and metadata

**Throws:**
- `ValidationException` - If device ID or data is invalid
- `AuthenticationException` - If API key is invalid (401)
- `NetworkException` - If network request fails
- `RateLimitException` - If rate limit is exceeded (429)
- `ServerException` - If server returns 5xx error

**Example:**

```java
Map<String, Object> data = Map.of(
    "temperature", 23.5,
    "humidity", 65.2,
    "is_online", true
);

IngestionResponse response = client.sendData("weather-station", data);
System.out.println(response.getDeviceId());  // "weather-station"
System.out.println(response.getTimestamp()); // "2024-01-15T10:30:00Z"
```

## Examples

### Continuous Monitoring

```java
import io.sensorvision.sdk.SensorVisionClient;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ContinuousMonitoring {
    public static void main(String[] args) {
        SensorVisionClient client = new SensorVisionClient.Builder()
            .apiUrl("http://localhost:8080")
            .apiKey("your-device-token")
            .build();

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        // Send data every 60 seconds
        executor.scheduleAtFixedRate(() -> {
            try {
                Map<String, Object> data = Map.of(
                    "temperature", readTemperature(),
                    "humidity", readHumidity(),
                    "pressure", readPressure()
                );

                client.sendData("weather-station", data);
                System.out.println("Data sent successfully");
            } catch (Exception e) {
                System.err.println("Failed to send data: " + e.getMessage());
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    private static double readTemperature() {
        // Your sensor reading logic
        return 23.5;
    }

    private static double readHumidity() {
        return 65.2;
    }

    private static double readPressure() {
        return 1013.25;
    }
}
```

### Error Handling

```java
import io.sensorvision.sdk.SensorVisionClient;
import io.sensorvision.sdk.exception.*;

import java.util.Map;

public class ErrorHandlingExample {
    public static void main(String[] args) {
        SensorVisionClient client = new SensorVisionClient.Builder()
            .apiUrl("http://localhost:8080")
            .apiKey("your-device-token")
            .build();

        Map<String, Object> data = Map.of("temperature", 23.5);

        try {
            client.sendData("sensor-001", data);
        } catch (AuthenticationException e) {
            System.err.println("Invalid API key: " + e.getMessage());
        } catch (ValidationException e) {
            System.err.println("Invalid data format: " + e.getMessage());
        } catch (NetworkException e) {
            System.err.println("Network connection failed: " + e.getMessage());
        } catch (RateLimitException e) {
            System.err.println("Rate limit exceeded, please slow down");
        } catch (ServerException e) {
            System.err.println("Server error: " + e.getMessage());
        } catch (SensorVisionException e) {
            System.err.println("Unexpected error: " + e.getMessage());
        }
    }
}
```

### Spring Boot Integration

```java
import io.sensorvision.sdk.SensorVisionClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class SensorVisionConfig {

    @Bean
    public SensorVisionClient sensorVisionClient(
        @Value("${sensorvision.api.url}") String apiUrl,
        @Value("${sensorvision.api.key}") String apiKey
    ) {
        return new SensorVisionClient.Builder()
            .apiUrl(apiUrl)
            .apiKey(apiKey)
            .timeout(30000)
            .retryAttempts(3)
            .build();
    }
}
```

**application.properties:**
```properties
sensorvision.api.url=http://localhost:8080
sensorvision.api.key=your-device-token
```

### Android IoT Device

```java
import io.sensorvision.sdk.SensorVisionClient;
import android.os.AsyncTask;

import java.util.Map;

public class SensorDataTask extends AsyncTask<Void, Void, Void> {
    private final SensorVisionClient client;

    public SensorDataTask() {
        this.client = new SensorVisionClient.Builder()
            .apiUrl("http://your-server:8080")
            .apiKey("your-device-token")
            .build();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            Map<String, Object> data = Map.of(
                "temperature", 23.5,
                "battery_level", 85,
                "is_online", true
            );

            client.sendData("android-device-001", data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
```

## Testing

Run the test suite:

```bash
mvn test
```

Run with coverage:

```bash
mvn test jacoco:report
```

## Building from Source

```bash
# Clone the repository
git clone https://github.com/CodeFleck/sensorvision.git
cd sensorvision/sensorvision-sdk-java

# Build the project
mvn clean install

# Run tests
mvn test

# Generate Javadoc
mvn javadoc:javadoc
```

## Dependencies

- **OkHttp 4.12.0** - HTTP client
- **Jackson 2.16.1** - JSON serialization
- **SLF4J 2.0.9** - Logging facade
- **JUnit 5.10.1** - Testing framework (test scope)

## Requirements

- Java 17 or higher
- Maven 3.6+ or Gradle 7+ (for building from source)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.

## Support

- **Documentation**: [GitHub Wiki](https://github.com/CodeFleck/sensorvision/wiki)
- **Issues**: [GitHub Issues](https://github.com/CodeFleck/sensorvision/issues)
- **Repository**: [GitHub](https://github.com/CodeFleck/sensorvision)

## Related Projects

- [SensorVision](https://github.com/CodeFleck/sensorvision) - Main IoT monitoring platform
- [Python SDK](../sensorvision-sdk/) - Python SDK for SensorVision
- [JavaScript SDK](../sensorvision-sdk-js/) - JavaScript/TypeScript SDK for SensorVision

## Changelog

### Version 0.1.0 (Initial Release)

- HTTP client with OkHttp
- Automatic retry logic with configurable attempts and delay
- Comprehensive error handling (Authentication, Validation, Network, RateLimit, Server)
- Type-safe Java API with builder pattern
- Full test suite with MockWebServer
- SLF4J logging integration
- Java 17+ support
