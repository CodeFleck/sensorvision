/**
 * MQTT to WebSocket Integration Flow Test
 *
 * This test validates the complete data flow:
 * 1. Publish MQTT telemetry message
 * 2. Backend receives and processes via TelemetryIngestionService
 * 3. Data stored in PostgreSQL
 * 4. WebSocket broadcast to connected clients
 * 5. Frontend receives real-time update
 *
 * Success Criteria:
 * - MQTT message published successfully
 * - Backend processes within 500ms
 * - Database record created
 * - WebSocket message received by client
 * - Data integrity maintained throughout flow
 */

import mqtt from 'mqtt';
import WebSocket from 'ws';
import axios from 'axios';
import { createRequire } from 'module';
const require = createRequire(import.meta.url);
const chalk = require('chalk');

// Configuration
const config = {
  mqttBroker: process.env.MQTT_BROKER || 'mqtt://localhost:1883',
  wsUrl: process.env.WS_URL || 'ws://localhost:8080/ws/telemetry',
  apiUrl: process.env.API_URL || 'http://localhost:8080/api/v1',
  deviceId: `test-flow-device-${Date.now()}`,
  username: 'admin',
  password: 'admin123',
};

// Test state
let authToken;
let deviceDbId;
let deviceToken;
let testPassed = false;
let testStartTime;

// Utility functions
const log = {
  info: (msg) => console.log(chalk.blue('ℹ'), msg),
  success: (msg) => console.log(chalk.green('✓'), msg),
  error: (msg) => console.log(chalk.red('✗'), msg),
  warn: (msg) => console.log(chalk.yellow('⚠'), msg),
  step: (msg) => console.log(chalk.cyan('→'), msg),
};

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

async function authenticateUser() {
  log.step('Step 1: Authenticating user...');

  try {
    const response = await axios.post(`${config.apiUrl}/auth/login`, {
      username: config.username,
      password: config.password,
    });

    authToken = response.data.token;
    log.success(`Authenticated as ${config.username}`);
    return true;
  } catch (error) {
    log.error(`Authentication failed: ${error.message}`);
    return false;
  }
}

async function createTestDevice() {
  log.step('Step 2: Creating test device...');

  try {
    const response = await axios.post(
      `${config.apiUrl}/devices`,
      {
        name: 'Flow Test Device',
        deviceId: config.deviceId,
        description: 'Created by integration flow test',
        active: true,
      },
      {
        headers: { Authorization: `Bearer ${authToken}` },
      }
    );

    deviceDbId = response.data.id;
    log.success(`Device created with ID: ${deviceDbId}`);
    return true;
  } catch (error) {
    log.error(`Device creation failed: ${error.message}`);
    return false;
  }
}

async function generateDeviceToken() {
  log.step('Step 3: Generating device token...');

  try {
    const response = await axios.post(
      `${config.apiUrl}/device-tokens/${deviceDbId}/generate`,
      {},
      {
        headers: { Authorization: `Bearer ${authToken}` },
      }
    );

    deviceToken = response.data.token;
    log.success(`Device token generated: ${deviceToken.substring(0, 8)}...`);
    return true;
  } catch (error) {
    log.error(`Token generation failed: ${error.message}`);
    return false;
  }
}

function connectWebSocket() {
  log.step('Step 4: Connecting to WebSocket...');

  return new Promise((resolve, reject) => {
    const ws = new WebSocket(config.wsUrl);
    let connected = false;
    let messageReceived = false;

    ws.on('open', () => {
      log.success('WebSocket connected');
      connected = true;

      // Subscribe to device updates
      ws.send(JSON.stringify({
        type: 'subscribe',
        deviceId: config.deviceId,
      }));

      log.info('Subscribed to device telemetry updates');
      resolve(ws);
    });

    ws.on('error', (error) => {
      if (!connected) {
        log.error(`WebSocket connection failed: ${error.message}`);
        reject(error);
      }
    });

    ws.on('message', (data) => {
      if (!messageReceived) {
        try {
          const message = JSON.parse(data.toString());
          log.success('WebSocket message received!');
          log.info(`Message content: ${JSON.stringify(message, null, 2)}`);

          // Verify data integrity
          if (message.deviceId === config.deviceId) {
            const endTime = Date.now();
            const latency = endTime - testStartTime;

            log.success(`✅ Complete flow validated in ${latency}ms`);
            log.success(`Data integrity maintained: deviceId matches`);

            messageReceived = true;
            testPassed = true;
          }
        } catch (error) {
          log.error(`Failed to parse WebSocket message: ${error.message}`);
        }
      }
    });

    setTimeout(() => {
      if (!connected) {
        reject(new Error('WebSocket connection timeout'));
      }
    }, 10000);
  });
}

async function publishMqttTelemetry(ws) {
  log.step('Step 5: Publishing MQTT telemetry...');

  return new Promise((resolve, reject) => {
    const client = mqtt.connect(config.mqttBroker);

    client.on('connect', () => {
      log.success('Connected to MQTT broker');

      const telemetryData = {
        deviceId: config.deviceId,
        timestamp: new Date().toISOString(),
        variables: {
          temperature: 25.5 + Math.random() * 10,
          humidity: 60 + Math.random() * 20,
          pressure: 1013 + Math.random() * 5,
        },
      };

      const topic = `sensorvision/devices/${config.deviceId}/telemetry`;

      testStartTime = Date.now();

      client.publish(topic, JSON.stringify(telemetryData), (error) => {
        if (error) {
          log.error(`MQTT publish failed: ${error.message}`);
          client.end();
          reject(error);
        } else {
          log.success(`MQTT message published to topic: ${topic}`);
          log.info(`Telemetry data: ${JSON.stringify(telemetryData, null, 2)}`);
          client.end();
          resolve(telemetryData);
        }
      });
    });

    client.on('error', (error) => {
      log.error(`MQTT connection failed: ${error.message}`);
      reject(error);
    });

    setTimeout(() => {
      if (!testStartTime) {
        reject(new Error('MQTT publish timeout'));
      }
    }, 10000);
  });
}

async function verifyDatabaseStorage(telemetryData) {
  log.step('Step 6: Verifying database storage...');

  // Wait a bit for async processing
  await sleep(2000);

  try {
    const response = await axios.get(
      `${config.apiUrl}/devices/${deviceDbId}/telemetry?limit=1`,
      {
        headers: { Authorization: `Bearer ${authToken}` },
      }
    );

    if (response.data && response.data.length > 0) {
      const latestRecord = response.data[0];
      log.success('Telemetry data found in database');
      log.info(`Database record: ${JSON.stringify(latestRecord, null, 2)}`);

      // Verify data integrity
      if (
        Math.abs(latestRecord.temperature - telemetryData.variables.temperature) < 0.1
      ) {
        log.success('Data integrity verified: values match');
        return true;
      } else {
        log.warn('Data mismatch detected');
        return false;
      }
    } else {
      log.error('No telemetry data found in database');
      return false;
    }
  } catch (error) {
    log.error(`Database verification failed: ${error.message}`);
    return false;
  }
}

async function cleanupTestDevice() {
  log.step('Cleanup: Removing test device...');

  try {
    await axios.delete(`${config.apiUrl}/devices/${deviceDbId}`, {
      headers: { Authorization: `Bearer ${authToken}` },
    });
    log.success('Test device removed');
  } catch (error) {
    log.warn(`Cleanup failed: ${error.message}`);
  }
}

async function runIntegrationFlowTest() {
  console.log(chalk.bold.blue('\n🚀 SensorVision MQTT → WebSocket Integration Flow Test\n'));

  let ws;

  try {
    // Step 1: Authenticate
    if (!(await authenticateUser())) {
      throw new Error('Authentication failed');
    }

    // Step 2: Create device
    if (!(await createTestDevice())) {
      throw new Error('Device creation failed');
    }

    // Step 3: Generate token
    if (!(await generateDeviceToken())) {
      throw new Error('Token generation failed');
    }

    // Step 4: Connect WebSocket
    ws = await connectWebSocket();

    // Step 5: Publish MQTT message
    const telemetryData = await publishMqttTelemetry(ws);

    // Step 6: Wait for WebSocket message (handled in WebSocket listener)
    log.info('Waiting for WebSocket message (timeout: 10s)...');
    await sleep(10000);

    // Step 7: Verify database
    const dbVerified = await verifyDatabaseStorage(telemetryData);

    // Final verification
    if (testPassed && dbVerified) {
      console.log(chalk.bold.green('\n✅ INTEGRATION FLOW TEST PASSED\n'));
      console.log(chalk.green('All components verified:'));
      console.log(chalk.green('  ✓ MQTT publish'));
      console.log(chalk.green('  ✓ Backend processing'));
      console.log(chalk.green('  ✓ Database storage'));
      console.log(chalk.green('  ✓ WebSocket broadcast'));
      console.log(chalk.green('  ✓ Data integrity'));
    } else {
      console.log(chalk.bold.red('\n❌ INTEGRATION FLOW TEST FAILED\n'));
      console.log(chalk.red('Failed checks:'));
      if (!testPassed) console.log(chalk.red('  ✗ WebSocket message not received'));
      if (!dbVerified) console.log(chalk.red('  ✗ Database verification failed'));
      process.exit(1);
    }
  } catch (error) {
    log.error(`Test failed with error: ${error.message}`);
    console.log(chalk.bold.red('\n❌ INTEGRATION FLOW TEST FAILED\n'));
    process.exit(1);
  } finally {
    // Cleanup
    if (ws) {
      ws.close();
    }
    if (deviceDbId) {
      await cleanupTestDevice();
    }
  }
}

// Run the test
runIntegrationFlowTest();
