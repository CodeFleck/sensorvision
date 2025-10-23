#!/usr/bin/env python3
"""
SensorVision Raspberry Pi GPIO Sensor Example

Read sensors connected to Raspberry Pi GPIO pins and send data to SensorVision.

Hardware Example:
- DHT22 temperature/humidity sensor on GPIO 4
- Analog light sensor via MCP3008 ADC (SPI)
- Digital motion sensor (PIR) on GPIO 17

Requirements:
- Raspberry Pi (any model with GPIO)
- Python 3.7+
- Sensors connected to GPIO pins
"""

import requests
import time
import sys
from datetime import datetime

# Try to import sensor libraries (install if needed)
try:
    import Adafruit_DHT
    DHT_AVAILABLE = True
except ImportError:
    DHT_AVAILABLE = False
    print("⚠️  Adafruit_DHT library not installed")
    print("   Install: pip3 install Adafruit_DHT")

try:
    import RPi.GPIO as GPIO
    GPIO_AVAILABLE = True
except ImportError:
    GPIO_AVAILABLE = False
    print("⚠️  RPi.GPIO library not installed")
    print("   Install: pip3 install RPi.GPIO")

# ============================================
# CONFIGURATION - UPDATE THESE VALUES
# ============================================

# SensorVision API configuration
API_URL = "http://localhost:8080/api/v1/ingest"
DEVICE_ID = "raspi-gpio-001"  # Your device ID
API_KEY = "YOUR_DEVICE_API_KEY"  # Get from SensorVision dashboard (Key icon 🔑)

# GPIO Pin Configuration
DHT_PIN = 4  # GPIO pin for DHT22 sensor
PIR_PIN = 17  # GPIO pin for PIR motion sensor

# Sensor type (DHT11, DHT22, or AM2302)
DHT_SENSOR_TYPE = Adafruit_DHT.DHT22 if DHT_AVAILABLE else None

# Data send interval (seconds)
SEND_INTERVAL = 60  # Send every 60 seconds

# ============================================
# END CONFIGURATION
# ============================================


class RaspberryPiSensor:
    """Raspberry Pi sensor reader with GPIO"""

    def __init__(self):
        self.motion_detected = False
        self.setup_gpio()

    def setup_gpio(self):
        """Initialize GPIO pins"""
        if not GPIO_AVAILABLE:
            return

        GPIO.setmode(GPIO.BCM)
        GPIO.setwarnings(False)

        # Setup PIR motion sensor as input
        GPIO.setup(PIR_PIN, GPIO.IN)
        print(f"✓ GPIO initialized (PIR on GPIO {PIR_PIN})")

    def read_dht22(self):
        """Read DHT22 temperature and humidity sensor"""
        if not DHT_AVAILABLE:
            return None, None

        try:
            humidity, temperature = Adafruit_DHT.read_retry(
                DHT_SENSOR_TYPE,
                DHT_PIN,
                retries=3,
                delay_seconds=2
            )

            if humidity is not None and temperature is not None:
                return round(temperature, 2), round(humidity, 2)
            else:
                print("✗ Failed to read DHT22 sensor")
                return None, None

        except Exception as e:
            print(f"Error reading DHT22: {e}")
            return None, None

    def read_pir_motion(self):
        """Read PIR motion sensor"""
        if not GPIO_AVAILABLE:
            return 0

        try:
            # Read current state
            current_state = GPIO.input(PIR_PIN)

            if current_state == GPIO.HIGH:
                self.motion_detected = True
                return 1  # Motion detected
            else:
                motion_value = 1 if self.motion_detected else 0
                self.motion_detected = False  # Reset flag
                return motion_value

        except Exception as e:
            print(f"Error reading PIR sensor: {e}")
            return 0

    def read_cpu_temperature(self):
        """Read Raspberry Pi CPU temperature"""
        try:
            with open('/sys/class/thermal/thermal_zone0/temp', 'r') as f:
                temp = float(f.read()) / 1000.0
                return round(temp, 2)
        except Exception as e:
            print(f"Error reading CPU temperature: {e}")
            return None

    def read_all_sensors(self):
        """Read all sensors and return data dictionary"""
        data = {}

        # DHT22 temperature and humidity
        if DHT_AVAILABLE:
            temperature, humidity = self.read_dht22()
            if temperature is not None:
                data['temperature'] = temperature
            if humidity is not None:
                data['humidity'] = humidity
        else:
            print("DHT22 sensor not available (library not installed)")

        # PIR motion sensor
        if GPIO_AVAILABLE:
            motion = self.read_pir_motion()
            data['motion_detected'] = motion

        # CPU temperature (always available on Raspberry Pi)
        cpu_temp = self.read_cpu_temperature()
        if cpu_temp is not None:
            data['cpu_temperature'] = cpu_temp

        return data

    def cleanup(self):
        """Clean up GPIO on exit"""
        if GPIO_AVAILABLE:
            GPIO.cleanup()
            print("✓ GPIO cleaned up")


class SensorVisionClient:
    """Simple client for sending data to SensorVision"""

    def __init__(self, api_url, device_id, api_key):
        self.api_url = api_url
        self.device_id = device_id
        self.api_key = api_key
        self.session = requests.Session()
        self.session.headers.update({
            'X-API-Key': api_key,
            'Content-Type': 'application/json'
        })

    def send(self, data):
        """Send sensor data to SensorVision"""
        if not data:
            print("⚠️  No sensor data to send")
            return False

        url = f"{self.api_url}/{self.device_id}"

        try:
            response = self.session.post(url, json=data, timeout=10)

            if response.status_code == 200:
                result = response.json()
                print(f"✓ Data sent successfully: {result.get('message')}")
                return True
            else:
                print(f"✗ HTTP Error {response.status_code}: {response.text}")
                return False

        except requests.exceptions.ConnectionError:
            print(f"✗ Connection error: Cannot reach {url}")
            return False
        except requests.exceptions.Timeout:
            print("✗ Request timeout")
            return False
        except Exception as e:
            print(f"✗ Error sending data: {e}")
            return False


def main():
    """Main loop"""
    print("SensorVision Raspberry Pi GPIO Client")
    print("=" * 50)
    print(f"Device ID: {DEVICE_ID}")
    print(f"API URL: {API_URL}")
    print(f"Send interval: {SEND_INTERVAL} seconds")
    print(f"DHT22 Pin: GPIO {DHT_PIN}")
    print(f"PIR Pin: GPIO {PIR_PIN}")
    print("=" * 50)
    print()

    # Validate configuration
    if API_KEY == "YOUR_DEVICE_API_KEY":
        print("⚠️  WARNING: API_KEY not configured!")
        print("   Update API_KEY in the script with your device token")
        print("   Get it from SensorVision dashboard (Key icon 🔑)")
        sys.exit(1)

    # Check if at least one sensor library is available
    if not DHT_AVAILABLE and not GPIO_AVAILABLE:
        print("⚠️  No sensor libraries installed!")
        print("   Install at least one: pip3 install Adafruit_DHT RPi.GPIO")
        sys.exit(1)

    # Initialize
    sensor = RaspberryPiSensor()
    client = SensorVisionClient(API_URL, DEVICE_ID, API_KEY)

    print("Starting data collection... (Press Ctrl+C to stop)\n")

    try:
        while True:
            timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

            # Read all sensors
            data = sensor.read_all_sensors()

            if not data:
                print("No sensor data available, check sensor connections")
                time.sleep(10)
                continue

            # Display data
            print(f"[{timestamp}] Sensor readings:")
            for key, value in data.items():
                print(f"  {key}: {value}")

            # Send to SensorVision
            success = client.send(data)

            if success:
                print(f"Next send in {SEND_INTERVAL} seconds...\n")
            else:
                print("Retrying in 10 seconds...\n")
                time.sleep(10)
                continue

            # Wait for next interval
            time.sleep(SEND_INTERVAL)

    except KeyboardInterrupt:
        print("\n\n✓ Stopped by user")

    finally:
        sensor.cleanup()
        print("Thank you for using SensorVision!")


if __name__ == "__main__":
    main()
