/**
 * SensorVision ESP32 Temperature Sensor Example
 *
 * This example shows how to send temperature data to SensorVision
 * using the simplified HTTP ingestion endpoint.
 *
 * Hardware Required:
 * - ESP32 board
 * - DHT22 temperature/humidity sensor (or any analog temperature sensor)
 * - Connecting wires
 *
 * Wiring:
 * - DHT22 VCC -> ESP32 3.3V
 * - DHT22 GND -> ESP32 GND
 * - DHT22 DATA -> ESP32 GPIO 4
 *
 * Libraries Required:
 * - WiFi (built-in)
 * - HTTPClient (built-in)
 * - DHT sensor library by Adafruit (install via Library Manager)
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <DHT.h>

// ============================================
// CONFIGURATION - UPDATE THESE VALUES
// ============================================

// WiFi credentials
const char* WIFI_SSID = "YOUR_WIFI_SSID";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

// SensorVision API configuration
const char* API_URL = "http://YOUR_SERVER_IP:8080/api/v1/ingest";
const char* DEVICE_ID = "esp32-temp-001";  // Your device ID
const char* API_KEY = "YOUR_DEVICE_API_KEY";  // Get from SensorVision dashboard (Key icon 🔑)

// Sensor configuration
#define DHT_PIN 4  // GPIO pin connected to DHT22 data pin
#define DHT_TYPE DHT22  // DHT22 (AM2302)

// Data send interval (milliseconds)
const unsigned long SEND_INTERVAL = 60000;  // Send every 60 seconds

// ============================================
// END CONFIGURATION
// ============================================

DHT dht(DHT_PIN, DHT_TYPE);
HTTPClient http;
unsigned long lastSendTime = 0;

void setup() {
  Serial.begin(115200);
  delay(1000);

  Serial.println("\n\nSensorVision ESP32 Temperature Sensor");
  Serial.println("=====================================");

  // Initialize DHT sensor
  dht.begin();
  Serial.println("✓ DHT22 sensor initialized");

  // Connect to WiFi
  connectWiFi();
}

void loop() {
  // Check WiFi connection
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi disconnected! Reconnecting...");
    connectWiFi();
  }

  // Send data at specified interval
  unsigned long currentTime = millis();
  if (currentTime - lastSendTime >= SEND_INTERVAL) {
    lastSendTime = currentTime;
    sendSensorData();
  }

  delay(1000);  // Small delay to prevent CPU overload
}

void connectWiFi() {
  Serial.print("Connecting to WiFi: ");
  Serial.println(WIFI_SSID);

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 30) {
    delay(500);
    Serial.print(".");
    attempts++;
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n✓ WiFi connected!");
    Serial.print("IP address: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("\n✗ WiFi connection failed!");
  }
}

void sendSensorData() {
  // Read sensor data
  float temperature = dht.readTemperature();
  float humidity = dht.readHumidity();

  // Check if readings are valid
  if (isnan(temperature) || isnan(humidity)) {
    Serial.println("✗ Failed to read from DHT sensor!");
    return;
  }

  Serial.println("\n--- Sending Sensor Data ---");
  Serial.print("Temperature: ");
  Serial.print(temperature);
  Serial.println(" °C");
  Serial.print("Humidity: ");
  Serial.print(humidity);
  Serial.println(" %");

  // Build JSON payload (simple format!)
  String payload = "{";
  payload += "\"temperature\":" + String(temperature, 2) + ",";
  payload += "\"humidity\":" + String(humidity, 2);
  payload += "}";

  // Build full URL
  String url = String(API_URL) + "/" + DEVICE_ID;

  // Send HTTP POST request
  http.begin(url);
  http.addHeader("X-API-Key", API_KEY);
  http.addHeader("Content-Type", "application/json");

  int httpResponseCode = http.POST(payload);

  // Check response
  if (httpResponseCode > 0) {
    String response = http.getString();
    Serial.print("✓ HTTP Response code: ");
    Serial.println(httpResponseCode);
    Serial.print("Response: ");
    Serial.println(response);
  } else {
    Serial.print("✗ HTTP Error code: ");
    Serial.println(httpResponseCode);
    Serial.print("Error: ");
    Serial.println(http.errorToString(httpResponseCode));
  }

  http.end();
  Serial.println("---------------------------\n");
}
