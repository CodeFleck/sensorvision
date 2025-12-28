"""
Simple IndCloud SDK Example.

This is the most basic example showing how to send data to IndCloud.

Requirements:
    pip install indcloud-sdk

Usage:
    python simple_example.py
"""

from indcloud import IndCloudClient

# Configuration
# IMPORTANT: Replace with your IndCloud instance URL
# - Development: http://localhost:8080
# - Production: http://YOUR-SERVER-IP:8080 or https://your-domain.com
API_URL = "http://localhost:8080"  # Change this to your IndCloud URL
API_KEY = "your-device-token"       # Your device token from Integration Wizard
DEVICE_ID = "my-sensor"             # Your device ID

# Initialize client
client = IndCloudClient(
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
