# Python Sensor Client Integration

Send sensor data from Python to SensorVision. Works on Raspberry Pi, Linux, macOS, and Windows.

## Requirements

- Python 3.7 or higher
- `requests` library
- (Optional) Sensor-specific libraries

## Quick Start

### 1. Install Dependencies

```bash
# Install requests library
pip install requests

# OR use the provided requirements.txt
pip install -r requirements.txt
```

### 2. Get Your API Key

1. Register at your SensorVision instance: http://your-server:3001/register
2. Login to the dashboard
3. Create a device (or use existing)
4. Click the **Key icon (🔑)** next to your device
5. Copy the UUID token

### 3. Configure the Script

Edit `sensor_client.py` and update these values:

```python
# SensorVision API configuration
API_URL = "http://192.168.1.100:8080/api/v1/ingest"  # Your server IP
DEVICE_ID = "python-sensor-001"  # Your device ID
API_KEY = "550e8400-e29b-41d4-a716-446655440000"  # From dashboard

# Data send interval (seconds)
SEND_INTERVAL = 60  # Send every 60 seconds

# Simulation mode (set to False when using real sensors)
SIMULATION_MODE = True
```

### 4. Run the Script

```bash
python sensor_client.py
```

## Expected Output

```
SensorVision Python Client
==================================================
Device ID: python-sensor-001
API URL: http://localhost:8080/api/v1/ingest
Send interval: 60 seconds
Mode: SIMULATION
==================================================

Starting data collection... (Press Ctrl+C to stop)

[2025-10-21 14:30:00] Sensor readings:
  temperature: 22.45
  humidity: 65.30
  pressure: 1013.25
  light_intensity: 450.50
✓ Data sent successfully: Data received successfully
Next send in 60 seconds...
```

## Using Real Sensors

### Example: DHT22 on Raspberry Pi

1. **Install library:**
   ```bash
   pip install Adafruit_DHT
   ```

2. **Update code:**
   ```python
   import Adafruit_DHT

   def read_real_sensors():
       humidity, temperature = Adafruit_DHT.read_retry(Adafruit_DHT.DHT22, 4)
       return {
           "temperature": temperature,
           "humidity": humidity
       }
   ```

3. **Set simulation mode to False:**
   ```python
   SIMULATION_MODE = False
   ```

### Example: DS18B20 Temperature Sensor

1. **Install library:**
   ```bash
   pip install w1thermsensor
   ```

2. **Update code:**
   ```python
   from w1thermsensor import W1ThermSensor

   def read_real_sensors():
       sensor = W1ThermSensor()
       temperature = sensor.get_temperature()
       return {"temperature": temperature}
   ```

### Example: BME280 (Temperature + Humidity + Pressure)

1. **Install library:**
   ```bash
   pip install RPi.bme280
   ```

2. **Update code:**
   ```python
   import smbus2
   import bme280

   port = 1
   address = 0x76
   bus = smbus2.SMBus(port)
   calibration_params = bme280.load_calibration_params(bus, address)

   def read_real_sensors():
       data = bme280.sample(bus, address, calibration_params)
       return {
           "temperature": data.temperature,
           "humidity": data.humidity,
           "pressure": data.pressure
       }
   ```

## Running as a System Service (Linux/Raspberry Pi)

### 1. Create systemd service file

```bash
sudo nano /etc/systemd/system/sensorvision-client.service
```

### 2. Add this content:

```ini
[Unit]
Description=SensorVision Python Client
After=network.target

[Service]
Type=simple
User=pi
WorkingDirectory=/home/pi/sensorvision-client
ExecStart=/usr/bin/python3 /home/pi/sensorvision-client/sensor_client.py
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 3. Enable and start service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable sensorvision-client
sudo systemctl start sensorvision-client

# Check status
sudo systemctl status sensorvision-client

# View logs
sudo journalctl -u sensorvision-client -f
```

## Running on Startup (Windows)

### Option 1: Task Scheduler

1. Open Task Scheduler
2. Create Basic Task
3. Trigger: "When the computer starts"
4. Action: "Start a program"
5. Program: `pythonw.exe`
6. Arguments: `C:\path\to\sensor_client.py`

### Option 2: Startup Folder

1. Create batch file `start_sensor.bat`:
   ```bat
   @echo off
   cd C:\path\to\sensorvision-client
   python sensor_client.py
   ```
2. Place in: `C:\Users\YourUser\AppData\Roaming\Microsoft\Windows\Start Menu\Programs\Startup`

## Advanced Usage

### Send Multiple Variables

```python
data = {
    "temperature": 23.5,
    "humidity": 65.2,
    "pressure": 1013.25,
    "light": 450.5,
    "battery_voltage": 3.7,
    "rssi": -67
}

client.send(data)
```

### Error Handling and Retries

```python
MAX_RETRIES = 3

for attempt in range(MAX_RETRIES):
    if client.send(data):
        break
    else:
        print(f"Retry {attempt + 1}/{MAX_RETRIES}")
        time.sleep(5)
```

### Batch Sending (Store and Forward)

```python
import json

# Store data when offline
def save_offline_data(data):
    with open('offline_data.json', 'a') as f:
        json.dump(data, f)
        f.write('\n')

# Send stored data when online
def send_offline_data(client):
    if not os.path.exists('offline_data.json'):
        return

    with open('offline_data.json', 'r') as f:
        for line in f:
            data = json.loads(line)
            client.send(data)

    # Clear file after sending
    os.remove('offline_data.json')
```

## Troubleshooting

### Connection Refused
- Ensure SensorVision backend is running
- Check API_URL is correct (IP and port)
- Verify network connectivity: `ping your-server-ip`

### HTTP 401 Unauthorized
- Verify API_KEY is correct UUID from dashboard
- Ensure you copied the full token (36 characters)
- Check token hasn't been revoked in dashboard

### Permission Denied (Raspberry Pi GPIO)
- Run with sudo for GPIO access: `sudo python sensor_client.py`
- OR add user to gpio group: `sudo usermod -a -G gpio $USER`

### Module Not Found
- Ensure you're using correct Python version: `python3 --version`
- Reinstall dependencies: `pip3 install -r requirements.txt`
- Check virtual environment is activated

## Next Steps

- View live data in dashboard: http://your-server:3001/dashboard
- Set up alerts for sensor thresholds
- Create custom dashboards with widgets
- Deploy multiple sensors across different locations

## Support

- [SensorVision Documentation](https://github.com/CodeFleck/sensorvision)
- [Requests Library Docs](https://requests.readthedocs.io/)
- [Raspberry Pi GPIO](https://www.raspberrypi.com/documentation/computers/raspberry-pi.html)
