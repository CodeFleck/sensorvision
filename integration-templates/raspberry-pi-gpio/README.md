# Raspberry Pi GPIO Sensors Integration

Send sensor data from Raspberry Pi GPIO pins to Industrial Cloud.

## Supported Sensors

- **DHT22** - Temperature and humidity
- **DHT11** - Temperature and humidity (lower accuracy)
- **PIR** - Motion detection
- **CPU Temperature** - Built-in Raspberry Pi sensor

## Hardware Requirements

- Raspberry Pi (any model with GPIO)
- DHT22 or DHT11 sensor
- PIR motion sensor (HC-SR501)
- Breadboard and jumper wires
- (Optional) 10kΩ pull-up resistor for DHT sensor

## Wiring Diagram

### DHT22 Sensor
```
Raspberry Pi    DHT22
============    =====
3.3V      -->   VCC (Pin 1)
GND       -->   GND (Pin 4)
GPIO 4    -->   DATA (Pin 2)

Note: Add 10kΩ pull-up resistor between DATA and VCC
```

### PIR Motion Sensor
```
Raspberry Pi    HC-SR501
============    ========
5V        -->   VCC
GND       -->   GND
GPIO 17   -->   OUT
```

## Setup Instructions

### 1. Enable Raspberry Pi Interfaces

```bash
sudo raspi-config
```

Go to:
- Interface Options → SPI → Enable (if using ADC)
- Interface Options → I2C → Enable (if using I2C sensors)

### 2. Update System

```bash
sudo apt-get update
sudo apt-get upgrade
```

### 3. Install Python and Dependencies

```bash
# Install Python 3 and pip
sudo apt-get install python3 python3-pip

# Install required libraries
pip3 install requests
pip3 install Adafruit_DHT
pip3 install RPi.GPIO
```

### 4. Get Your API Key

1. Register at your Industrial Cloud instance: http://your-server:3001/register
2. Login to the dashboard
3. Create a device (or use existing)
4. Click the **Key icon (🔑)** next to your device
5. Copy the UUID token

### 5. Configure the Script

Edit `gpio_sensor.py` and update these values:

```python
# Industrial Cloud API configuration
API_URL = "http://192.168.1.100:8080/api/v1/ingest"
DEVICE_ID = "raspi-gpio-001"
API_KEY = "550e8400-e29b-41d4-a716-446655440000"  # From dashboard

# GPIO Pin Configuration
DHT_PIN = 4  # GPIO pin for DHT22
PIR_PIN = 17  # GPIO pin for PIR motion sensor
```

### 6. Run the Script

```bash
# Run with sudo for GPIO access
sudo python3 gpio_sensor.py

# OR add user to gpio group (recommended)
sudo usermod -a -G gpio $USER
# Log out and log back in, then run without sudo:
python3 gpio_sensor.py
```

## Expected Output

```
Industrial Cloud Raspberry Pi GPIO Client
==================================================
Device ID: raspi-gpio-001
API URL: http://localhost:8080/api/v1/ingest
Send interval: 60 seconds
DHT22 Pin: GPIO 4
PIR Pin: GPIO 17
==================================================

✓ GPIO initialized (PIR on GPIO 17)
Starting data collection... (Press Ctrl+C to stop)

[2025-10-21 14:30:00] Sensor readings:
  temperature: 22.35
  humidity: 58.20
  motion_detected: 0
  cpu_temperature: 51.25
✓ Data sent successfully: Data received successfully
Next send in 60 seconds...
```

## Running as System Service

Create a systemd service to run automatically on boot.

### 1. Create service file

```bash
sudo nano /etc/systemd/system/indcloud-gpio.service
```

### 2. Add this content:

```ini
[Unit]
Description=Industrial Cloud GPIO Sensor Client
After=network.target

[Service]
Type=simple
User=pi
WorkingDirectory=/home/pi/indcloud-gpio
ExecStart=/usr/bin/python3 /home/pi/indcloud-gpio/gpio_sensor.py
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

### 3. Enable and start service:

```bash
# Reload systemd
sudo systemctl daemon-reload

# Enable service (start on boot)
sudo systemctl enable indcloud-gpio

# Start service now
sudo systemctl start indcloud-gpio

# Check status
sudo systemctl status indcloud-gpio

# View live logs
sudo journalctl -u indcloud-gpio -f

# Stop service
sudo systemctl stop indcloud-gpio
```

## Adding More Sensors

### DS18B20 Temperature Sensor (1-Wire)

1. **Enable 1-Wire interface:**
   ```bash
   sudo raspi-config
   # Interface Options → 1-Wire → Enable
   sudo reboot
   ```

2. **Install library:**
   ```bash
   pip3 install w1thermsensor
   ```

3. **Add to code:**
   ```python
   from w1thermsensor import W1ThermSensor

   def read_ds18b20(self):
       try:
           sensor = W1ThermSensor()
           temperature = sensor.get_temperature()
           return round(temperature, 2)
       except Exception as e:
           print(f"Error reading DS18B20: {e}")
           return None
   ```

### Analog Sensors via MCP3008 ADC

1. **Install library:**
   ```bash
   pip3 install adafruit-circuitpython-mcp3xxx
   ```

2. **Wire MCP3008:**
   ```
   MCP3008    Raspberry Pi
   =======    ============
   VDD   -->  3.3V
   VREF  -->  3.3V
   AGND  -->  GND
   DGND  -->  GND
   CLK   -->  GPIO 11 (SCLK)
   DOUT  -->  GPIO 9 (MISO)
   DIN   -->  GPIO 10 (MOSI)
   CS    -->  GPIO 8 (CE0)
   ```

3. **Add to code:**
   ```python
   import busio
   import digitalio
   import board
   import adafruit_mcp3xxx.mcp3008 as MCP
   from adafruit_mcp3xxx.analog_in import AnalogIn

   # Create SPI bus
   spi = busio.SPI(clock=board.SCK, MISO=board.MISO, MOSI=board.MOSI)
   cs = digitalio.DigitalInOut(board.D8)
   mcp = MCP.MCP3008(spi, cs)

   # Read analog channel 0
   channel = AnalogIn(mcp, MCP.P0)
   print(channel.value)  # 0-65535
   print(channel.voltage)  # 0-3.3V
   ```

### BME280 (Temperature, Humidity, Pressure) via I2C

1. **Install library:**
   ```bash
   pip3 install RPi.bme280 smbus2
   ```

2. **Add to code:**
   ```python
   import smbus2
   import bme280

   port = 1
   address = 0x76  # or 0x77
   bus = smbus2.SMBus(port)
   calibration_params = bme280.load_calibration_params(bus, address)

   def read_bme280(self):
       data = bme280.sample(bus, address, calibration_params)
       return {
           'temperature': round(data.temperature, 2),
           'humidity': round(data.humidity, 2),
           'pressure': round(data.pressure, 2)
       }
   ```

## Troubleshooting

### Permission Denied (GPIO)
```bash
# Option 1: Run with sudo
sudo python3 gpio_sensor.py

# Option 2: Add user to gpio group (recommended)
sudo usermod -a -G gpio $USER
# Log out and log back in
```

### DHT Sensor Returns None
- Check wiring connections
- Ensure 10kΩ pull-up resistor is installed
- Try different GPIO pin (avoid GPIO 2, 3, 14, 15)
- Verify sensor type (DHT11 vs DHT22)

### PIR Sensor Always Triggers
- Adjust sensitivity potentiometer on sensor
- Check trigger mode jumper (H = repeat, L = single)
- Verify 5V power supply

### Connection Refused
- Ensure Industrial Cloud backend is running
- Check API_URL and port
- Verify network connectivity: `ping your-server-ip`

### I2C/SPI Not Working
```bash
# Check interfaces are enabled
ls /dev/i2c* /dev/spidev*

# Install I2C tools
sudo apt-get install i2c-tools

# Scan I2C bus
sudo i2cdetect -y 1
```

## Best Practices

### Power Management
```python
# Reduce CPU load between readings
import time
time.sleep(SEND_INTERVAL)
```

### Error Recovery
```python
MAX_ERRORS = 5
error_count = 0

try:
    data = sensor.read_all_sensors()
    error_count = 0  # Reset on success
except Exception as e:
    error_count += 1
    if error_count >= MAX_ERRORS:
        print("Too many errors, restarting...")
        sys.exit(1)
```

### Graceful Shutdown
```python
import signal

def signal_handler(sig, frame):
    print("\nShutting down gracefully...")
    sensor.cleanup()
    sys.exit(0)

signal.signal(signal.SIGINT, signal_handler)
signal.signal(signal.SIGTERM, signal_handler)
```

## Next Steps

- View live data in dashboard: http://your-server:3001/dashboard
- Set up alerts for temperature thresholds
- Add motion detection alerts
- Create custom dashboards with gauges and charts

## Resources

- [Raspberry Pi GPIO Pinout](https://pinout.xyz/)
- [DHT22 Datasheet](https://www.sparkfun.com/datasheets/Sensors/Temperature/DHT22.pdf)
- [Industrial Cloud Documentation](https://github.com/CodeFleck/indcloud)
- [RPi.GPIO Documentation](https://sourceforge.net/p/raspberry-gpio-python/wiki/Home/)
