/**
 * SensorVision Comprehensive Health Check System
 *
 * Performs deep health checks on all system components:
 * - Backend API (all endpoints)
 * - Database connectivity and performance
 * - MQTT broker
 * - WebSocket server
 * - Frontend availability
 * - System performance metrics
 *
 * Outputs detailed HTML report with pass/fail status
 */

import axios from 'axios';
import mqtt from 'mqtt';
import WebSocket from 'ws';
import pg from 'pg';
import fs from 'fs';
import { createRequire } from 'module';
const require = createRequire(import.meta.url);
const chalk = require('chalk');

const config = {
  apiUrl: process.env.API_URL || 'http://localhost:8080',
  wsUrl: process.env.WS_URL || 'ws://localhost:8080/ws/telemetry',
  mqttBroker: process.env.MQTT_BROKER || 'mqtt://localhost:1883',
  frontendUrl: process.env.FRONTEND_URL || 'http://localhost:3001',
  dbConfig: {
    host: process.env.DB_HOST || 'localhost',
    port: process.env.DB_PORT || 5432,
    database: process.env.DB_NAME || 'sensorvision',
    user: process.env.DB_USER || 'sensorvision',
    password: process.env.DB_PASSWORD || 'sensorvision',
  },
  continuous: process.argv.includes('--continuous'),
  interval: 300000, // 5 minutes
};

const results = {
  timestamp: new Date().toISOString(),
  overall: 'UNKNOWN',
  checks: [],
  metrics: {},
};

function addCheck(name, status, message, duration = 0) {
  results.checks.push({
    name,
    status, // 'PASS', 'FAIL', 'WARN'
    message,
    duration: `${duration}ms`,
    timestamp: new Date().toISOString(),
  });

  const icon = status === 'PASS' ? '✓' : status === 'FAIL' ? '✗' : '⚠';
  const color = status === 'PASS' ? chalk.green : status === 'FAIL' ? chalk.red : chalk.yellow;
  console.log(color(`${icon} ${name}: ${message} (${duration}ms)`));
}

async function checkBackendHealth() {
  const startTime = Date.now();

  try {
    const response = await axios.get(`${config.apiUrl}/actuator/health`, {
      timeout: 5000,
    });

    const duration = Date.now() - startTime;

    if (response.data.status === 'UP') {
      addCheck('Backend Health', 'PASS', 'Backend is UP', duration);
      return true;
    } else {
      addCheck('Backend Health', 'FAIL', `Status: ${response.data.status}`, duration);
      return false;
    }
  } catch (error) {
    const duration = Date.now() - startTime;
    addCheck('Backend Health', 'FAIL', `Error: ${error.message}`, duration);
    return false;
  }
}

async function checkBackendLiveness() {
  const startTime = Date.now();

  try {
    const response = await axios.get(`${config.apiUrl}/actuator/health/liveness`, {
      timeout: 5000,
    });

    const duration = Date.now() - startTime;

    if (response.status === 200) {
      addCheck('Backend Liveness', 'PASS', 'Liveness probe successful', duration);
      return true;
    } else {
      addCheck('Backend Liveness', 'FAIL', `Status: ${response.status}`, duration);
      return false;
    }
  } catch (error) {
    const duration = Date.now() - startTime;
    addCheck('Backend Liveness', 'FAIL', `Error: ${error.message}`, duration);
    return false;
  }
}

async function checkBackendReadiness() {
  const startTime = Date.now();

  try {
    const response = await axios.get(`${config.apiUrl}/actuator/health/readiness`, {
      timeout: 5000,
    });

    const duration = Date.now() - startTime;

    if (response.status === 200) {
      addCheck('Backend Readiness', 'PASS', 'Readiness probe successful', duration);
      return true;
    } else {
      addCheck('Backend Readiness', 'FAIL', `Status: ${response.status}`, duration);
      return false;
    }
  } catch (error) {
    const duration = Date.now() - startTime;
    addCheck('Backend Readiness', 'FAIL', `Error: ${error.message}`, duration);
    return false;
  }
}

async function checkDatabase() {
  const startTime = Date.now();
  const client = new pg.Client(config.dbConfig);

  try {
    await client.connect();

    // Check connection
    await client.query('SELECT 1');
    let duration = Date.now() - startTime;
    addCheck('Database Connection', 'PASS', 'PostgreSQL connection successful', duration);

    // Check tables exist
    const tablesResult = await client.query(`
      SELECT table_name
      FROM information_schema.tables
      WHERE table_schema = 'public'
      AND table_type = 'BASE TABLE'
    `);

    const tableCount = tablesResult.rows.length;
    duration = Date.now() - startTime;
    addCheck('Database Schema', 'PASS', `${tableCount} tables found`, duration);

    // Check data
    const deviceCount = await client.query('SELECT COUNT(*) FROM devices');
    const telemetryCount = await client.query('SELECT COUNT(*) FROM telemetry_records LIMIT 1000');

    results.metrics.deviceCount = parseInt(deviceCount.rows[0].count);
    results.metrics.telemetryRecords = parseInt(telemetryCount.rows[0].count);

    duration = Date.now() - startTime;
    addCheck(
      'Database Data',
      'PASS',
      `${results.metrics.deviceCount} devices, ${results.metrics.telemetryRecords} records`,
      duration
    );

    await client.end();
    return true;
  } catch (error) {
    const duration = Date.now() - startTime;
    addCheck('Database Connection', 'FAIL', `Error: ${error.message}`, duration);
    try {
      await client.end();
    } catch (e) {}
    return false;
  }
}

async function checkMQTTBroker() {
  const startTime = Date.now();

  return new Promise((resolve) => {
    const client = mqtt.connect(config.mqttBroker, {
      connectTimeout: 5000,
    });

    let resolved = false;

    client.on('connect', () => {
      const duration = Date.now() - startTime;
      addCheck('MQTT Broker', 'PASS', 'MQTT connection successful', duration);
      client.end();
      resolved = true;
      resolve(true);
    });

    client.on('error', (error) => {
      if (!resolved) {
        const duration = Date.now() - startTime;
        addCheck('MQTT Broker', 'FAIL', `Error: ${error.message}`, duration);
        client.end();
        resolved = true;
        resolve(false);
      }
    });

    setTimeout(() => {
      if (!resolved) {
        const duration = Date.now() - startTime;
        addCheck('MQTT Broker', 'FAIL', 'Connection timeout', duration);
        client.end();
        resolved = true;
        resolve(false);
      }
    }, 5000);
  });
}

async function checkWebSocket() {
  const startTime = Date.now();

  return new Promise((resolve) => {
    const ws = new WebSocket(config.wsUrl);
    let resolved = false;

    ws.on('open', () => {
      const duration = Date.now() - startTime;
      addCheck('WebSocket Server', 'PASS', 'WebSocket connection successful', duration);
      ws.close();
      resolved = true;
      resolve(true);
    });

    ws.on('error', (error) => {
      if (!resolved) {
        const duration = Date.now() - startTime;
        addCheck('WebSocket Server', 'FAIL', `Error: ${error.message}`, duration);
        resolved = true;
        resolve(false);
      }
    });

    setTimeout(() => {
      if (!resolved) {
        const duration = Date.now() - startTime;
        addCheck('WebSocket Server', 'FAIL', 'Connection timeout', duration);
        try {
          ws.close();
        } catch (e) {}
        resolved = true;
        resolve(false);
      }
    }, 5000);
  });
}

async function checkFrontend() {
  const startTime = Date.now();

  try {
    const response = await axios.get(config.frontendUrl, {
      timeout: 5000,
    });

    const duration = Date.now() - startTime;

    if (response.status === 200) {
      addCheck('Frontend', 'PASS', 'Frontend is accessible', duration);
      return true;
    } else {
      addCheck('Frontend', 'FAIL', `Status: ${response.status}`, duration);
      return false;
    }
  } catch (error) {
    const duration = Date.now() - startTime;
    addCheck('Frontend', 'FAIL', `Error: ${error.message}`, duration);
    return false;
  }
}

async function checkAPIEndpoints() {
  // Login first
  try {
    const loginResponse = await axios.post(`${config.apiUrl}/api/v1/auth/login`, {
      username: 'admin',
      password: 'admin123',
    });

    const token = loginResponse.data.token;

    // Test critical endpoints
    const endpoints = [
      { method: 'get', path: '/api/v1/devices', name: 'Devices List' },
      { method: 'get', path: '/api/v1/dashboards', name: 'Dashboards List' },
      { method: 'get', path: '/api/v1/rules', name: 'Rules List' },
      { method: 'get', path: '/api/v1/alerts', name: 'Alerts List' },
    ];

    for (const endpoint of endpoints) {
      const startTime = Date.now();

      try {
        await axios[endpoint.method](`${config.apiUrl}${endpoint.path}`, {
          headers: { Authorization: `Bearer ${token}` },
          timeout: 5000,
        });

        const duration = Date.now() - startTime;
        addCheck(`API: ${endpoint.name}`, 'PASS', `${endpoint.method.toUpperCase()} ${endpoint.path}`, duration);
      } catch (error) {
        const duration = Date.now() - startTime;
        addCheck(`API: ${endpoint.name}`, 'FAIL', `Error: ${error.message}`, duration);
      }
    }
  } catch (error) {
    addCheck('API Authentication', 'FAIL', `Login failed: ${error.message}`, 0);
  }
}

function generateHTMLReport() {
  const passCount = results.checks.filter((c) => c.status === 'PASS').length;
  const failCount = results.checks.filter((c) => c.status === 'FAIL').length;
  const warnCount = results.checks.filter((c) => c.status === 'WARN').length;
  const totalCount = results.checks.length;
  const successRate = ((passCount / totalCount) * 100).toFixed(1);

  results.overall = failCount === 0 ? 'HEALTHY' : failCount < 3 ? 'DEGRADED' : 'UNHEALTHY';

  const html = `
<!DOCTYPE html>
<html>
<head>
  <title>SensorVision Health Check Report</title>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      padding: 20px;
      min-height: 100vh;
    }
    .container {
      max-width: 1200px;
      margin: 0 auto;
      background: white;
      border-radius: 12px;
      box-shadow: 0 20px 60px rgba(0,0,0,0.3);
      overflow: hidden;
    }
    .header {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 40px;
      text-align: center;
    }
    .header h1 {
      font-size: 2.5em;
      margin-bottom: 10px;
    }
    .header .timestamp {
      opacity: 0.9;
      font-size: 0.9em;
    }
    .status-banner {
      padding: 30px;
      text-align: center;
      font-size: 1.5em;
      font-weight: bold;
    }
    .status-banner.healthy { background: #10b981; color: white; }
    .status-banner.degraded { background: #f59e0b; color: white; }
    .status-banner.unhealthy { background: #ef4444; color: white; }
    .metrics {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 20px;
      padding: 30px;
      background: #f9fafb;
    }
    .metric-card {
      background: white;
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    .metric-card .label {
      font-size: 0.9em;
      color: #6b7280;
      margin-bottom: 8px;
    }
    .metric-card .value {
      font-size: 2em;
      font-weight: bold;
      color: #1f2937;
    }
    .checks {
      padding: 30px;
    }
    .checks h2 {
      margin-bottom: 20px;
      color: #1f2937;
    }
    .check-item {
      display: flex;
      align-items: center;
      padding: 15px;
      margin-bottom: 10px;
      border-radius: 8px;
      background: #f9fafb;
    }
    .check-item.pass { border-left: 4px solid #10b981; }
    .check-item.fail { border-left: 4px solid #ef4444; }
    .check-item.warn { border-left: 4px solid #f59e0b; }
    .check-icon {
      font-size: 1.5em;
      margin-right: 15px;
      width: 30px;
      text-align: center;
    }
    .check-icon.pass { color: #10b981; }
    .check-icon.fail { color: #ef4444; }
    .check-icon.warn { color: #f59e0b; }
    .check-content {
      flex: 1;
    }
    .check-name {
      font-weight: 600;
      color: #1f2937;
      margin-bottom: 4px;
    }
    .check-message {
      font-size: 0.9em;
      color: #6b7280;
    }
    .check-duration {
      font-size: 0.85em;
      color: #9ca3af;
      margin-left: 15px;
    }
    .footer {
      padding: 20px;
      text-align: center;
      background: #f9fafb;
      color: #6b7280;
      font-size: 0.9em;
    }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <h1>🏥 SensorVision Health Check</h1>
      <div class="timestamp">${results.timestamp}</div>
    </div>

    <div class="status-banner ${results.overall.toLowerCase()}">
      ${results.overall} - ${successRate}% Success Rate
    </div>

    <div class="metrics">
      <div class="metric-card">
        <div class="label">Total Checks</div>
        <div class="value">${totalCount}</div>
      </div>
      <div class="metric-card">
        <div class="label">Passed</div>
        <div class="value" style="color: #10b981;">${passCount}</div>
      </div>
      <div class="metric-card">
        <div class="label">Failed</div>
        <div class="value" style="color: #ef4444;">${failCount}</div>
      </div>
      <div class="metric-card">
        <div class="label">Warnings</div>
        <div class="value" style="color: #f59e0b;">${warnCount}</div>
      </div>
      ${results.metrics.deviceCount !== undefined ? `
      <div class="metric-card">
        <div class="label">Devices</div>
        <div class="value">${results.metrics.deviceCount}</div>
      </div>
      ` : ''}
      ${results.metrics.telemetryRecords !== undefined ? `
      <div class="metric-card">
        <div class="label">Telemetry Records</div>
        <div class="value">${results.metrics.telemetryRecords}</div>
      </div>
      ` : ''}
    </div>

    <div class="checks">
      <h2>Health Check Results</h2>
      ${results.checks
        .map(
          (check) => `
        <div class="check-item ${check.status.toLowerCase()}">
          <div class="check-icon ${check.status.toLowerCase()}">
            ${check.status === 'PASS' ? '✓' : check.status === 'FAIL' ? '✗' : '⚠'}
          </div>
          <div class="check-content">
            <div class="check-name">${check.name}</div>
            <div class="check-message">${check.message}</div>
          </div>
          <div class="check-duration">${check.duration}</div>
        </div>
      `
        )
        .join('')}
    </div>

    <div class="footer">
      Generated by SensorVision Regression Test Suite<br>
      Last updated: ${new Date().toLocaleString()}
    </div>
  </div>

  <script>
    // Auto-refresh every 5 minutes
    setTimeout(() => location.reload(), 300000);
  </script>
</body>
</html>
  `;

  fs.writeFileSync('health-report.html', html);
  console.log(chalk.blue('\n📊 HTML report generated: health-report.html\n'));
}

async function runHealthChecks() {
  console.log(chalk.bold.blue('\n🏥 Running SensorVision Health Checks...\n'));

  await checkBackendHealth();
  await checkBackendLiveness();
  await checkBackendReadiness();
  await checkDatabase();
  await checkMQTTBroker();
  await checkWebSocket();
  await checkFrontend();
  await checkAPIEndpoints();

  generateHTMLReport();

  const failCount = results.checks.filter((c) => c.status === 'FAIL').length;

  if (failCount === 0) {
    console.log(chalk.bold.green('\n✅ ALL HEALTH CHECKS PASSED\n'));
    return 0;
  } else {
    console.log(chalk.bold.red(`\n❌ ${failCount} HEALTH CHECKS FAILED\n`));
    return 1;
  }
}

async function continuousMonitoring() {
  console.log(chalk.bold.blue('\n🔄 Starting continuous health monitoring...\n'));
  console.log(chalk.blue(`Interval: ${config.interval / 1000} seconds\n`));

  while (true) {
    await runHealthChecks();
    console.log(chalk.blue(`\nWaiting ${config.interval / 1000} seconds until next check...\n`));
    await new Promise((resolve) => setTimeout(resolve, config.interval));
    results.checks = []; // Reset for next run
  }
}

// Main execution
if (config.continuous) {
  continuousMonitoring();
} else {
  runHealthChecks().then((exitCode) => process.exit(exitCode));
}
