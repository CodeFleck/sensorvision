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
export { IndCloudClient } from './client';
export { WebSocketClient } from './websocket';
export { IndCloudError, AuthenticationError, ValidationError, NetworkError, ServerError, RateLimitError, WebSocketError, } from './errors';
export type { ClientConfig, TelemetryData, IngestionResponse, WebSocketConfig, TelemetryPoint, WebSocketMessage, SubscriptionCallback, ErrorCallback, } from './types';
/**
 * SDK version
 */
export declare const VERSION = "0.1.0";
//# sourceMappingURL=index.d.ts.map