/**
 * Node.js Continuous Monitoring Example
 *
 * Simulates a sensor sending data every 10 seconds
 *
 * Run with: ts-node examples/nodejs-monitor.ts
 */

import { IndCloudClient } from '../src';

const client = new IndCloudClient({
  apiUrl: 'http://localhost:8080',
  apiKey: 'your-device-token',
  timeout: 30000,
  retryAttempts: 3,
});

const DEVICE_ID = 'weather-station-001';
const INTERVAL_MS = 10000;  // 10 seconds

/**
 * Simulate reading sensor data
 */
function readSensors() {
  return {
    temperature: parseFloat((20 + Math.random() * 10).toFixed(2)),
    humidity: parseFloat((50 + Math.random() * 30).toFixed(2)),
    pressure: parseFloat((1000 + Math.random() * 30).toFixed(2)),
    light_level: parseFloat((Math.random() * 1000).toFixed(2)),
    battery_voltage: parseFloat((3.0 + Math.random() * 1.2).toFixed(2)),
  };
}

async function sendTelemetry() {
  try {
    const data = readSensors();
    const response = await client.sendData(DEVICE_ID, data);

    console.log(`[${new Date().toISOString()}] Sent data:`, {
      temp: `${data.temperature}°C`,
      humidity: `${data.humidity}%`,
      pressure: `${data.pressure}hPa`,
      light: data.light_level,
      battery: `${data.battery_voltage}V`,
    });
  } catch (error) {
    console.error(`[${new Date().toISOString()}] Error:`, error);
  }
}

async function main() {
  console.log(`Starting continuous monitoring for ${DEVICE_ID}`);
  console.log(`Sending data every ${INTERVAL_MS / 1000} seconds`);
  console.log('Press Ctrl+C to stop\n');

  // Send initial data
  await sendTelemetry();

  // Send data at regular intervals
  const intervalId = setInterval(sendTelemetry, INTERVAL_MS);

  // Graceful shutdown
  process.on('SIGINT', () => {
    console.log('\nStopping monitor...');
    clearInterval(intervalId);
    process.exit(0);
  });
}

main().catch(console.error);
