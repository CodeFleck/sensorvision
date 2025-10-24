"use strict";
/**
 * WebSocket client for real-time telemetry subscriptions
 */
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.WebSocketClient = void 0;
const ws_1 = __importDefault(require("ws"));
const errors_1 = require("./errors");
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
class WebSocketClient {
    /**
     * Create a new WebSocket client
     *
     * @param config - WebSocket configuration options
     */
    constructor(config) {
        this.ws = null;
        this.subscriptions = new Map();
        this.errorCallbacks = new Set();
        this.reconnectAttempts = 0;
        this.reconnectTimer = null;
        this.isConnecting = false;
        this.isManualClose = false;
        this.config = {
            wsUrl: config.wsUrl,
            apiKey: config.apiKey,
            reconnect: config.reconnect ?? true,
            reconnectDelay: config.reconnectDelay ?? 5000,
            maxReconnectAttempts: config.maxReconnectAttempts ?? 10,
        };
    }
    /**
     * Connect to the WebSocket server
     */
    async connect() {
        if (this.isConnecting || (this.ws && this.ws.readyState === ws_1.default.OPEN)) {
            return;
        }
        this.isConnecting = true;
        this.isManualClose = false;
        return new Promise((resolve, reject) => {
            try {
                this.ws = new ws_1.default(this.config.wsUrl, {
                    headers: {
                        'X-API-Key': this.config.apiKey,
                    },
                });
                this.ws.on('open', () => {
                    this.isConnecting = false;
                    this.reconnectAttempts = 0;
                    console.log('WebSocket connected');
                    resolve();
                });
                this.ws.on('message', (data) => {
                    this.handleMessage(data);
                });
                this.ws.on('error', (error) => {
                    console.error('WebSocket error:', error);
                    this.notifyError(new errors_1.WebSocketError(error.message));
                });
                this.ws.on('close', () => {
                    console.log('WebSocket disconnected');
                    this.ws = null;
                    this.isConnecting = false;
                    if (!this.isManualClose && this.config.reconnect) {
                        this.scheduleReconnect();
                    }
                });
            }
            catch (error) {
                this.isConnecting = false;
                const wsError = new errors_1.WebSocketError(`Failed to connect: ${error instanceof Error ? error.message : String(error)}`);
                this.notifyError(wsError);
                reject(wsError);
            }
        });
    }
    /**
     * Disconnect from the WebSocket server
     */
    disconnect() {
        this.isManualClose = true;
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
            this.reconnectTimer = null;
        }
        if (this.ws) {
            this.ws.close();
            this.ws = null;
        }
    }
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
    subscribe(deviceId, callback) {
        if (!this.subscriptions.has(deviceId)) {
            this.subscriptions.set(deviceId, new Set());
        }
        this.subscriptions.get(deviceId).add(callback);
        // Send subscription message if connected
        if (this.ws && this.ws.readyState === ws_1.default.OPEN) {
            this.ws.send(JSON.stringify({
                type: 'subscribe',
                deviceId,
            }));
        }
    }
    /**
     * Unsubscribe from telemetry data for a specific device
     *
     * @param deviceId - Device ID to unsubscribe from
     * @param callback - Optional specific callback to remove
     */
    unsubscribe(deviceId, callback) {
        if (!this.subscriptions.has(deviceId)) {
            return;
        }
        if (callback) {
            this.subscriptions.get(deviceId).delete(callback);
            if (this.subscriptions.get(deviceId).size === 0) {
                this.subscriptions.delete(deviceId);
            }
        }
        else {
            this.subscriptions.delete(deviceId);
        }
        // Send unsubscribe message if connected
        if (this.ws && this.ws.readyState === ws_1.default.OPEN && this.subscriptions.get(deviceId)?.size === 0) {
            this.ws.send(JSON.stringify({
                type: 'unsubscribe',
                deviceId,
            }));
        }
    }
    /**
     * Register an error callback
     *
     * @param callback - Error callback function
     */
    onError(callback) {
        this.errorCallbacks.add(callback);
    }
    /**
     * Get connection status
     */
    isConnected() {
        return this.ws !== null && this.ws.readyState === ws_1.default.OPEN;
    }
    /**
     * Handle incoming WebSocket messages
     */
    handleMessage(data) {
        try {
            const message = JSON.parse(data.toString());
            if (message.type === 'telemetry' && message.data) {
                const telemetry = message.data;
                const callbacks = this.subscriptions.get(telemetry.deviceId);
                if (callbacks) {
                    callbacks.forEach(callback => {
                        try {
                            callback(telemetry);
                        }
                        catch (error) {
                            console.error('Error in subscription callback:', error);
                        }
                    });
                }
            }
        }
        catch (error) {
            console.error('Failed to parse WebSocket message:', error);
            this.notifyError(new errors_1.WebSocketError('Failed to parse message'));
        }
    }
    /**
     * Notify error callbacks
     */
    notifyError(error) {
        this.errorCallbacks.forEach(callback => {
            try {
                callback(error);
            }
            catch (err) {
                console.error('Error in error callback:', err);
            }
        });
    }
    /**
     * Schedule reconnection attempt
     */
    scheduleReconnect() {
        if (this.reconnectAttempts >= this.config.maxReconnectAttempts) {
            console.error('Max reconnection attempts reached');
            this.notifyError(new errors_1.WebSocketError('Max reconnection attempts reached'));
            return;
        }
        this.reconnectAttempts++;
        const delay = this.config.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
        console.log(`Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.config.maxReconnectAttempts})`);
        this.reconnectTimer = setTimeout(() => {
            this.connect().catch(error => {
                console.error('Reconnection failed:', error);
            });
        }, delay);
    }
}
exports.WebSocketClient = WebSocketClient;
//# sourceMappingURL=websocket.js.map