# ESP32 Temperature Sensor Integration

Send temperature and humidity data from an ESP32 with DHT22 sensor to SensorVision.

## Hardware Required

- ESP32 development board
- DHT22 (AM2302) temperature/humidity sensor
- Breadboard and jumper wires
- USB cable for programming

## Wiring Diagram

```
ESP32          DHT22 Sensor
======         ============
3.3V     -->   VCC (Pin 1)
GND      -->   GND (Pin 4)
GPIO 4   -->   DATA (Pin 2)

Note: Add 10kΩ pull-up resistor between DATA and VCC
```

## Setup Instructions

### 1. Install Arduino IDE

Download from: https://www.arduino.cc/en/software

### 2. Install ESP32 Board Support

1. Open Arduino IDE
2. Go to **File → Preferences**
3. Add this URL to "Additional Board Manager URLs":
   ```
   https://dl.espressif.com/dl/package_esp32_index.json
   ```
4. Go to **Tools → Board → Boards Manager**
5. Search for "ESP32" and install "ESP32 by Espressif Systems"

### 3. Install Required Libraries

1. Go to **Sketch → Include Library → Manage Libraries**
2. Search and install:
   - **DHT sensor library** by Adafruit
   - **Adafruit Unified Sensor** (dependency)

### 4. Configure the Sketch

Open `esp32-temperature.ino` and update these values:

```cpp
// WiFi credentials
const char* WIFI_SSID = "YOUR_WIFI_SSID";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

// SensorVision API configuration
const char* API_URL = "http://192.168.1.100:8080/api/v1/ingest";  // Your server IP
const char* DEVICE_ID = "esp32-temp-001";  // Unique device ID
const char* API_KEY = "550e8400-e29b-41d4-a716-446655440000";  // From SensorVision dashboard
```

### 5. Get Your API Key

1. Register at your SensorVision instance: http://your-server:3001/register
2. Login to the dashboard
3. Create a device (or use existing)
4. Click the **Key icon (🔑)** next to your device
5. Copy the UUID token

### 6. Upload to ESP32

1. Connect ESP32 via USB
2. Select **Tools → Board → ESP32 Dev Module**
3. Select correct **Port** under **Tools → Port**
4. Click **Upload** button (→)
5. Open **Serial Monitor** (115200 baud) to see output

## Expected Output

```
SensorVision ESP32 Temperature Sensor
=====================================
✓ DHT22 sensor initialized
Connecting to WiFi: MyNetwork
.....
✓ WiFi connected!
IP address: 192.168.1.50

--- Sending Sensor Data ---
Temperature: 23.45 °C
Humidity: 65.20 %
✓ HTTP Response code: 200
Response: {"success":true,"message":"Data received successfully"}
---------------------------
```

## Troubleshooting

### WiFi Connection Issues
- Verify SSID and password are correct
- Ensure 2.4GHz WiFi (ESP32 doesn't support 5GHz)
- Check WiFi signal strength

### Sensor Reading NaN
- Check wiring connections
- Verify 10kΩ pull-up resistor is installed
- Try different GPIO pin if using GPIO 2 or 12 (boot mode pins)

### HTTP Error 401 (Unauthorized)
- Verify API_KEY is correct UUID from dashboard
- Ensure you copied the full token (36 characters)

### HTTP Error 500 (Server Error)
- Check API_URL is correct (IP address and port)
- Ensure SensorVision backend is running
- Check server logs for errors

### Device Not Appearing in Dashboard
- First data send auto-creates the device
- Refresh browser after first successful send
- Check backend logs: `docker-compose logs sensorvision-app`

## Customization

### Add More Sensors

```cpp
// Read additional sensors
float pressure = bmp.readPressure() / 100.0;  // hPa

// Add to payload
String payload = "{";
payload += "\"temperature\":" + String(temperature, 2) + ",";
payload += "\"humidity\":" + String(humidity, 2) + ",";
payload += "\"pressure\":" + String(pressure, 2);
payload += "}";
```

### Battery Powered (Deep Sleep)

```cpp
#include <esp_sleep.h>

// Send data
sendSensorData();

// Sleep for 5 minutes
esp_sleep_enable_timer_wakeup(5 * 60 * 1000000ULL);  // microseconds
esp_deep_sleep_start();
```

### Change Send Interval

```cpp
const unsigned long SEND_INTERVAL = 30000;  // Send every 30 seconds
```

## Next Steps

- View live data in dashboard: http://your-server:3001/dashboard
- Set up alerts for temperature thresholds
- Create custom dashboards with widgets
- Add more sensors to the same device

## Support

- [SensorVision Documentation](https://github.com/CodeFleck/sensorvision)
- [ESP32 Arduino Core Docs](https://docs.espressif.com/projects/arduino-esp32/)
- [DHT22 Datasheet](https://www.sparkfun.com/datasheets/Sensors/Temperature/DHT22.pdf)
