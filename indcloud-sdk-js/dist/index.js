"use strict";
/**
 * IndCloud SDK - JavaScript/TypeScript client library
 *
 * This SDK provides easy-to-use clients for sending telemetry data
 * and subscribing to real-time updates from the IndCloud IoT platform.
 *
 * @example HTTP Client
 * ```typescript
 * import { IndCloudClient } from 'indcloud-sdk';
 *
 * const client = new IndCloudClient({
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
 * import { WebSocketClient } from 'indcloud-sdk';
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
exports.VERSION = exports.WebSocketError = exports.RateLimitError = exports.ServerError = exports.NetworkError = exports.ValidationError = exports.AuthenticationError = exports.IndCloudError = exports.WebSocketClient = exports.IndCloudClient = void 0;
var client_1 = require("./client");
Object.defineProperty(exports, "IndCloudClient", { enumerable: true, get: function () { return client_1.IndCloudClient; } });
var websocket_1 = require("./websocket");
Object.defineProperty(exports, "WebSocketClient", { enumerable: true, get: function () { return websocket_1.WebSocketClient; } });
var errors_1 = require("./errors");
Object.defineProperty(exports, "IndCloudError", { enumerable: true, get: function () { return errors_1.IndCloudError; } });
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