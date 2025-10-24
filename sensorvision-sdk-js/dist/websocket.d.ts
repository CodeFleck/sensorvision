/**
 * WebSocket client for real-time telemetry subscriptions
 *
 * Supports both Node.js (using 'ws' package) and browser (native WebSocket) environments.
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
     * Replay all pending subscriptions to the server
     * Called when WebSocket connection opens to ensure backend is aware of all subscriptions
     */
    private replayPendingSubscriptions;
    /**
     * Cross-platform event listener attachment
     * Handles differences between Node.js EventEmitter (.on) and browser WebSocket (addEventListener/properties)
     */
    private attachEventHandlers;
    /**
     * Connect to the WebSocket server
     */
    connect(): Promise<void>;
    /**
     * Disconnect from the WebSocket server
     */
    disconnect(): void;
    /**
     * Subscribe to telemetry updates for a specific device
     *
     * @param deviceId - The device ID to subscribe to
     * @param callback - Callback function to receive telemetry data
     */
    subscribe(deviceId: string, callback: SubscriptionCallback): void;
    /**
     * Unsubscribe from telemetry updates for a specific device
     *
     * @param deviceId - The device ID to unsubscribe from
     * @param callback - Optional specific callback to remove. If not provided, removes all callbacks for the device.
     */
    unsubscribe(deviceId: string, callback?: SubscriptionCallback): void;
    /**
     * Register an error callback
     *
     * @param callback - Callback function to receive error notifications
     */
    onError(callback: ErrorCallback): void;
    /**
     * Handle incoming WebSocket messages
     */
    private handleMessage;
    /**
     * Notify error callbacks
     */
    private notifyError;
    /**
     * Schedule a reconnection attempt
     */
    private scheduleReconnect;
    /**
     * Check if WebSocket is connected
     */
    isConnected(): boolean;
}
//# sourceMappingURL=websocket.d.ts.map