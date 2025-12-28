import io.indcloud.sdk.IndCloudClient;
import io.indcloud.sdk.model.IngestionResponse;

import java.util.Map;

/**
 * Basic example demonstrating Industrial Cloud Java SDK usage.
 *
 * Run with: java -cp target/indcloud-sdk-0.1.0.jar:. BasicExample
 */
public class BasicExample {
    public static void main(String[] args) {
        // Create client
        IndCloudClient client = new IndCloudClient.Builder()
            .apiUrl("http://localhost:8080")
            .apiKey("your-device-token-here")
            .timeout(30000)
            .retryAttempts(3)
            .build();

        // Prepare telemetry data
        Map<String, Object> data = Map.of(
            "temperature", 23.5,
            "humidity", 65.2,
            "pressure", 1013.25,
            "is_online", true
        );

        // Send data
        try {
            IngestionResponse response = client.sendData("sensor-001", data);
            System.out.println("✓ Success: " + response.getMessage());
            System.out.println("  Device ID: " + response.getDeviceId());
            System.out.println("  Timestamp: " + response.getTimestamp());
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
