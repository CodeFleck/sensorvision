"""
Simple SensorVision SDK Example.

This is the most basic example showing how to send data to SensorVision.

Requirements:
    pip install sensorvision-sdk

Usage:
    python simple_example.py
"""

from sensorvision import SensorVisionClient

# Configuration
API_URL = "http://localhost:8080"  # Your SensorVision instance URL
API_KEY = "your-device-token"       # Your device token
DEVICE_ID = "my-sensor"             # Your device ID

# Initialize client
client = SensorVisionClient(
    api_url=API_URL,
    api_key=API_KEY
)

# Send some data
try:
    response = client.send_data(DEVICE_ID, {
        "temperature": 23.5,
        "humidity": 65.2,
        "pressure": 1013.25
    })
    print(f"Success! {response.message}")
except Exception as e:
    print(f"Error: {e}")
finally:
    client.close()
