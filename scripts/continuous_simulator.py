#!/usr/bin/env python3
"""
Continuous Telemetry Simulator with Anomaly Generation
Sends mock IoT data to the MQTT broker with occasional anomalies for alert testing.

Usage:
    python continuous_simulator.py --host indcloud.io --port 1883

Features:
- Generates realistic telemetry data every 30 seconds
- 5% chance of generating anomalies per device per interval
- Supports multiple device types (smart_meter, environmental, hvac, industrial)
- Anomaly types: high voltage, extreme temperature, power spike, sensor failure
"""

import json
import time
import random
import argparse
import signal
import sys
from datetime import datetime, timezone

try:
    import paho.mqtt.client as mqtt
except ImportError:
    print("ERROR: paho-mqtt not installed. Run: pip install paho-mqtt")
    sys.exit(1)

# Configuration
DEVICES = [
    {"id": "sim-meter-001", "name": "Factory Sensor 01", "type": "smart_meter", "location": "Building A - Floor 1"},
    {"id": "sim-meter-002", "name": "Factory Sensor 02", "type": "smart_meter", "location": "Building A - Floor 2"},
    {"id": "sim-env-001", "name": "Warehouse Monitor", "type": "environmental", "location": "Warehouse B"},
    {"id": "sim-hvac-001", "name": "HVAC Unit Main", "type": "hvac", "location": "Mechanical Room"},
    {"id": "sim-ind-001", "name": "Production Line A", "type": "industrial", "location": "Production Hall"},
]

ANOMALY_PROBABILITY = 0.05  # 5% chance of anomaly per device per interval
running = True


def signal_handler(sig, frame):
    global running
    print("\n[INFO] Shutting down simulator...")
    running = False


def get_base_load(hour):
    """Return base power consumption based on time of day."""
    if 6 <= hour < 9:
        return 110  # Morning ramp-up
    elif 9 <= hour < 17:
        return 140  # Peak hours
    elif 17 <= hour < 22:
        return 180  # Evening peak
    else:
        return 90  # Night (low usage)


def generate_telemetry(device, force_anomaly=False):
    """Generate telemetry data for a device, optionally with anomaly."""
    now = datetime.now(timezone.utc)
    hour = now.hour
    is_anomaly = force_anomaly or random.random() < ANOMALY_PROBABILITY
    anomaly_type = None

    # Base values
    base_load = get_base_load(hour)
    kw = base_load + random.uniform(-2.5, 2.5)
    voltage = 220 + random.uniform(-5, 5)
    current = (kw / voltage) * 1000
    power_factor = 0.85 + random.uniform(-0.05, 0.05)
    frequency = 50 + random.uniform(-0.1, 0.1)
    temperature = 22 + random.uniform(-3, 3)
    humidity = 45 + random.uniform(-10, 10)

    # Generate anomaly if triggered
    if is_anomaly:
        anomaly_choices = ["high_voltage", "power_spike", "extreme_temp", "low_power_factor", "frequency_drift"]
        anomaly_type = random.choice(anomaly_choices)

        if anomaly_type == "high_voltage":
            voltage = 250 + random.uniform(0, 20)  # Way above normal
            print(f"  [ANOMALY] {device['id']}: High voltage - {voltage:.1f}V")
        elif anomaly_type == "power_spike":
            kw = base_load * 2.5 + random.uniform(0, 50)  # Power spike
            current = (kw / voltage) * 1000
            print(f"  [ANOMALY] {device['id']}: Power spike - {kw:.1f}kW")
        elif anomaly_type == "extreme_temp":
            temperature = random.choice([45 + random.uniform(0, 10), -5 + random.uniform(-5, 0)])
            print(f"  [ANOMALY] {device['id']}: Extreme temperature - {temperature:.1f}°C")
        elif anomaly_type == "low_power_factor":
            power_factor = 0.5 + random.uniform(-0.1, 0.1)  # Poor power factor
            print(f"  [ANOMALY] {device['id']}: Low power factor - {power_factor:.2f}")
        elif anomaly_type == "frequency_drift":
            frequency = 50 + random.choice([-2, 2]) + random.uniform(-0.5, 0.5)
            print(f"  [ANOMALY] {device['id']}: Frequency drift - {frequency:.2f}Hz")

    # Build payload
    variables = {
        "kw_consumption": round(kw, 3),
        "voltage": round(voltage, 3),
        "current": round(current, 3),
        "power_factor": round(power_factor, 3),
        "frequency": round(frequency, 3),
    }

    # Add temperature/humidity for environmental and HVAC sensors
    if device["type"] in ["environmental", "hvac"]:
        variables["temperature"] = round(temperature, 1)
        variables["humidity"] = round(humidity, 1)

    payload = {
        "deviceId": device["id"],
        "timestamp": now.isoformat(),
        "variables": variables,
        "metadata": {
            "location": device["location"],
            "sensor_type": device["type"],
            "is_anomaly": is_anomaly,
            "anomaly_type": anomaly_type,
            "simulator": "continuous_simulator.py"
        }
    }

    return payload


def on_connect(client, userdata, flags, rc, properties=None):
    if rc == 0:
        print("[INFO] Connected to MQTT broker successfully")
    else:
        print(f"[ERROR] Failed to connect, return code: {rc}")


def on_disconnect(client, userdata, rc, properties=None, reason=None):
    print(f"[WARN] Disconnected from MQTT broker (rc={rc})")


def main():
    global running

    parser = argparse.ArgumentParser(description="Continuous IoT Telemetry Simulator")
    parser.add_argument("--host", default="indcloud.io", help="MQTT broker host")
    parser.add_argument("--port", type=int, default=1883, help="MQTT broker port")
    parser.add_argument("--interval", type=int, default=30, help="Interval between sends (seconds)")
    parser.add_argument("--anomaly-rate", type=float, default=0.05, help="Anomaly probability (0.0-1.0)")
    args = parser.parse_args()

    global ANOMALY_PROBABILITY
    ANOMALY_PROBABILITY = args.anomaly_rate

    # Setup signal handler for graceful shutdown
    signal.signal(signal.SIGINT, signal_handler)
    signal.signal(signal.SIGTERM, signal_handler)

    print("=" * 60)
    print("  Continuous Telemetry Simulator")
    print("=" * 60)
    print(f"  MQTT Broker: {args.host}:{args.port}")
    print(f"  Interval: {args.interval} seconds")
    print(f"  Devices: {len(DEVICES)}")
    print(f"  Anomaly Rate: {ANOMALY_PROBABILITY * 100:.1f}%")
    print("=" * 60)
    print()

    # Connect to MQTT broker
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=f"simulator-{random.randint(1000, 9999)}")
    client.on_connect = on_connect
    client.on_disconnect = on_disconnect

    try:
        print(f"[INFO] Connecting to {args.host}:{args.port}...")
        client.connect(args.host, args.port, 60)
        client.loop_start()
    except Exception as e:
        print(f"[ERROR] Failed to connect to MQTT broker: {e}")
        sys.exit(1)

    # Wait for connection
    time.sleep(2)

    iteration = 0
    while running:
        iteration += 1
        print(f"\n[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] Iteration {iteration}")

        for device in DEVICES:
            try:
                payload = generate_telemetry(device)
                topic = f"indcloud/devices/{device['id']}/telemetry"
                message = json.dumps(payload)

                result = client.publish(topic, message, qos=1)
                if result.rc == mqtt.MQTT_ERR_SUCCESS:
                    print(f"  [{device['id']}] Sent: kW={payload['variables']['kw_consumption']:.1f}, V={payload['variables']['voltage']:.1f}")
                else:
                    print(f"  [{device['id']}] Failed to publish (rc={result.rc})")

            except Exception as e:
                print(f"  [{device['id']}] Error: {e}")

        # Wait for next interval
        for _ in range(args.interval):
            if not running:
                break
            time.sleep(1)

    print("\n[INFO] Stopping MQTT client...")
    client.loop_stop()
    client.disconnect()
    print("[INFO] Simulator stopped.")


if __name__ == "__main__":
    main()
