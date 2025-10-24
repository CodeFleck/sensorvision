/**
 * WebSocket client for real-time telemetry subscriptions
 */
import { WebSocketConfig, SubscriptionCallback, ErrorCallback } from './types';
/**
 * SensorVision WebSocket Client
 *
 * Provides real-time subscriptions to telemetry data via WebSocket.
 *
 * @example
 * ```typescript
 * const wsClient = new WebSocketClient({
 *   wsUrl: 'ws://localhost:8080/ws/telemetry',
 *   apiKey: 'your-api-key'
 * });
 *
 * wsClient.subscribe('sensor-001', (data) => {
 *   console.log('Received:', data);
 * });
 *
 * wsClient.connect();
 * ```
 */
export declare class WebSocketClient {
    private config;
    private ws;
    private subscriptions;
    private errorCallbacks;
    private reconnectAttempts;
    private reconnectTimer;
    private isConnecting;
    private isManualClose;
    /**
     * Create a new WebSocket client
     *
     * @param config - WebSocket configuration options
     */
    constructor(config: WebSocketConfig);
    /**
     * Connect to the WebSocket server
     */
    connect(): Promise<void>;
    /**
     * Disconnect from the WebSocket server
     */
    disconnect(): void;
    /**
     * Subscribe to telemetry data for a specific device
     *
     * @param deviceId - Device ID to subscribe to
     * @param callback - Callback function to receive telemetry data
     *
     * @example
     * ```typescript
     * wsClient.subscribe('sensor-001', (data) => {
     *   console.log(`Temperature: ${data.variables.temperature}`);
     * });
     * ```
     */
    subscribe(deviceId: string, callback: SubscriptionCallback): void;
    /**
     * Unsubscribe from telemetry data for a specific device
     *
     * @param deviceId - Device ID to unsubscribe from
     * @param callback - Optional specific callback to remove
     */
    unsubscribe(deviceId: string, callback?: SubscriptionCallback): void;
    /**
     * Register an error callback
     *
     * @param callback - Error callback function
     */
    onError(callback: ErrorCallback): void;
    /**
     * Get connection status
     */
    isConnected(): boolean;
    /**
     * Handle incoming WebSocket messages
     */
    private handleMessage;
    /**
     * Notify error callbacks
     */
    private notifyError;
    /**
     * Schedule reconnection attempt
     */
    private scheduleReconnect;
}
//# sourceMappingURL=websocket.d.ts.map