# Phase 5: JavaScript/Node.js SDK for Simplified IoT Integration

**Labels:** `enhancement`, `sdk`, `javascript`, `typescript`
**Milestone:** Integration Simplification
**Estimated Duration:** 1 week
**Priority:** Medium
**Depends on:** Phase 0 (Quick Wins) ✅ COMPLETE

## 📦 Objectives

Create a production-ready JavaScript/TypeScript SDK for:
- Node.js server-side applications
- Browser-based monitoring dashboards
- Node-RED IoT flows
- Electron desktop applications
- WebSocket real-time subscriptions

## ✅ Tasks

### Core SDK Development
- [ ] Create TypeScript package structure
- [ ] Implement Node.js client with axios/fetch
- [ ] Implement browser client (UMD + ESM builds)
- [ ] Add WebSocket support for real-time subscriptions
- [ ] Implement automatic retries and error handling
- [ ] Add request throttling and queuing

### Features
- [ ] Device token authentication
- [ ] Auto-device creation support
- [ ] TypeScript type definitions
- [ ] Promise-based API
- [ ] Stream-based API for real-time data
- [ ] Environment variable configuration
- [ ] Proxy support for Node.js

### Quality & Testing
- [ ] Full TypeScript type coverage
- [ ] Unit tests with Jest (>90% coverage)
- [ ] Integration tests with mock server
- [ ] Browser compatibility tests
- [ ] JSDoc comments for all APIs

### Examples & Documentation
- [ ] Node.js server example
- [ ] Browser dashboard example
- [ ] Node-RED custom node
- [ ] WebSocket real-time subscription example
- [ ] Batch data sending example
- [ ] API reference documentation
- [ ] Quick start guide

### Distribution
- [ ] Package configuration (package.json, tsconfig.json)
- [ ] Build configuration (UMD, ESM, CJS)
- [ ] README with installation instructions
- [ ] Publish to NPM
- [ ] Set up GitHub Actions for CI/CD
- [ ] Semantic versioning

## 📦 Package Structure

```
sensorvision-sdk-js/
├── src/
│   ├── index.ts
│   ├── client.ts          # HTTP client
│   ├── websocket.ts       # WebSocket client
│   ├── auth.ts            # Token authentication
│   ├── types.ts           # TypeScript types
│   ├── errors.ts          # Custom errors
│   └── utils.ts           # Helper functions
├── examples/
│   ├── nodejs-server.js
│   ├── browser-dashboard.html
│   ├── node-red-node/
│   ├── websocket-subscription.js
│   └── batch-sender.js
├── tests/
│   ├── client.test.ts
│   ├── websocket.test.ts
│   └── integration.test.ts
├── dist/                  # Build output
│   ├── index.js           # CJS
│   ├── index.mjs          # ESM
│   ├── index.umd.js       # UMD for browsers
│   └── index.d.ts         # TypeScript definitions
├── package.json
├── tsconfig.json
└── README.md
```

## 💡 Usage Example

```typescript
// Node.js / TypeScript
import { SensorVisionClient } from 'sensorvision-sdk';

const client = new SensorVisionClient({
  apiUrl: 'http://localhost:8080',
  apiKey: 'your-device-token'
});

// Send data
await client.sendData('sensor-001', {
  temperature: 23.5,
  humidity: 65.2
});

// WebSocket real-time subscription
import { WebSocketClient } from 'sensorvision-sdk';

const ws = new WebSocketClient({
  wsUrl: 'ws://localhost:8080/ws',
  apiKey: 'token'
});

ws.subscribe('sensor-001', (data) => {
  console.log('Received:', data);
});

// Browser (UMD)
<script src="https://cdn.jsdelivr.net/npm/sensorvision-sdk"></script>
<script>
  const client = new SensorVision.Client({
    apiUrl: 'http://localhost:8080',
    apiKey: 'your-device-token'
  });

  client.sendData('sensor-001', {
    temperature: 23.5,
    humidity: 65.2
  });
</script>
```

## 🔴 Node-RED Integration

Create a custom Node-RED node for easy integration:

```javascript
// In Node-RED
[SensorVision Config] --> [Device Data] --> [SensorVision Output]
```

## 🎯 Success Criteria

- [ ] Package published to NPM
- [ ] >90% test coverage
- [ ] TypeScript definitions complete
- [ ] Works in Node.js 14+ and modern browsers
- [ ] WebSocket support tested
- [ ] At least 5 working examples
- [ ] Node-RED custom node published
- [ ] CI/CD pipeline set up

## 📚 References

- Backend API: `/api/v1/ingest/{deviceId}`
- WebSocket endpoint: `/ws/telemetry`
- Phase 0 implementation: ✅ COMPLETE

## 🔗 Related

- **Depends on:** Phase 0 (Quick Wins) ✅ COMPLETE
- **Related to:** Phase 4 (Python SDK)
- **Blocks:** Phase 7 (Documentation & Guides)
