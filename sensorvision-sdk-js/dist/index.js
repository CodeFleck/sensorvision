"use strict";
/**
 * SensorVision SDK - JavaScript/TypeScript client library
 *
 * This SDK provides easy-to-use clients for sending telemetry data
 * and subscribing to real-time updates from the SensorVision IoT platform.
 *
 * @example HTTP Client
 * ```typescript
 * import { SensorVisionClient } from 'sensorvision-sdk';
 *
 * const client = new SensorVisionClient({
 *   apiUrl: 'http://localhost:8080',
 *   apiKey: 'your-device-token'
 * });
 *
 * await client.sendData('sensor-001', {
 *   temperature: 23.5,
 *   humidity: 65.2
 * });
 * ```
 *
 * @example WebSocket Client
 * ```typescript
 * import { WebSocketClient } from 'sensorvision-sdk';
 *
 * const wsClient = new WebSocketClient({
 *   wsUrl: 'ws://localhost:8080/ws/telemetry',
 *   apiKey: 'your-api-key'
 * });
 *
 * wsClient.subscribe('sensor-001', (data) => {
 *   console.log('Temperature:', data.variables.temperature);
 * });
 *
 * await wsClient.connect();
 * ```
 *
 * @packageDocumentation
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.VERSION = exports.WebSocketError = exports.RateLimitError = exports.ServerError = exports.NetworkError = exports.ValidationError = exports.AuthenticationError = exports.SensorVisionError = exports.WebSocketClient = exports.SensorVisionClient = void 0;
var client_1 = require("./client");
Object.defineProperty(exports, "SensorVisionClient", { enumerable: true, get: function () { return client_1.SensorVisionClient; } });
var websocket_1 = require("./websocket");
Object.defineProperty(exports, "WebSocketClient", { enumerable: true, get: function () { return websocket_1.WebSocketClient; } });
var errors_1 = require("./errors");
Object.defineProperty(exports, "SensorVisionError", { enumerable: true, get: function () { return errors_1.SensorVisionError; } });
Object.defineProperty(exports, "AuthenticationError", { enumerable: true, get: function () { return errors_1.AuthenticationError; } });
Object.defineProperty(exports, "ValidationError", { enumerable: true, get: function () { return errors_1.ValidationError; } });
Object.defineProperty(exports, "NetworkError", { enumerable: true, get: function () { return errors_1.NetworkError; } });
Object.defineProperty(exports, "ServerError", { enumerable: true, get: function () { return errors_1.ServerError; } });
Object.defineProperty(exports, "RateLimitError", { enumerable: true, get: function () { return errors_1.RateLimitError; } });
Object.defineProperty(exports, "WebSocketError", { enumerable: true, get: function () { return errors_1.WebSocketError; } });
/**
 * SDK version
 */
exports.VERSION = '0.1.0';
//# sourceMappingURL=index.js.map