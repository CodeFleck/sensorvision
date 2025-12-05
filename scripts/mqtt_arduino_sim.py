#!/usr/bin/env python3
"""
MQTT Arduino-Style Telemetry Simulator for SensorVision.

Mimics the Arduino MQTT workflow by publishing to:
  topic: sensorvision/devices/{deviceId}/telemetry
  payload: JSON with deviceId, apiToken, timestamp, variables, metadata

Usage (prompts for token):
  python scripts/mqtt_arduino_sim.py --host mqtt.prod.example.com --port 1883 --device-id prod-meter-001
"""

import argparse
import json
import os
import random
import sys
import time
from datetime import datetime, timezone
from getpass import getpass

try:
    import paho.mqtt.client as mqtt
except ImportError:
    print("paho-mqtt is required. Install with: pip install paho-mqtt")
    sys.exit(1)


def build_payload(device_id: str, api_token: str) -> str:
    """Create a telemetry payload similar to Arduino MQTT examples."""
    payload = {
        "deviceId": device_id,
        "apiToken": api_token,  # Backend expects token in payload (mqtt.device-auth.required=true)
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "variables": {
            "temperature": round(random.uniform(20, 30), 2),
            "humidity": round(random.uniform(40, 70), 2),
            "voltage": round(random.uniform(215, 230), 2),
            "power_kw": round(random.uniform(0.1, 2.5), 3),
        },
        "metadata": {
            "source": "arduino-sim",
            "firmware": "mqtt-sim-1.0",
        },
    }
    return json.dumps(payload)


def resolve_config():
    """Parse CLI args, env, and prompt for token."""
    parser = argparse.ArgumentParser(description="Publish MQTT telemetry to SensorVision.")
    parser.add_argument("--host", default=os.environ.get("MQTT_HOST", "localhost"), help="MQTT broker host")
    parser.add_argument("--port", type=int, default=int(os.environ.get("MQTT_PORT", "1883")), help="MQTT broker port")
    parser.add_argument("--device-id", default=os.environ.get("DEVICE_ID", "mqtt-arduino-001"), help="Device external ID")
    parser.add_argument("--interval", type=int, default=int(os.environ.get("SEND_INTERVAL", "15")), help="Seconds between publishes")
    parser.add_argument("--token", help="User API token (reads SENSORVISION_API_TOKEN/SENSORVISION_USER_TOKEN or prompts)")
    parser.add_argument("--qos", type=int, choices=[0, 1], default=1, help="MQTT QoS level (0 or 1)")
    args = parser.parse_args()

    api_token = (
        args.token
        or os.environ.get("SENSORVISION_API_TOKEN")
        or os.environ.get("SENSORVISION_USER_TOKEN")
    )
    if not api_token:
        api_token = getpass("Enter your user API token: ").strip()
    if not api_token:
        raise SystemExit("Token is required.")

    return {
        "host": args.host,
        "port": args.port,
        "device_id": args.device_id,
        "interval": args.interval,
        "token": api_token,
        "qos": args.qos,
    }


def main():
    config = resolve_config()
    topic = f"sensorvision/devices/{config['device_id']}/telemetry"

    client = mqtt.Client(client_id=f"arduino-sim-{config['device_id']}")
    # If your broker enforces username/password, set here:
    # client.username_pw_set(username="sensorvision", password="your-broker-pass")

    try:
        client.connect(config["host"], config["port"], keepalive=60)
    except Exception as exc:
        print(f"Failed to connect to MQTT broker {config['host']}:{config['port']}: {exc}")
        sys.exit(1)

    print("=== MQTT Arduino-Style Simulator ===")
    print(f"Broker   : {config['host']}:{config['port']}")
    print(f"Topic    : {topic}")
    print(f"QoS      : {config['qos']}")
    print(f"Interval : {config['interval']}s")
    print("Press Ctrl+C to stop\n")

    try:
        while True:
            payload = build_payload(config["device_id"], config["token"])
            print(f"[{datetime.now().isoformat(timespec='seconds')}] Publishing -> {topic}")
            print(f"Payload: {payload}")

            result = client.publish(topic, payload=payload, qos=config["qos"])
            status = result.rc
            if status != mqtt.MQTT_ERR_SUCCESS:
                print(f"Publish failed with status: {status}")

            time.sleep(config["interval"])
    except KeyboardInterrupt:
        print("\nStopped by user.")
    finally:
        client.disconnect()


if __name__ == "__main__":
    main()
