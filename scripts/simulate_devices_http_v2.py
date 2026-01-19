#!/usr/bin/env python3
"""
Device Telemetry Simulator for SensorVision/IndCloud (HTTP Version)
Fixed version that uses a single org token to auto-provision devices correctly.

Usage:
    API_TOKEN="your-token-here" python simulate_devices_http_v2.py

    Or edit the API_TOKEN variable below with your token from Integration Wizard.

Requirements:
    pip install requests
"""

import json
import os
import random
import time
import requests
from datetime import datetime, timezone

# Configuration
API_BASE_URL = os.environ.get("API_BASE_URL", "https://indcloud.io")

# IMPORTANT: Set this to a token from YOUR organization
# Get it from Integration Wizard -> Create Device -> Copy Token
API_TOKEN = os.environ.get("API_TOKEN", "YOUR_TOKEN_HERE")

# Device IDs - using fresh IDs to avoid conflict with old Default Org devices
DEVICES = [
    {"external_id": "meter-alpha-01", "name": "Smart Meter Alpha", "type": "smart_meter"},
    {"external_id": "meter-beta-02", "name": "Smart Meter Beta", "type": "smart_meter"},
    {"external_id": "temp-gamma-01", "name": "Temperature Sensor Gamma", "type": "temperature_sensor"},
    {"external_id": "pressure-delta-01", "name": "Pressure Sensor Delta", "type": "pressure_sensor"},
    {"external_id": "vibration-epsilon-01", "name": "Vibration Sensor Epsilon", "type": "vibration_sensor"},
]

# Telemetry patterns for different device types
TELEMETRY_PATTERNS = {
    "smart_meter": {
        "variables": {
            "kw_consumption": {"min": 10, "max": 100, "spike_value": 200},
            "voltage": {"min": 218, "max": 242, "spike_value": 260},
            "current": {"min": 5, "max": 50, "spike_value": 80},
            "power_factor": {"min": 0.85, "max": 0.99, "spike_value": 0.5},
        }
    },
    "temperature_sensor": {
        "variables": {
            "temperature": {"min": 20, "max": 35, "spike_value": 55},
            "humidity": {"min": 30, "max": 70, "spike_value": 95},
        }
    },
    "pressure_sensor": {
        "variables": {
            "pressure": {"min": 1.0, "max": 5.0, "spike_value": 8.0},
            "flow_rate": {"min": 10, "max": 100, "spike_value": 150},
        }
    },
    "vibration_sensor": {
        "variables": {
            "vibration": {"min": 0.1, "max": 2.0, "spike_value": 5.0},
            "frequency": {"min": 50, "max": 200, "spike_value": 500},
            "temperature": {"min": 30, "max": 60, "spike_value": 90},
        }
    },
}


def generate_telemetry(device_type: str, spike_probability: float = 0.1) -> dict:
    """Generate telemetry data with occasional spikes for alert testing."""
    pattern = TELEMETRY_PATTERNS.get(device_type, TELEMETRY_PATTERNS["smart_meter"])
    variables = {}

    for var_name, config in pattern["variables"].items():
        if random.random() < spike_probability:
            value = config["spike_value"]
            print(f"  [!] SPIKE: {var_name} = {value} (alert trigger)")
        else:
            value = round(random.uniform(config["min"], config["max"]), 2)
        variables[var_name] = value

    return variables


def send_telemetry(device: dict, variables: dict) -> bool:
    """Send telemetry data via HTTP API using the org token."""
    url = f"{API_BASE_URL}/api/v1/ingest/{device['external_id']}"
    headers = {
        "X-API-Key": API_TOKEN,  # Use the single org token for all devices
        "Content-Type": "application/json"
    }

    try:
        response = requests.post(url, json=variables, headers=headers, timeout=10)
        if response.status_code == 200:
            print(f"[TX] {device['name']} ({device['type']}): {variables}")
            return True
        else:
            print(f"[ERROR] {device['name']}: HTTP {response.status_code} - {response.text}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"[ERROR] {device['name']}: {e}")
        return False


def main():
    print("=" * 60)
    print("SensorVision Device Telemetry Simulator v2 (HTTP)")
    print("=" * 60)
    print(f"API Endpoint: {API_BASE_URL}")
    print(f"Using token: {API_TOKEN[:8]}...{API_TOKEN[-4:]}" if len(API_TOKEN) > 12 else "TOKEN NOT SET")
    print(f"Devices: {len(DEVICES)}")
    print()

    if API_TOKEN == "YOUR_TOKEN_HERE":
        print("[ERROR] Please set API_TOKEN environment variable or edit the script!")
        print("        Get a token from Integration Wizard -> Create Device -> Copy Token")
        return

    print("Devices that will be auto-provisioned:")
    for d in DEVICES:
        print(f"  - {d['external_id']} ({d['type']})")
    print()

    print("Starting simulation (Ctrl+C to stop)...")
    print("-" * 60)

    iteration = 0
    try:
        while True:
            iteration += 1
            print(f"\n[{iteration}] Iteration - {datetime.now().strftime('%H:%M:%S')}")

            # Increase spike probability every 5th iteration
            spike_prob = 0.3 if iteration % 5 == 0 else 0.1
            if spike_prob > 0.1:
                print("   (Higher spike probability this iteration)")

            for device in DEVICES:
                variables = generate_telemetry(device["type"], spike_prob)
                send_telemetry(device, variables)
                time.sleep(0.3)

            print(f"\n... Waiting 30 seconds before next iteration...")
            time.sleep(30)

    except KeyboardInterrupt:
        print("\n\n[STOP] Simulation stopped by user")


if __name__ == "__main__":
    main()
