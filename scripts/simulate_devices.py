#!/usr/bin/env python3
"""
Device Telemetry Simulator for SensorVision/IndCloud
Simulates varied telemetry data from multiple devices to test alerts and notifications.

NOTE: This is a DEVELOPMENT/TESTING script. Not intended for production use.

Usage:
    python simulate_devices.py

Environment variables (optional):
    MQTT_BROKER - MQTT broker host (default: 54.149.190.208)
    MQTT_PORT   - MQTT broker port (default: 1883)
    MQTT_USERNAME - MQTT username (optional)
    MQTT_PASSWORD - MQTT password (optional)

Requirements:
    pip install paho-mqtt
"""

import json
import os
import random
import time
from datetime import datetime, timezone
import paho.mqtt.client as mqtt

# Configuration (can be overridden via environment variables)
MQTT_BROKER = os.environ.get("MQTT_BROKER", "54.149.190.208")  # Production: indcloud.io
MQTT_PORT = int(os.environ.get("MQTT_PORT", "1883"))
MQTT_USERNAME = os.environ.get("MQTT_USERNAME")
MQTT_PASSWORD = os.environ.get("MQTT_PASSWORD")

# Device configurations for danielfleck268+01@gmail.com account
DEVICES = [
    {"device_id": "42a94d06-d3b6-4791-ba8b-f3cd8ec65ecd", "name": "smart-meter-001", "type": "smart_meter"},
    {"device_id": "558b0623-9b85-488a-8909-d0e185dee2a3", "name": "smart-meter-002", "type": "smart_meter"},
    {"device_id": "34e14c04-2e55-4384-b871-ff950184964b", "name": "temp-sensor-001", "type": "temperature_sensor"},
    {"device_id": "472f1d2f-a6f5-4d24-9f4a-8ffd7164acd6", "name": "pressure-sensor-001", "type": "pressure_sensor"},
    {"device_id": "64d2ced4-78a5-4243-84e9-0b71c9ea5f8e", "name": "vibration-sensor-001", "type": "vibration_sensor"},
]

# Telemetry patterns for different device types
TELEMETRY_PATTERNS = {
    "smart_meter": {
        "variables": {
            "kw_consumption": {"min": 10, "max": 100, "unit": "kW", "spike_value": 200},
            "voltage": {"min": 218, "max": 242, "unit": "V", "spike_value": 260},
            "current": {"min": 5, "max": 50, "unit": "A", "spike_value": 80},
            "power_factor": {"min": 0.85, "max": 0.99, "unit": "", "spike_value": 0.5},
        }
    },
    "temperature_sensor": {
        "variables": {
            "temperature": {"min": 20, "max": 35, "unit": "°C", "spike_value": 55},
            "humidity": {"min": 30, "max": 70, "unit": "%", "spike_value": 95},
        }
    },
    "pressure_sensor": {
        "variables": {
            "pressure": {"min": 1.0, "max": 5.0, "unit": "bar", "spike_value": 8.0},
            "flow_rate": {"min": 10, "max": 100, "unit": "L/min", "spike_value": 150},
        }
    },
    "vibration_sensor": {
        "variables": {
            "vibration": {"min": 0.1, "max": 2.0, "unit": "mm/s", "spike_value": 5.0},
            "frequency": {"min": 50, "max": 200, "unit": "Hz", "spike_value": 500},
            "temperature": {"min": 30, "max": 60, "unit": "°C", "spike_value": 90},
        }
    },
}


def generate_telemetry(device_type: str, spike_probability: float = 0.1) -> dict:
    """Generate telemetry data with occasional spikes for alert testing."""
    pattern = TELEMETRY_PATTERNS.get(device_type, TELEMETRY_PATTERNS["smart_meter"])
    variables = {}

    for var_name, config in pattern["variables"].items():
        # Randomly decide if this reading should spike (for alert testing)
        if random.random() < spike_probability:
            value = config["spike_value"]
            print(f"  [!] SPIKE: {var_name} = {value} (alert trigger)")
        else:
            value = round(random.uniform(config["min"], config["max"]), 2)
        variables[var_name] = value

    return variables


# MQTT connection result codes
MQTT_RC_CODES = {
    0: "Connection successful",
    1: "Incorrect protocol version",
    2: "Invalid client identifier",
    3: "Server unavailable",
    4: "Bad username or password",
    5: "Not authorized",
}


def on_connect(client, userdata, flags, rc, properties=None):
    if rc == 0:
        print("[OK] Connected to MQTT broker")
    else:
        error_msg = MQTT_RC_CODES.get(rc, f"Unknown error (code {rc})")
        print(f"[ERROR] Connection failed: {error_msg}")


def on_publish(client, userdata, mid, *args):
    pass  # Silent publish confirmation


def simulate_device(client: mqtt.Client, device: dict, spike_probability: float = 0.1):
    """Send telemetry for a single device."""
    device_id = device["device_id"]
    device_name = device.get("name", device_id)
    device_type = device.get("type", "smart_meter")

    # Generate telemetry
    variables = generate_telemetry(device_type, spike_probability)

    payload = {
        "deviceId": device_id,
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "variables": variables,
    }

    # MQTT topic for telemetry
    topic = f"indcloud/devices/{device_id}/telemetry"

    # Publish telemetry data
    result = client.publish(topic, json.dumps(payload), qos=1)

    print(f"[TX] {device_name} ({device_type}): {variables}")
    return result


def main():
    print("=" * 60)
    print("SensorVision Device Telemetry Simulator")
    print("=" * 60)
    print(f"MQTT Broker: {MQTT_BROKER}:{MQTT_PORT}")
    print(f"Devices: {len(DEVICES)}")
    print()

    # Display device configuration
    print("Configured devices:")
    for d in DEVICES:
        print(f"  - {d['name']} ({d['type']})")
    print()

    # Create MQTT client
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
    client.on_connect = on_connect
    client.on_publish = on_publish

    # Set authentication if provided
    if MQTT_USERNAME and MQTT_PASSWORD:
        client.username_pw_set(MQTT_USERNAME, MQTT_PASSWORD)
        print(f"Using MQTT authentication (username: {MQTT_USERNAME})")

    # Connect to broker
    print(f"Connecting to {MQTT_BROKER}:{MQTT_PORT}...")
    try:
        client.connect(MQTT_BROKER, MQTT_PORT, 60)
        client.loop_start()
        # Wait for connection with timeout
        for _ in range(10):
            time.sleep(0.5)
            if client.is_connected():
                break
        if not client.is_connected():
            print("[ERROR] Connection timeout - broker may be unreachable")
            return
    except Exception as e:
        print(f"[ERROR] Failed to connect: {e}")
        return

    print()
    print("Starting simulation (Ctrl+C to stop)...")
    print("-" * 60)

    iteration = 0
    try:
        while True:
            iteration += 1
            print(f"\n[{iteration}] Iteration - {datetime.now().strftime('%H:%M:%S')}")

            # Increase spike probability every 5th iteration for testing alerts
            spike_prob = 0.3 if iteration % 5 == 0 else 0.1
            if spike_prob > 0.1:
                print("   (Higher spike probability this iteration)")

            for device in DEVICES:
                simulate_device(client, device, spike_prob)
                time.sleep(0.5)  # Small delay between devices

            # Wait before next iteration
            print(f"\n... Waiting 30 seconds before next iteration...")
            time.sleep(30)

    except KeyboardInterrupt:
        print("\n\n[STOP] Simulation stopped by user")
    finally:
        client.loop_stop()
        client.disconnect()
        print("Disconnected from MQTT broker")


if __name__ == "__main__":
    main()
