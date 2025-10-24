/**
 * WebSocket Real-time Subscription Example
 *
 * Subscribe to real-time telemetry data from devices
 *
 * Run with: ts-node examples/websocket-subscription.ts
 */

import { WebSocketClient } from '../src';

const wsClient = new WebSocketClient({
  wsUrl: 'ws://localhost:8080/ws/telemetry',
  apiKey: 'your-api-key',
  reconnect: true,
  reconnectDelay: 5000,
  maxReconnectAttempts: 10,
});

const DEVICE_IDS = ['sensor-001', 'sensor-002', 'sensor-003'];

async function main() {
  console.log('Connecting to WebSocket...');

  try {
    await wsClient.connect();
    console.log('Connected!\n');

    // Subscribe to multiple devices
    DEVICE_IDS.forEach(deviceId => {
      wsClient.subscribe(deviceId, (data) => {
        console.log(`[${deviceId}] ${new Date(data.timestamp).toISOString()}`);
        console.log('  Variables:', data.variables);
        console.log('');
      });
      console.log(`Subscribed to ${deviceId}`);
    });

    // Handle errors
    wsClient.onError((error) => {
      console.error('WebSocket error:', error.message);
    });

    console.log('\nListening for telemetry data...');
    console.log('Press Ctrl+C to stop\n');

    // Graceful shutdown
    process.on('SIGINT', () => {
      console.log('\nDisconnecting...');
      wsClient.disconnect();
      process.exit(0);
    });
  } catch (error) {
    console.error('Failed to connect:', error);
    process.exit(1);
  }
}

main();
