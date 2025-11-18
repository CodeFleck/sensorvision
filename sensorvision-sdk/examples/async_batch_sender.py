"""
Asynchronous Batch Data Sender Example.

This example demonstrates how to send telemetry data from multiple
devices concurrently using the async client.

Requirements:
    pip install sensorvision-sdk aiohttp

Usage:
    python async_batch_sender.py
"""

import asyncio
import logging
import random
from datetime import datetime
from typing import List, Dict
from sensorvision import AsyncSensorVisionClient

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Configuration
# IMPORTANT: Replace with your SensorVision instance URL
# - Development: http://localhost:8080
# - Production: http://YOUR-SERVER-IP:8080 or https://your-domain.com
API_URL = "http://localhost:8080"
API_KEY = "your-device-token"
DEVICE_IDS = [f"sensor-{i:03d}" for i in range(1, 11)]  # sensor-001 to sensor-010
BATCH_SIZE = 5
INTERVAL = 10  # Seconds between batches


def generate_sensor_data(device_id: str) -> Dict[str, float]:
    """Generate simulated sensor data."""
    base_temp = 20 + int(device_id.split('-')[-1]) % 10
    return {
        "temperature": round(base_temp + random.uniform(-2, 5), 2),
        "humidity": round(50 + random.uniform(-10, 15), 2),
        "pressure": round(1013 + random.uniform(-10, 10), 2),
        "battery": round(random.uniform(3.0, 4.2), 2)
    }


async def send_device_data(
    client: AsyncSensorVisionClient,
    device_id: str
) -> None:
    """Send data for a single device."""
    try:
        data = generate_sensor_data(device_id)
        response = await client.send_data(device_id, data)
        logger.info(
            f"[{device_id}] Sent: temp={data['temperature']}°C, "
            f"humidity={data['humidity']}%, "
            f"pressure={data['pressure']}hPa, "
            f"battery={data['battery']}V"
        )
    except Exception as e:
        logger.error(f"[{device_id}] Failed to send data: {e}")


async def send_batch(
    client: AsyncSensorVisionClient,
    device_ids: List[str]
) -> None:
    """Send data for multiple devices concurrently."""
    tasks = [send_device_data(client, device_id) for device_id in device_ids]
    await asyncio.gather(*tasks, return_exceptions=True)


async def main():
    """Main async loop to send batched telemetry data."""
    logger.info(f"Starting async batch sender for {len(DEVICE_IDS)} devices")
    logger.info(f"Batch size: {BATCH_SIZE}, Interval: {INTERVAL} seconds")

    async with AsyncSensorVisionClient(
        api_url=API_URL,
        api_key=API_KEY,
        retry_attempts=3
    ) as client:
        try:
            iteration = 0
            while True:
                iteration += 1
                start_time = datetime.now()

                logger.info(f"\n--- Batch {iteration} started at {start_time} ---")

                # Split devices into batches
                for i in range(0, len(DEVICE_IDS), BATCH_SIZE):
                    batch = DEVICE_IDS[i:i + BATCH_SIZE]
                    logger.info(f"Processing batch: {batch}")
                    await send_batch(client, batch)

                elapsed = (datetime.now() - start_time).total_seconds()
                logger.info(
                    f"--- Batch {iteration} completed in {elapsed:.2f}s ---\n"
                )

                # Wait before next batch
                await asyncio.sleep(INTERVAL)

        except KeyboardInterrupt:
            logger.info("Stopped by user")


if __name__ == "__main__":
    asyncio.run(main())
