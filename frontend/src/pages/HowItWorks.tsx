import { useState } from 'react';
import { Copy, Check, Zap, Database, Wifi, Code, Terminal, Activity, AlertTriangle, Calculator, TrendingUp, Box, ArrowRight, ExternalLink } from 'lucide-react';
import { Link } from 'react-router-dom';

interface CodeBlockProps {
  code: string;
  language: string;
}

const CodeBlock = ({ code, language }: CodeBlockProps) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="relative group">
      <div className="absolute top-2 right-2 z-10">
        <button
          onClick={handleCopy}
          className="p-2 bg-gray-700 hover:bg-gray-600 rounded-md transition-colors"
          title="Copy to clipboard"
        >
          {copied ? (
            <Check className="w-4 h-4 text-green-400" />
          ) : (
            <Copy className="w-4 h-4 text-gray-300" />
          )}
        </button>
      </div>
      <pre className="bg-gray-900 text-gray-100 p-4 rounded-lg overflow-x-auto">
        <code className={`language-${language}`}>{code}</code>
      </pre>
    </div>
  );
};

const HowItWorks = () => {
  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      {/* Hero Section */}
      <div className="text-center mb-12">
        <h1 className="text-4xl font-bold text-gray-900 mb-4">
          How SensorVision Works
        </h1>
        <p className="text-xl text-gray-600 mb-8">
          Connect any device, collect telemetry data, and monitor in real-time in under 5 minutes
        </p>

        {/* Data Hierarchy Diagram */}
        <div className="bg-gradient-to-r from-blue-50 to-indigo-50 rounded-lg p-8 mb-8">
          <h3 className="text-lg font-semibold text-gray-900 mb-6">Data Hierarchy</h3>
          <div className="flex items-center justify-center gap-4 flex-wrap text-sm">
            <div className="bg-white px-6 py-3 rounded-lg shadow-sm border border-blue-200">
              <Database className="w-5 h-5 text-blue-600 mx-auto mb-1" />
              <div className="font-medium">Organization</div>
            </div>
            <ArrowRight className="w-5 h-5 text-gray-400" />
            <div className="bg-white px-6 py-3 rounded-lg shadow-sm border border-blue-200">
              <Box className="w-5 h-5 text-blue-600 mx-auto mb-1" />
              <div className="font-medium">Devices</div>
            </div>
            <ArrowRight className="w-5 h-5 text-gray-400" />
            <div className="bg-white px-6 py-3 rounded-lg shadow-sm border border-blue-200">
              <Activity className="w-5 h-5 text-blue-600 mx-auto mb-1" />
              <div className="font-medium">Telemetry Records</div>
            </div>
            <ArrowRight className="w-5 h-5 text-gray-400" />
            <div className="bg-white px-6 py-3 rounded-lg shadow-sm border border-blue-200 text-left">
              <Terminal className="w-5 h-5 text-blue-600 mb-2" />
              <div className="text-xs text-gray-600">Variables (temperature, voltage)</div>
              <div className="text-xs text-gray-600">Timestamp</div>
              <div className="text-xs text-gray-600">Context (metadata)</div>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Start Section */}
      <section className="mb-12">
        <div className="bg-green-50 border-l-4 border-green-500 p-6 rounded-r-lg mb-6">
          <h2 className="text-2xl font-bold text-gray-900 mb-2 flex items-center gap-2">
            <Zap className="w-6 h-6 text-green-600" />
            Send Your First Data Point in 30 Seconds
          </h2>
        </div>

        <p className="text-gray-700 mb-4">
          The simplest way to get started is with a basic HTTP request. Here's a curl example:
        </p>

        <CodeBlock
          language="bash"
          code={`curl -X POST http://35.88.65.186:8080/api/v1/ingest/my-device-001 \\
  -H "X-API-Key: YOUR_DEVICE_TOKEN" \\
  -H "Content-Type: application/json" \\
  -d '{
    "temperature": 22.5,
    "humidity": 65.0,
    "voltage": 220.1
  }'`}
        />

        <div className="mt-4 p-4 bg-blue-50 rounded-lg">
          <p className="text-sm text-gray-700">
            <strong>Don't have a device token yet?</strong> Use the{' '}
            <Link to="/integration-wizard" className="text-blue-600 hover:text-blue-800 font-medium">
              Integration Wizard
            </Link>{' '}
            to create your device and get a token automatically!
          </p>
        </div>
      </section>

      {/* Core Concepts Section */}
      <section className="mb-12">
        <h2 className="text-3xl font-bold text-gray-900 mb-6">Core Concepts</h2>

        {/* Telemetry Data Structure */}
        <div className="mb-8">
          <h3 className="text-2xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Database className="w-5 h-5 text-blue-600" />
            Telemetry Data Structure
          </h3>

          <div className="overflow-x-auto">
            <table className="min-w-full bg-white border border-gray-200 rounded-lg">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">Field</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">Type</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">Required</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase">Description</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                <tr>
                  <td className="px-6 py-4 text-sm font-mono text-gray-900">deviceId</td>
                  <td className="px-6 py-4 text-sm text-gray-600">string</td>
                  <td className="px-6 py-4 text-sm text-green-600 font-medium">Yes</td>
                  <td className="px-6 py-4 text-sm text-gray-600">Unique device identifier (URL parameter)</td>
                </tr>
                <tr>
                  <td className="px-6 py-4 text-sm font-mono text-gray-900">variables</td>
                  <td className="px-6 py-4 text-sm text-gray-600">object</td>
                  <td className="px-6 py-4 text-sm text-green-600 font-medium">Yes</td>
                  <td className="px-6 py-4 text-sm text-gray-600">Key-value pairs of sensor readings (temperature, voltage, etc.)</td>
                </tr>
                <tr>
                  <td className="px-6 py-4 text-sm font-mono text-gray-900">_timestamp</td>
                  <td className="px-6 py-4 text-sm text-gray-600">ISO 8601</td>
                  <td className="px-6 py-4 text-sm text-gray-500">No</td>
                  <td className="px-6 py-4 text-sm text-gray-600">Data collection time (defaults to server time if omitted)</td>
                </tr>
                <tr>
                  <td className="px-6 py-4 text-sm font-mono text-gray-900">_context</td>
                  <td className="px-6 py-4 text-sm text-gray-600">object</td>
                  <td className="px-6 py-4 text-sm text-gray-500">No</td>
                  <td className="px-6 py-4 text-sm text-gray-600">Additional metadata (location, firmware version, etc.)</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div className="mt-4">
            <p className="text-sm text-gray-700 mb-3">
              <strong>Example with all fields:</strong>
            </p>
            <CodeBlock
              language="json"
              code={`{
  "temperature": 22.5,
  "humidity": 65.0,
  "voltage": 220.1,
  "_timestamp": "2025-10-24T10:30:00Z",
  "_context": {
    "location": "warehouse-01",
    "firmware": "v2.1.0"
  }
}`}
            />
          </div>
        </div>

        {/* Device Authentication */}
        <div className="mb-8">
          <h3 className="text-2xl font-semibold text-gray-900 mb-4">Device Authentication</h3>
          <div className="grid md:grid-cols-2 gap-4">
            <div className="bg-white border border-gray-200 rounded-lg p-4">
              <h4 className="font-semibold text-gray-900 mb-2">Device Tokens</h4>
              <p className="text-sm text-gray-600">Each device has a unique API token for authentication</p>
            </div>
            <div className="bg-white border border-gray-200 rounded-lg p-4">
              <h4 className="font-semibold text-gray-900 mb-2">Token Generation</h4>
              <p className="text-sm text-gray-600">Automatic via Integration Wizard or manual via REST API</p>
            </div>
            <div className="bg-white border border-gray-200 rounded-lg p-4">
              <h4 className="font-semibold text-gray-900 mb-2">Header Format</h4>
              <p className="text-sm text-gray-600 font-mono">X-API-Key: your-device-token</p>
            </div>
            <div className="bg-white border border-gray-200 rounded-lg p-4">
              <h4 className="font-semibold text-gray-900 mb-2">Security</h4>
              <p className="text-sm text-gray-600">Tokens are scoped per device and organization</p>
            </div>
          </div>
        </div>

        {/* Variables */}
        <div className="mb-8">
          <h3 className="text-2xl font-semibold text-gray-900 mb-4">Variables (Sensor Readings)</h3>
          <ul className="space-y-2 text-gray-700">
            <li className="flex items-start gap-2">
              <Check className="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" />
              <div>
                <strong>Native Variables:</strong> Direct sensor readings (temperature, voltage, current)
              </div>
            </li>
            <li className="flex items-start gap-2">
              <Check className="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" />
              <div>
                <strong>Synthetic Variables:</strong> Calculated metrics (power = voltage × current)
              </div>
            </li>
            <li className="flex items-start gap-2">
              <Check className="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" />
              <div>
                <strong>Data Types:</strong> Numeric (float/integer), string, boolean
              </div>
            </li>
            <li className="flex items-start gap-2">
              <Check className="w-5 h-5 text-green-600 mt-0.5 flex-shrink-0" />
              <div>
                <strong>Flexible Schema:</strong> Send any variable name/value pair
              </div>
            </li>
          </ul>
        </div>

        {/* Timestamps */}
        <div className="mb-8">
          <h3 className="text-2xl font-semibold text-gray-900 mb-4">Timestamps</h3>
          <div className="bg-gray-50 rounded-lg p-4 space-y-2 text-sm text-gray-700">
            <p><strong>Automatic:</strong> Server assigns current time if not provided</p>
            <p><strong>Custom:</strong> Provide <code className="bg-gray-200 px-2 py-1 rounded">_timestamp</code> field in ISO 8601 format</p>
            <p><strong>Historical Data:</strong> Backfill data by setting past timestamps</p>
            <p><strong>Time Series:</strong> Optimized PostgreSQL storage with indexing</p>
          </div>
        </div>

        {/* Context */}
        <div className="mb-8">
          <h3 className="text-2xl font-semibold text-gray-900 mb-4">Context (Metadata)</h3>
          <p className="text-gray-700 mb-3">
            Context provides optional additional information about each data point.
          </p>
          <div className="grid md:grid-cols-2 gap-4 mb-4">
            <div>
              <h4 className="font-semibold text-gray-900 mb-2">Use Cases</h4>
              <ul className="text-sm text-gray-600 space-y-1">
                <li>• Device location</li>
                <li>• Firmware version</li>
                <li>• Sensor calibration data</li>
                <li>• Environmental conditions</li>
              </ul>
            </div>
            <div>
              <h4 className="font-semibold text-gray-900 mb-2">Example</h4>
              <CodeBlock
                language="json"
                code={`{
  "location": "room-5",
  "floor": "3"
}`}
              />
            </div>
          </div>
        </div>
      </section>

      {/* Integration Methods Section */}
      <section className="mb-12">
        <h2 className="text-3xl font-bold text-gray-900 mb-6">Integration Methods</h2>

        {/* REST API */}
        <div className="mb-8">
          <h3 className="text-2xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Code className="w-5 h-5 text-blue-600" />
            REST API Integration
          </h3>

          {/* Basic curl */}
          <div className="mb-6">
            <h4 className="text-lg font-semibold text-gray-900 mb-3">Basic curl Example</h4>
            <CodeBlock
              language="bash"
              code={`curl -X POST http://35.88.65.186:8080/api/v1/ingest/sensor-42 \\
  -H "X-API-Key: abc123token" \\
  -H "Content-Type: application/json" \\
  -d '{"temperature": 25.3, "humidity": 58}'`}
            />
          </div>

          {/* Python SDK */}
          <div className="mb-6">
            <h4 className="text-lg font-semibold text-gray-900 mb-3">Python SDK</h4>
            <p className="text-sm text-gray-700 mb-3">
              <strong>Installation:</strong>
            </p>
            <CodeBlock
              language="bash"
              code="pip install sensorvision-sdk"
            />

            <p className="text-sm text-gray-700 mb-3 mt-4">
              <strong>Basic Usage:</strong>
            </p>
            <CodeBlock
              language="python"
              code={`from sensorvision import SensorVisionClient, ClientConfig

# Configure with retry logic
config = ClientConfig(
    base_url="http://35.88.65.186:8080",
    device_id="sensor-42",
    api_key="abc123token",
    retry_attempts=3,
    retry_delay=1.0
)

client = SensorVisionClient(config)

# Send data
data = {"temperature": 25.3, "humidity": 58}
response = client.send_telemetry(data)
print(f"Success: {response}")`}
            />

            <div className="mt-3 p-3 bg-blue-50 rounded-lg">
              <p className="text-sm text-gray-700">
                <strong>Advanced Features:</strong> Configurable retry with exponential backoff, custom error handling, timestamp and context support
              </p>
              <a href="https://github.com/CodeFleck/sensorvision/tree/main/sensorvision-sdk" target="_blank" rel="noopener noreferrer" className="text-sm text-blue-600 hover:text-blue-800 font-medium inline-flex items-center gap-1 mt-2">
                Full Python SDK Documentation <ExternalLink className="w-3 h-3" />
              </a>
            </div>
          </div>

          {/* JavaScript SDK */}
          <div className="mb-6">
            <h4 className="text-lg font-semibold text-gray-900 mb-3">JavaScript/TypeScript SDK</h4>
            <p className="text-sm text-gray-700 mb-3">
              <strong>Installation:</strong>
            </p>
            <CodeBlock
              language="bash"
              code="npm install sensorvision-sdk-js"
            />

            <p className="text-sm text-gray-700 mb-3 mt-4">
              <strong>Node.js Usage:</strong>
            </p>
            <CodeBlock
              language="javascript"
              code={`const { SensorVisionClient } = require('sensorvision-sdk-js');

const client = new SensorVisionClient({
  baseUrl: 'http://35.88.65.186:8080',
  deviceId: 'sensor-42',
  apiKey: 'abc123token'
});

// Send telemetry
await client.sendTelemetry({
  temperature: 25.3,
  humidity: 58
});`}
            />

            <p className="text-sm text-gray-700 mb-3 mt-4">
              <strong>Browser Usage:</strong>
            </p>
            <CodeBlock
              language="html"
              code={`<script src="https://unpkg.com/sensorvision-sdk-js/dist/umd/sensorvision-sdk.min.js"></script>
<script>
  const client = new SensorVisionSDK.SensorVisionClient({
    baseUrl: 'http://35.88.65.186:8080',
    deviceId: 'sensor-42',
    apiKey: 'abc123token'
  });

  client.sendTelemetry({ temperature: 25.3 });
</script>`}
            />

            <div className="mt-3 p-3 bg-blue-50 rounded-lg">
              <a href="https://github.com/CodeFleck/sensorvision/tree/main/sensorvision-sdk-js" target="_blank" rel="noopener noreferrer" className="text-sm text-blue-600 hover:text-blue-800 font-medium inline-flex items-center gap-1">
                Full JavaScript SDK Documentation <ExternalLink className="w-3 h-3" />
              </a>
            </div>
          </div>
        </div>

        {/* MQTT Integration */}
        <div className="mb-8">
          <h3 className="text-2xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Wifi className="w-5 h-5 text-green-600" />
            MQTT Integration
          </h3>

          <div className="mb-4 p-4 bg-gray-50 rounded-lg">
            <h4 className="font-semibold text-gray-900 mb-2">Topic Structure</h4>
            <code className="text-sm bg-white px-3 py-1 rounded border border-gray-200">
              sensorvision/devices/&#123;deviceId&#125;/telemetry
            </code>
          </div>

          {/* Python MQTT */}
          <div className="mb-6">
            <h4 className="text-lg font-semibold text-gray-900 mb-3">Python MQTT Example</h4>
            <CodeBlock
              language="python"
              code={`import paho.mqtt.client as mqtt
import json

client = mqtt.Client()
client.username_pw_set("device-id", "your-token")
client.connect("35.88.65.186", 1883)

data = {
    "deviceId": "sensor-42",
    "timestamp": "2025-10-24T10:30:00Z",
    "variables": {
        "temperature": 25.3,
        "humidity": 58
    }
}

client.publish(
    "sensorvision/devices/sensor-42/telemetry",
    json.dumps(data)
)`}
            />
          </div>

          {/* ESP32 MQTT */}
          <div className="mb-6">
            <h4 className="text-lg font-semibold text-gray-900 mb-3">ESP32/Arduino MQTT Example</h4>
            <CodeBlock
              language="cpp"
              code={`#include <WiFi.h>
#include <PubSubClient.h>

WiFiClient espClient;
PubSubClient client(espClient);

void setup() {
  client.setServer("35.88.65.186", 1883);
  client.connect("sensor-42", "device-id", "your-token");
}

void loop() {
  String payload = "{\\"temperature\\":25.3,\\"humidity\\":58}";
  client.publish("sensorvision/devices/sensor-42/telemetry",
                 payload.c_str());
  delay(60000); // Send every minute
}`}
            />
          </div>
        </div>

        {/* WebSocket */}
        <div className="mb-8">
          <h3 className="text-2xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Activity className="w-5 h-5 text-purple-600" />
            WebSocket Real-Time Subscriptions
          </h3>

          <p className="text-gray-700 mb-4">
            Subscribe to real-time telemetry updates using WebSockets. Perfect for live dashboards and monitoring applications.
          </p>

          <div className="mb-4">
            <h4 className="text-lg font-semibold text-gray-900 mb-3">JavaScript WebSocket Client</h4>
            <CodeBlock
              language="javascript"
              code={`const client = new SensorVisionClient({
  baseUrl: 'http://35.88.65.186:8080',
  deviceId: 'sensor-42',
  apiKey: 'abc123token'
});

// Subscribe to real-time updates
await client.subscribe('sensor-42', (data) => {
  console.log('New telemetry:', data);
  // { deviceId, timestamp, temperature: 25.3, humidity: 58 }
});

// Unsubscribe when done
await client.unsubscribe('sensor-42');
await client.disconnect();`}
            />
          </div>

          <div className="p-4 bg-purple-50 rounded-lg">
            <h4 className="font-semibold text-gray-900 mb-2">WebSocket URL</h4>
            <code className="text-sm bg-white px-3 py-1 rounded border border-gray-200">
              ws://35.88.65.186:8080/ws/telemetry
            </code>
            <p className="text-sm text-gray-600 mt-2">
              Authentication: Provide JWT token via query parameter or headers
            </p>
          </div>
        </div>
      </section>

      {/* Integration Wizard Section */}
      <section className="mb-12">
        <div className="bg-gradient-to-r from-blue-500 to-indigo-600 text-white rounded-lg p-8">
          <div className="flex items-start gap-4">
            <Zap className="w-12 h-12 flex-shrink-0" />
            <div className="flex-1">
              <h2 className="text-3xl font-bold mb-3">Integration Wizard (Zero-Config Setup)</h2>
              <p className="text-blue-100 mb-4 text-lg">
                The fastest way to connect your first device!
              </p>

              <div className="grid md:grid-cols-2 gap-3 mb-6">
                <div className="flex items-center gap-2">
                  <Check className="w-5 h-5" />
                  <span>5-step visual setup process</span>
                </div>
                <div className="flex items-center gap-2">
                  <Check className="w-5 h-5" />
                  <span>Automatic device creation</span>
                </div>
                <div className="flex items-center gap-2">
                  <Check className="w-5 h-5" />
                  <span>Live code generation for 6 platforms</span>
                </div>
                <div className="flex items-center gap-2">
                  <Check className="w-5 h-5" />
                  <span>Real-time connection testing</span>
                </div>
                <div className="flex items-center gap-2">
                  <Check className="w-5 h-5" />
                  <span>Copy-to-clipboard & download</span>
                </div>
                <div className="flex items-center gap-2">
                  <Check className="w-5 h-5" />
                  <span>Works with existing devices</span>
                </div>
              </div>

              <div className="bg-white/10 rounded-lg p-4 mb-4">
                <p className="text-sm text-blue-100 mb-2">
                  <strong>Supported Platforms:</strong>
                </p>
                <p className="text-sm text-blue-50">
                  ESP32/Arduino (.ino), Python (.py), Node.js/JavaScript (.js), Raspberry Pi, cURL/Bash (.sh), Generic HTTP
                </p>
              </div>

              <Link
                to="/integration-wizard"
                className="inline-flex items-center gap-2 bg-white text-blue-600 px-6 py-3 rounded-lg font-semibold hover:bg-blue-50 transition-colors"
              >
                Try Integration Wizard Now
                <ArrowRight className="w-5 h-5" />
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Advanced Features Section */}
      <section className="mb-12">
        <h2 className="text-3xl font-bold text-gray-900 mb-6">Advanced Features</h2>

        {/* Rules Engine */}
        <div className="mb-8">
          <h3 className="text-2xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <AlertTriangle className="w-5 h-5 text-yellow-600" />
            Rules Engine & Alerts
          </h3>

          <p className="text-gray-700 mb-4">
            Create conditional monitoring rules to automatically trigger alerts when sensor values exceed thresholds.
          </p>

          <CodeBlock
            language="json"
            code={`{
  "name": "High Temperature Alert",
  "variable": "temperature",
  "operator": "GT",
  "threshold": 30.0,
  "severity": "HIGH",
  "enabled": true
}`}
          />

          <div className="mt-4 grid md:grid-cols-2 gap-4">
            <div className="bg-white border border-gray-200 rounded-lg p-4">
              <h4 className="font-semibold text-gray-900 mb-2">Supported Operators</h4>
              <ul className="text-sm text-gray-600 space-y-1">
                <li>• <code className="bg-gray-100 px-2 py-0.5 rounded">GT</code> (Greater Than)</li>
                <li>• <code className="bg-gray-100 px-2 py-0.5 rounded">GTE</code> (Greater Than or Equal)</li>
                <li>• <code className="bg-gray-100 px-2 py-0.5 rounded">LT</code> (Less Than)</li>
                <li>• <code className="bg-gray-100 px-2 py-0.5 rounded">LTE</code> (Less Than or Equal)</li>
                <li>• <code className="bg-gray-100 px-2 py-0.5 rounded">EQ</code> (Equal)</li>
              </ul>
            </div>
            <div className="bg-white border border-gray-200 rounded-lg p-4">
              <h4 className="font-semibold text-gray-900 mb-2">Alert Features</h4>
              <ul className="text-sm text-gray-600 space-y-1">
                <li>• Automatic severity calculation</li>
                <li>• 5-minute cooldown (prevent spam)</li>
                <li>• Real-time WebSocket notifications</li>
                <li>• Alert history and acknowledgment</li>
              </ul>
            </div>
          </div>
        </div>

        {/* Synthetic Variables */}
        <div className="mb-8">
          <h3 className="text-2xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <Calculator className="w-5 h-5 text-green-600" />
            Synthetic Variables (Derived Metrics)
          </h3>

          <p className="text-gray-700 mb-4">
            Calculate new metrics from existing variables using mathematical expressions.
          </p>

          <CodeBlock
            language="json"
            code={`{
  "name": "power",
  "expression": "voltage * current",
  "deviceId": "sensor-42"
}`}
          />

          <div className="mt-4 bg-green-50 rounded-lg p-4">
            <h4 className="font-semibold text-gray-900 mb-2">Expression Features</h4>
            <ul className="text-sm text-gray-700 space-y-1">
              <li>• Basic arithmetic: <code className="bg-white px-2 py-0.5 rounded border border-gray-200">+</code>, <code className="bg-white px-2 py-0.5 rounded border border-gray-200">-</code>, <code className="bg-white px-2 py-0.5 rounded border border-gray-200">*</code>, <code className="bg-white px-2 py-0.5 rounded border border-gray-200">/</code></li>
              <li>• Variable references: Use telemetry variable names</li>
              <li>• Automatic calculation on each data ingestion</li>
              <li>• Stored alongside native variables</li>
            </ul>

            <h4 className="font-semibold text-gray-900 mb-2 mt-4">Example Use Cases</h4>
            <ul className="text-sm text-gray-700 space-y-1">
              <li>• Power calculation: <code className="bg-white px-2 py-0.5 rounded border border-gray-200">voltage × current</code></li>
              <li>• Energy consumption: <code className="bg-white px-2 py-0.5 rounded border border-gray-200">kwConsumption × duration</code></li>
              <li>• Efficiency: <code className="bg-white px-2 py-0.5 rounded border border-gray-200">output / input × 100</code></li>
            </ul>
          </div>
        </div>

        {/* Analytics */}
        <div className="mb-8">
          <h3 className="text-2xl font-semibold text-gray-900 mb-4 flex items-center gap-2">
            <TrendingUp className="w-5 h-5 text-blue-600" />
            Analytics & Historical Data
          </h3>

          <p className="text-gray-700 mb-4">
            Query aggregated historical data with flexible time intervals and aggregation types.
          </p>

          <CodeBlock
            language="bash"
            code={`GET /api/v1/analytics/sensor-42?
  variable=temperature&
  interval=HOURLY&
  aggregation=AVG&
  from=2025-10-01T00:00:00Z&
  to=2025-10-24T23:59:59Z`}
          />

          <div className="mt-4 grid md:grid-cols-2 gap-4">
            <div className="bg-white border border-gray-200 rounded-lg p-4">
              <h4 className="font-semibold text-gray-900 mb-2">Aggregation Types</h4>
              <ul className="text-sm text-gray-600 space-y-1">
                <li>• <code className="bg-gray-100 px-2 py-0.5 rounded">AVG</code> - Average value</li>
                <li>• <code className="bg-gray-100 px-2 py-0.5 rounded">MIN</code> - Minimum value</li>
                <li>• <code className="bg-gray-100 px-2 py-0.5 rounded">MAX</code> - Maximum value</li>
                <li>• <code className="bg-gray-100 px-2 py-0.5 rounded">SUM</code> - Sum of values</li>
                <li>• <code className="bg-gray-100 px-2 py-0.5 rounded">COUNT</code> - Number of data points</li>
              </ul>
            </div>
            <div className="bg-white border border-gray-200 rounded-lg p-4">
              <h4 className="font-semibold text-gray-900 mb-2">Time Intervals</h4>
              <ul className="text-sm text-gray-600 space-y-1">
                <li>• <code className="bg-gray-100 px-2 py-0.5 rounded">HOURLY</code> - 1-hour buckets</li>
                <li>• <code className="bg-gray-100 px-2 py-0.5 rounded">DAILY</code> - 1-day buckets</li>
                <li>• <code className="bg-gray-100 px-2 py-0.5 rounded">WEEKLY</code> - 7-day buckets</li>
                <li>• <code className="bg-gray-100 px-2 py-0.5 rounded">MONTHLY</code> - 30-day buckets</li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* Architecture Overview Section */}
      <section className="mb-12">
        <h2 className="text-3xl font-bold text-gray-900 mb-6">Architecture Overview</h2>

        {/* Data Flow Diagram */}
        <div className="bg-gradient-to-br from-gray-50 to-blue-50 rounded-lg p-6 mb-6">
          <h3 className="text-xl font-semibold text-gray-900 mb-4">Data Flow</h3>
          <div className="space-y-3">
            <div className="flex items-center gap-3">
              <div className="bg-white px-4 py-2 rounded-lg shadow-sm border border-gray-200 text-sm font-medium">
                Device
              </div>
              <ArrowRight className="w-5 h-5 text-gray-400" />
              <div className="bg-white px-4 py-2 rounded-lg shadow-sm border border-gray-200 text-sm">
                REST / MQTT / WebSocket
              </div>
              <ArrowRight className="w-5 h-5 text-gray-400" />
              <div className="bg-blue-100 px-4 py-2 rounded-lg border border-blue-300 text-sm font-medium">
                Ingestion Service
              </div>
            </div>
            <div className="ml-8 space-y-2 text-sm text-gray-700">
              <div className="flex items-center gap-2">
                <div className="w-1 h-1 bg-gray-400 rounded-full"></div>
                Database (PostgreSQL) - Time-series storage
              </div>
              <div className="flex items-center gap-2">
                <div className="w-1 h-1 bg-gray-400 rounded-full"></div>
                Rules Engine Evaluation - Check thresholds
              </div>
              <div className="flex items-center gap-2">
                <div className="w-1 h-1 bg-gray-400 rounded-full"></div>
                Synthetic Variable Calculation - Compute derived metrics
              </div>
              <div className="flex items-center gap-2">
                <div className="w-1 h-1 bg-gray-400 rounded-full"></div>
                WebSocket Broadcast - Real-time dashboard updates
              </div>
              <div className="flex items-center gap-2">
                <div className="w-1 h-1 bg-gray-400 rounded-full"></div>
                Prometheus Metrics Export - Monitoring
              </div>
            </div>
          </div>
        </div>

        {/* Key Components */}
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div className="bg-white border border-gray-200 rounded-lg p-4">
            <h4 className="font-semibold text-gray-900 mb-2">Backend</h4>
            <p className="text-sm text-gray-600">Spring Boot (Java 17)</p>
          </div>
          <div className="bg-white border border-gray-200 rounded-lg p-4">
            <h4 className="font-semibold text-gray-900 mb-2">Database</h4>
            <p className="text-sm text-gray-600">PostgreSQL with time-series optimization</p>
          </div>
          <div className="bg-white border border-gray-200 rounded-lg p-4">
            <h4 className="font-semibold text-gray-900 mb-2">Messaging</h4>
            <p className="text-sm text-gray-600">MQTT (Mosquitto broker)</p>
          </div>
          <div className="bg-white border border-gray-200 rounded-lg p-4">
            <h4 className="font-semibold text-gray-900 mb-2">Real-time</h4>
            <p className="text-sm text-gray-600">WebSocket connections</p>
          </div>
          <div className="bg-white border border-gray-200 rounded-lg p-4">
            <h4 className="font-semibold text-gray-900 mb-2">Monitoring</h4>
            <p className="text-sm text-gray-600">Prometheus + Grafana</p>
          </div>
          <div className="bg-white border border-gray-200 rounded-lg p-4">
            <h4 className="font-semibold text-gray-900 mb-2">Frontend</h4>
            <p className="text-sm text-gray-600">React + TypeScript + Vite</p>
          </div>
        </div>
      </section>

      {/* Additional Resources Section */}
      <section className="mb-12">
        <h2 className="text-3xl font-bold text-gray-900 mb-6">Additional Resources</h2>

        <div className="grid md:grid-cols-2 gap-4 mb-6">
          <a
            href="https://github.com/CodeFleck/sensorvision/tree/main/sensorvision-sdk"
            target="_blank"
            rel="noopener noreferrer"
            className="bg-white border border-gray-200 rounded-lg p-4 hover:border-blue-400 hover:shadow-md transition-all group"
          >
            <h4 className="font-semibold text-gray-900 mb-1 group-hover:text-blue-600 flex items-center gap-2">
              Python SDK Documentation
              <ExternalLink className="w-4 h-4" />
            </h4>
            <p className="text-sm text-gray-600">Complete guide for Python integration with code examples</p>
          </a>

          <a
            href="https://github.com/CodeFleck/sensorvision/tree/main/sensorvision-sdk-js"
            target="_blank"
            rel="noopener noreferrer"
            className="bg-white border border-gray-200 rounded-lg p-4 hover:border-blue-400 hover:shadow-md transition-all group"
          >
            <h4 className="font-semibold text-gray-900 mb-1 group-hover:text-blue-600 flex items-center gap-2">
              JavaScript SDK Documentation
              <ExternalLink className="w-4 h-4" />
            </h4>
            <p className="text-sm text-gray-600">Node.js and browser integration guide with TypeScript support</p>
          </a>

          <Link
            to="/integration-wizard"
            className="bg-white border border-gray-200 rounded-lg p-4 hover:border-blue-400 hover:shadow-md transition-all group"
          >
            <h4 className="font-semibold text-gray-900 mb-1 group-hover:text-blue-600 flex items-center gap-2">
              Integration Wizard
              <Zap className="w-4 h-4" />
            </h4>
            <p className="text-sm text-gray-600">5-step guided setup with automatic code generation</p>
          </Link>

          <a
            href="https://github.com/CodeFleck/sensorvision"
            target="_blank"
            rel="noopener noreferrer"
            className="bg-white border border-gray-200 rounded-lg p-4 hover:border-blue-400 hover:shadow-md transition-all group"
          >
            <h4 className="font-semibold text-gray-900 mb-1 group-hover:text-blue-600 flex items-center gap-2">
              GitHub Repository
              <ExternalLink className="w-4 h-4" />
            </h4>
            <p className="text-sm text-gray-600">View source code, report issues, and contribute</p>
          </a>
        </div>

        {/* Example Projects */}
        <div className="bg-gray-50 rounded-lg p-6">
          <h3 className="text-xl font-semibold text-gray-900 mb-4">Example Projects</h3>
          <div className="grid md:grid-cols-2 gap-3 text-sm text-gray-700">
            <div className="flex items-center gap-2">
              <Check className="w-4 h-4 text-green-600" />
              <span>Smart meter monitoring with ESP32</span>
            </div>
            <div className="flex items-center gap-2">
              <Check className="w-4 h-4 text-green-600" />
              <span>Raspberry Pi temperature logger</span>
            </div>
            <div className="flex items-center gap-2">
              <Check className="w-4 h-4 text-green-600" />
              <span>Node.js server monitoring</span>
            </div>
            <div className="flex items-center gap-2">
              <Check className="w-4 h-4 text-green-600" />
              <span>Python sensor data collector</span>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="text-center">
        <div className="bg-gradient-to-r from-blue-500 to-indigo-600 text-white rounded-lg p-8">
          <h2 className="text-2xl font-bold mb-3">Ready to Get Started?</h2>
          <p className="text-blue-100 mb-6">
            Connect your first device in under 5 minutes using our Integration Wizard
          </p>
          <Link
            to="/integration-wizard"
            className="inline-flex items-center gap-2 bg-white text-blue-600 px-8 py-3 rounded-lg font-semibold hover:bg-blue-50 transition-colors text-lg"
          >
            <Zap className="w-5 h-5" />
            Start Integration Wizard
          </Link>
        </div>
      </section>
    </div>
  );
};

export default HowItWorks;