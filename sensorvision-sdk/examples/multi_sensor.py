"""
Multi-Sensor Example with Context Manager.

This example demonstrates monitoring multiple sensor types
using the context manager pattern for proper resource cleanup.

Requirements:
    pip install sensorvision-sdk

Usage:
    python multi_sensor.py
"""

import time
import random
import logging
from sensorvision import SensorVisionClient

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Configuration
API_URL = "http://localhost:8080"
API_KEY = "your-device-token"
DEVICE_ID = "multi-sensor-station"
INTERVAL = 30  # Seconds


def read_environmental_sensors():
    """Simulate reading from environmental sensors."""
    return {
        "temperature": round(20 + random.uniform(-5, 10), 2),
        "humidity": round(50 + random.uniform(-15, 25), 2),
        "pressure": round(1013 + random.uniform(-20, 20), 2),
        "light_level": round(random.uniform(0, 1000), 2),
        "uv_index": round(random.uniform(0, 11), 2)
    }


def read_air_quality_sensors():
    """Simulate reading from air quality sensors."""
    return {
        "co2_ppm": round(400 + random.uniform(0, 600), 2),
        "pm25": round(random.uniform(0, 50), 2),
        "pm10": round(random.uniform(0, 100), 2),
        "voc": round(random.uniform(0, 500), 2)
    }


def read_power_sensors():
    """Simulate reading from power monitoring sensors."""
    return {
        "voltage": round(220 + random.uniform(-10, 10), 2),
        "current": round(random.uniform(0.1, 5.0), 2),
        "power_kw": round(random.uniform(0.1, 1.1), 3),
        "energy_kwh": round(random.uniform(0, 100), 2)
    }


def main():
    """Main monitoring loop."""
    logger.info(f"Starting multi-sensor monitoring: {DEVICE_ID}")

    # Use context manager for automatic cleanup
    with SensorVisionClient(api_url=API_URL, api_key=API_KEY) as client:
        try:
            while True:
                # Collect data from all sensor groups
                data = {}
                data.update(read_environmental_sensors())
                data.update(read_air_quality_sensors())
                data.update(read_power_sensors())

                # Send to SensorVision
                try:
                    response = client.send_data(DEVICE_ID, data)
                    logger.info(
                        f"Sent {len(data)} variables: "
                        f"temp={data['temperature']}°C, "
                        f"humidity={data['humidity']}%, "
                        f"CO2={data['co2_ppm']}ppm, "
                        f"power={data['power_kw']}kW"
                    )
                except Exception as e:
                    logger.error(f"Failed to send data: {e}")

                # Wait before next reading
                time.sleep(INTERVAL)

        except KeyboardInterrupt:
            logger.info("Stopped by user")


if __name__ == "__main__":
    main()
