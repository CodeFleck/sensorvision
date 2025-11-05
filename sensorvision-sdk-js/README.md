# SensorVision JavaScript/TypeScript SDK

[![npm version](https://img.shields.io/npm/v/sensorvision-sdk.svg)](https://www.npmjs.com/package/sensorvision-sdk)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.2-blue.svg)](https://www.typescriptlang.org/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

Official JavaScript/TypeScript SDK for [SensorVision](https://github.com/CodeFleck/sensorvision) - The IoT platform that scales with you.

Build IoT applications with enterprise-grade infrastructure and developer-friendly tools. This universal SDK works in Node.js and browsers with full TypeScript support and real-time WebSocket capabilities.

## Features

- 🚀 **Easy to Use** - Simple API for sending telemetry data
- 📡 **Real-time** - WebSocket support for live telemetry subscriptions
- 🌐 **Universal** - Works in Node.js and browsers
- 📘 **TypeScript** - Full type definitions included
- ⚡ **Async/Await** - Modern Promise-based API
- 🔄 **Auto-Retry** - Automatic retry logic with exponential backoff
- 🛡️ **Error Handling** - Comprehensive error types
- 📦 **Multiple Formats** - CommonJS, ESM, and UMD builds

## Installation

### Install from GitHub (Recommended)

```bash
# Using npm
npm install CodeFleck/sensorvision#main:sensorvision-sdk-js

# Using yarn
yarn add CodeFleck/sensorvision#main:sensorvision-sdk-js

# Using pnpm
pnpm add CodeFleck/sensorvision#main:sensorvision-sdk-js
```

### Install from npm (Coming Soon)

Once published to npm, you'll be able to install with:

```bash
npm install sensorvision-sdk
# or
yarn add sensorvision-sdk
# or
pnpm add sensorvision-sdk
```

## Quick Start

### Node.js

```typescript
import { SensorVisionClient } from 'sensorvision-sdk';

const client = new SensorVisionClient({
  apiUrl: 'http://localhost:8080',
  apiKey: 'your-device-token'
});

// Send telemetry data
const response = await client.sendData('sensor-001', {
  temperature: 23.5,
  humidity: 65.2,
  pressure: 1013.25
});

console.log(response.message); // "Data ingested successfully"
```

### Browser (UMD)

```html
<script src="https://unpkg.com/sensorvision-sdk/dist/index.umd.js"></script>
<script>
  const client = new SensorVision.SensorVisionClient({
    apiUrl: 'http://localhost:8080',
    apiKey: 'your-device-token'
  });

  client.sendData('sensor-001', {
    temperature: 23.5,
    humidity: 65.2
  }).then(response => {
    console.log('Success!', response);
  });
</script>
```

### WebSocket Real-time Subscriptions

```typescript
import { WebSocketClient } from 'sensorvision-sdk';

const wsClient = new WebSocketClient({
  wsUrl: 'ws://localhost:8080/ws/telemetry',
  apiKey: 'your-api-key'
});

// Subscribe to device telemetry
wsClient.subscribe('sensor-001', (data) => {
  console.log('Temperature:', data.variables.temperature);
  console.log('Humidity:', data.variables.humidity);
});

// Connect to server
await wsClient.connect();
```

## API Reference

### SensorVisionClient

#### Constructor

```typescript
new SensorVisionClient(config: ClientConfig)
```

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `apiUrl` | `string` | required | Base URL of SensorVision API |
| `apiKey` | `string` | required | Device authentication token |
| `timeout` | `number` | `30000` | Request timeout in milliseconds |
| `retryAttempts` | `number` | `3` | Number of retry attempts |
| `retryDelay` | `number` | `1000` | Initial retry delay in milliseconds |

#### Methods

##### `sendData(deviceId: string, data: TelemetryData): Promise<IngestionResponse>`

Send telemetry data for a device.

**Parameters:**
- `deviceId` - Unique identifier for the device
- `data` - Object with variable names mapped to numeric/boolean values

**Returns:** Promise that resolves to `IngestionResponse`

**Throws:**
- `ValidationError` - If device ID or data is invalid
- `AuthenticationError` - If API key is invalid
- `NetworkError` - If network request fails
- `ServerError` - If server returns 5xx error
- `RateLimitError` - If rate limit is exceeded

**Example:**

```typescript
const response = await client.sendData('weather-station', {
  temperature: 23.5,
  humidity: 65.2,
  pressure: 1013.25,
  wind_speed: 12.5
});
```

### WebSocketClient

#### Constructor

```typescript
new WebSocketClient(config: WebSocketConfig)
```

**Configuration Options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `wsUrl` | `string` | required | WebSocket URL |
| `apiKey` | `string` | required | API key for authentication |
| `reconnect` | `boolean` | `true` | Enable auto-reconnection |
| `reconnectDelay` | `number` | `5000` | Reconnection delay in ms |
| `maxReconnectAttempts` | `number` | `10` | Max reconnection attempts |

#### Methods

##### `connect(): Promise<void>`

Connect to the WebSocket server.

##### `disconnect(): void`

Disconnect from the WebSocket server.

##### `subscribe(deviceId: string, callback: SubscriptionCallback): void`

Subscribe to telemetry data for a device.

**Parameters:**
- `deviceId` - Device ID to subscribe to
- `callback` - Function called when data is received

**Example:**

```typescript
wsClient.subscribe('sensor-001', (data) => {
  console.log(`[${data.deviceId}] ${data.timestamp}`);
  console.log('Variables:', data.variables);
});
```

##### `unsubscribe(deviceId: string, callback?: SubscriptionCallback): void`

Unsubscribe from a device. If callback is provided, only that callback is removed.

##### `onError(callback: ErrorCallback): void`

Register an error callback.

##### `isConnected(): boolean`

Check if WebSocket is currently connected.

## Examples

### Continuous Monitoring

```typescript
import { SensorVisionClient } from 'sensorvision-sdk';

const client = new SensorVisionClient({
  apiUrl: 'http://localhost:8080',
  apiKey: 'your-device-token'
});

async function monitorSensor() {
  setInterval(async () => {
    const data = {
      temperature: readTemperature(),
      humidity: readHumidity(),
      pressure: readPressure()
    };

    try {
      await client.sendData('weather-station', data);
      console.log('Data sent successfully');
    } catch (error) {
      console.error('Failed to send data:', error);
    }
  }, 60000); // Every minute
}

monitorSensor();
```

### Real-time Dashboard

See [examples/browser-dashboard.html](examples/browser-dashboard.html) for a complete browser-based dashboard example.

### Error Handling

```typescript
import {
  SensorVisionClient,
  AuthenticationError,
  ValidationError,
  NetworkError
} from 'sensorvision-sdk';

const client = new SensorVisionClient({
  apiUrl: 'http://localhost:8080',
  apiKey: 'your-device-token'
});

try {
  await client.sendData('sensor-001', { temperature: 23.5 });
} catch (error) {
  if (error instanceof AuthenticationError) {
    console.error('Invalid API key');
  } else if (error instanceof ValidationError) {
    console.error('Invalid data format');
  } else if (error instanceof NetworkError) {
    console.error('Network connection failed');
  } else {
    console.error('Unexpected error:', error);
  }
}
```

## TypeScript Support

This SDK is written in TypeScript and includes full type definitions.

```typescript
import {
  SensorVisionClient,
  TelemetryData,
  IngestionResponse,
  TelemetryPoint
} from 'sensorvision-sdk';

const data: TelemetryData = {
  temperature: 23.5,
  humidity: 65.2
};

const client = new SensorVisionClient({
  apiUrl: 'http://localhost:8080',
  apiKey: 'token'
});

const response: IngestionResponse = await client.sendData('device', data);
```

## Build

```bash
# Install dependencies
npm install

# Build all formats (CommonJS, ESM, UMD)
npm run build

# Run tests
npm test

# Run tests with coverage
npm run test:coverage

# Lint code
npm run lint

# Format code
npm run format
```

## Testing

```bash
# Run all tests
npm test

# Run tests in watch mode
npm run test:watch

# Generate coverage report
npm run test:coverage
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- **Documentation**: [GitHub Wiki](https://github.com/CodeFleck/sensorvision/wiki)
- **Issues**: [GitHub Issues](https://github.com/CodeFleck/sensorvision/issues)
- **Repository**: [GitHub](https://github.com/CodeFleck/sensorvision)

## Related Projects

- [SensorVision](https://github.com/CodeFleck/sensorvision) - Main IoT monitoring platform
- [Python SDK](../sensorvision-sdk/) - Python SDK for SensorVision
- ESP32/Arduino SDK - Coming soon

## Changelog

### Version 0.1.0

- Initial release
- HTTP client with axios
- WebSocket client for real-time subscriptions
- TypeScript support with full type definitions
- CommonJS, ESM, and UMD builds
- Automatic retry logic
- Comprehensive error handling
- Browser and Node.js support
