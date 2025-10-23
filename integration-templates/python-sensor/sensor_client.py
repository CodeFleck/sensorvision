#!/usr/bin/env python3
"""
SensorVision Python Client Example

Simple Python script to send sensor data to SensorVision.
Works on: Raspberry Pi, Linux, macOS, Windows

Usage:
    python sensor_client.py

Configuration:
    Edit the CONFIGURATION section below with your values.
"""

import requests
import time
import random
import sys
from datetime import datetime

# ============================================
# CONFIGURATION - UPDATE THESE VALUES
# ============================================

# SensorVision API configuration
API_URL = "http://localhost:8080/api/v1/ingest"
DEVICE_ID = "python-sensor-001"  # Your device ID
API_KEY = "YOUR_DEVICE_API_KEY"  # Get from SensorVision dashboard (Key icon 🔑)

# Data send interval (seconds)
SEND_INTERVAL = 60  # Send every 60 seconds

# Simulation mode (set to False when using real sensors)
SIMULATION_MODE = True

# ============================================
# END CONFIGURATION
# ============================================


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
        """
        Send sensor data to SensorVision

        Args:
            data (dict): Dictionary of variable names and values
                         Example: {"temperature": 23.5, "humidity": 65.2}

        Returns:
            bool: True if successful, False otherwise
        """
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
            print("  Make sure SensorVision backend is running")
            return False
        except requests.exceptions.Timeout:
            print("✗ Request timeout")
            return False
        except Exception as e:
            print(f"✗ Error sending data: {e}")
            return False


def read_real_sensors():
    """
    Read data from real sensors

    TODO: Replace this with your actual sensor reading code.
    Examples:
    - DHT22 on Raspberry Pi: Use Adafruit_DHT library
    - DS18B20 temperature: Use w1thermsensor library
    - BME280: Use bme280 library
    - USB sensors: Use pyserial
    """
    try:
        # Example: Read DHT22 on Raspberry Pi (uncomment and install Adafruit_DHT)
        # import Adafruit_DHT
        # humidity, temperature = Adafruit_DHT.read_retry(Adafruit_DHT.DHT22, 4)
        # return {"temperature": temperature, "humidity": humidity}

        # Example: Read DS18B20 temperature sensor (uncomment and install w1thermsensor)
        # from w1thermsensor import W1ThermSensor
        # sensor = W1ThermSensor()
        # temperature = sensor.get_temperature()
        # return {"temperature": temperature}

        # If no real sensors, return None
        return None

    except Exception as e:
        print(f"Error reading sensors: {e}")
        return None


def simulate_sensors():
    """
    Simulate sensor data for testing

    Returns:
        dict: Simulated sensor values
    """
    # Simulate temperature with daily variation
    hour = datetime.now().hour
    base_temp = 20 + 5 * ((hour - 12) / 12)  # Warmer during day
    temperature = round(base_temp + random.uniform(-2, 2), 2)

    # Simulate humidity
    humidity = round(random.uniform(40, 80), 2)

    # Simulate additional metrics
    pressure = round(random.uniform(980, 1020), 2)
    light = round(random.uniform(0, 1000), 2)

    return {
        "temperature": temperature,
        "humidity": humidity,
        "pressure": pressure,
        "light_intensity": light
    }


def main():
    """Main loop"""
    print("SensorVision Python Client")
    print("=" * 50)
    print(f"Device ID: {DEVICE_ID}")
    print(f"API URL: {API_URL}")
    print(f"Send interval: {SEND_INTERVAL} seconds")
    print(f"Mode: {'SIMULATION' if SIMULATION_MODE else 'REAL SENSORS'}")
    print("=" * 50)
    print()

    # Validate configuration
    if API_KEY == "YOUR_DEVICE_API_KEY":
        print("⚠️  WARNING: API_KEY not configured!")
        print("   Update API_KEY in the script with your device token")
        print("   Get it from SensorVision dashboard (Key icon 🔑)")
        sys.exit(1)

    # Create client
    client = SensorVisionClient(API_URL, DEVICE_ID, API_KEY)

    # Main loop
    print("Starting data collection... (Press Ctrl+C to stop)\n")

    try:
        while True:
            timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

            # Read sensors
            if SIMULATION_MODE:
                data = simulate_sensors()
            else:
                data = read_real_sensors()
                if data is None:
                    print("No real sensors configured. Set SIMULATION_MODE=True")
                    break

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
        print("Thank you for using SensorVision!")


if __name__ == "__main__":
    main()
