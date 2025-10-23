# SensorVision Integration Templates

Ready-to-use code templates for integrating sensors and devices with SensorVision.

## Available Templates

| Template | Platform | Sensors | Difficulty | Setup Time |
|----------|----------|---------|------------|------------|
| **[ESP32 Temperature](./esp32-temperature/)** | ESP32/ESP8266 | DHT22 | ⭐⭐ Easy | 15 minutes |
| **[Python Sensor](./python-sensor/)** | Python 3.7+ | Simulated/Any | ⭐ Very Easy | 5 minutes |
| **[Raspberry Pi GPIO](./raspberry-pi-gpio/)** | Raspberry Pi | DHT22, PIR, CPU | ⭐⭐⭐ Moderate | 20 minutes |

## Quick Start

### 1. Choose Your Platform

Select the template that matches your hardware:

- **ESP32/Arduino** → Use `esp32-temperature/` for microcontrollers
- **Python/PC** → Use `python-sensor/` for computers or simulation
- **Raspberry Pi** → Use `raspberry-pi-gpio/` for GPIO sensors

### 2. Get Your API Key

1. Register at your SensorVision instance
2. Login to the dashboard
3. Create a device
4. Click the **Key icon (🔑)** to get your token

### 3. Follow Template Instructions

Each template directory contains:
- **Complete source code** ready to run
- **README.md** with detailed setup instructions
- **Wiring diagrams** (where applicable)
- **Troubleshooting guide**

### 4. Deploy and Monitor

- Upload code to your device
- Watch live data in the SensorVision dashboard
- Set up alerts and custom visualizations

## Template Details

### ESP32 Temperature

**What it does:**
- Reads temperature and humidity from DHT22 sensor
- Sends data via WiFi every 60 seconds
- Perfect for home automation projects

**Hardware:**
- ESP32 development board (~$5)
- DHT22 temperature sensor (~$3)
- USB cable for programming

**Languages:** C++ (Arduino IDE)

[→ Go to ESP32 template](./esp32-temperature/)

---

### Python Sensor

**What it does:**
- Sends simulated or real sensor data
- Works on any computer or Raspberry Pi
- Great for testing and prototyping

**Requirements:**
- Python 3.7 or higher
- `requests` library
- (Optional) Sensor-specific libraries

**Languages:** Python 3

[→ Go to Python template](./python-sensor/)

---

### Raspberry Pi GPIO

**What it does:**
- Reads multiple sensors via GPIO pins
- Supports DHT22, PIR motion, DS18B20, etc.
- Includes systemd service for autostart

**Hardware:**
- Raspberry Pi (any model)
- DHT22 sensor
- PIR motion sensor
- Connecting wires

**Languages:** Python 3

[→ Go to Raspberry Pi template](./raspberry-pi-gpio/)

---

## Common Setup Steps

### 1. Clone or Download Template

```bash
# Clone entire repository
git clone https://github.com/CodeFleck/sensorvision.git
cd sensorvision/integration-templates

# Or download specific template
# Click "Download ZIP" for the template you need
```

### 2. Install Dependencies

**ESP32/Arduino:**
```
Install via Arduino IDE Library Manager
```

**Python:**
```bash
cd python-sensor  # or raspberry-pi-gpio
pip install -r requirements.txt
```

### 3. Configure API Credentials

Edit the configuration section in each template:

```python
API_URL = "http://your-server:8080/api/v1/ingest"
DEVICE_ID = "your-device-id"
API_KEY = "your-api-key-from-dashboard"
```

### 4. Run

**ESP32:** Upload via Arduino IDE
**Python:** `python3 script_name.py`

## Features

✅ **Copy-paste ready** - Minimal configuration required
✅ **Well documented** - Extensive comments and README
✅ **Production ready** - Error handling and retries
✅ **Tested** - Verified on real hardware
✅ **Extensible** - Easy to add more sensors

## Advanced Usage

### Multiple Devices

Run multiple instances with different `DEVICE_ID` values:

```bash
# Terminal 1
python3 sensor_client.py  # device-001

# Terminal 2
DEVICE_ID=device-002 python3 sensor_client.py
```

### Custom Sensors

Add your sensor reading logic:

```python
def read_custom_sensor():
    value = read_from_hardware()
    return {"my_variable": value}

# Send to SensorVision
client.send(read_custom_sensor())
```

### Batch Data

Send multiple variables in one request:

```python
data = {
    "temperature": 23.5,
    "humidity": 65.2,
    "pressure": 1013.25,
    "light": 450.5,
    "battery": 3.7
}
client.send(data)
```

## Troubleshooting

### Connection Issues

```bash
# Test API connectivity
curl -H "X-API-Key: YOUR_KEY" \
  http://your-server:8080/api/v1/ingest/test-device \
  -d '{"test": 1}'
```

### Authentication Errors

- Verify API key is correct (36-character UUID)
- Check key hasn't been revoked in dashboard
- Ensure `X-API-Key` header is set

### No Data in Dashboard

- Check device ID matches dashboard
- Verify first data send completed successfully
- Refresh browser page
- Check backend logs for errors

## Support & Resources

- **Documentation:** [SensorVision GitHub](https://github.com/CodeFleck/sensorvision)
- **API Reference:** http://your-server:8080/swagger-ui.html
- **Issues:** [GitHub Issues](https://github.com/CodeFleck/sensorvision/issues)
- **Community:** [Discussions](https://github.com/CodeFleck/sensorvision/discussions)

## Contributing

Have a template for another platform? We'd love to include it!

1. Fork the repository
2. Create your template in a new directory
3. Include:
   - Complete source code
   - README with setup instructions
   - Example output
   - Troubleshooting guide
4. Submit a pull request

## License

These templates are provided under the MIT License. Use them freely in your projects!

---

**Need help?** Open an issue on GitHub or check the main SensorVision documentation.
