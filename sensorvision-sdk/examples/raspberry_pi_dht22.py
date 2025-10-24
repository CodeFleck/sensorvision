"""
Raspberry Pi DHT22 Temperature and Humidity Sensor Example.

This example demonstrates how to read data from a DHT22 sensor
connected to a Raspberry Pi and send it to SensorVision.

Hardware Setup:
- DHT22 sensor connected to GPIO pin 4
- VCC to 3.3V or 5V
- GND to Ground
- Data to GPIO 4

Requirements:
    pip install sensorvision-sdk Adafruit-DHT

Usage:
    python raspberry_pi_dht22.py
"""

import time
import logging
from sensorvision import SensorVisionClient

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Try to import DHT sensor library
try:
    import Adafruit_DHT
    DHT_AVAILABLE = True
except ImportError:
    logger.warning("Adafruit_DHT not available, using simulated data")
    DHT_AVAILABLE = False

# Configuration
API_URL = "http://localhost:8080"  # Change to your SensorVision URL
API_KEY = "your-device-token"       # Change to your device token
DEVICE_ID = "raspberry-pi-dht22"
DHT_SENSOR = Adafruit_DHT.DHT22 if DHT_AVAILABLE else None
DHT_PIN = 4  # GPIO pin number
INTERVAL = 60  # Seconds between readings


def read_sensor():
    """Read temperature and humidity from DHT22 sensor."""
    if not DHT_AVAILABLE:
        # Return simulated data for testing
        import random
        return {
            "temperature": round(20 + random.uniform(-5, 10), 2),
            "humidity": round(50 + random.uniform(-10, 20), 2)
        }

    humidity, temperature = Adafruit_DHT.read_retry(DHT_SENSOR, DHT_PIN)

    if humidity is not None and temperature is not None:
        return {
            "temperature": round(temperature, 2),
            "humidity": round(humidity, 2)
        }
    else:
        logger.error("Failed to read from DHT sensor")
        return None


def main():
    """Main loop to read sensor and send data to SensorVision."""
    logger.info(f"Starting DHT22 sensor monitoring for device: {DEVICE_ID}")
    logger.info(f"Reading interval: {INTERVAL} seconds")

    # Initialize SensorVision client
    client = SensorVisionClient(
        api_url=API_URL,
        api_key=API_KEY,
        retry_attempts=3
    )

    try:
        while True:
            # Read sensor data
            data = read_sensor()

            if data:
                try:
                    # Send data to SensorVision
                    response = client.send_data(DEVICE_ID, data)
                    logger.info(
                        f"Sent: Temperature={data['temperature']}°C, "
                        f"Humidity={data['humidity']}% - {response.message}"
                    )
                except Exception as e:
                    logger.error(f"Failed to send data: {e}")
            else:
                logger.warning("No valid sensor data to send")

            # Wait before next reading
            time.sleep(INTERVAL)

    except KeyboardInterrupt:
        logger.info("Stopped by user")
    finally:
        client.close()
        logger.info("Client closed")


if __name__ == "__main__":
    main()
