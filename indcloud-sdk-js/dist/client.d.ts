/**
 * HTTP client for IndCloud API
 */
import { ClientConfig, TelemetryData, IngestionResponse } from './types';
/**
 * IndCloud HTTP Client
 *
 * Provides methods to send telemetry data to IndCloud platform
 * via the simplified ingestion API.
 *
 * @example
 * ```typescript
 * const client = new IndCloudClient({
 *   apiUrl: 'http://localhost:8080',
 *   apiKey: 'your-device-token'
 * });
 *
 * const response = await client.sendData('sensor-001', {
 *   temperature: 23.5,
 *   humidity: 65.2
 * });
 * ```
 */
export declare class IndCloudClient {
    private config;
    private axios;
    /**
     * Create a new IndCloud client
     *
     * @param config - Client configuration options
     */
    constructor(config: ClientConfig);
    /**
     * Send telemetry data for a device
     *
     * @param deviceId - Unique identifier for the device
     * @param data - Telemetry data (variable names mapped to numeric values)
     * @returns Response with success status and message
     * @throws {ValidationError} If device ID or data is invalid
     * @throws {AuthenticationError} If API key is invalid
     * @throws {NetworkError} If network request fails
     * @throws {ServerError} If server returns 5xx error
     * @throws {RateLimitError} If rate limit is exceeded
     *
     * @example
     * ```typescript
     * const response = await client.sendData('sensor-001', {
     *   temperature: 23.5,
     *   humidity: 65.2,
     *   pressure: 1013.25
     * });
     * ```
     */
    sendData(deviceId: string, data: TelemetryData): Promise<IngestionResponse>;
    /**
     * Send data with automatic retry logic
     */
    private sendDataWithRetry;
    /**
     * Validate device ID
     */
    private validateDeviceId;
    /**
     * Validate telemetry data
     */
    private validateTelemetryData;
    /**
     * Delay utility for retry logic
     */
    private delay;
}
//# sourceMappingURL=client.d.ts.map