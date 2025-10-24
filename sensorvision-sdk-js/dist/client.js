"use strict";
/**
 * HTTP client for SensorVision API
 */
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.SensorVisionClient = void 0;
const axios_1 = __importDefault(require("axios"));
const errors_1 = require("./errors");
/**
 * SensorVision HTTP Client
 *
 * Provides methods to send telemetry data to SensorVision platform
 * via the simplified ingestion API.
 *
 * @example
 * ```typescript
 * const client = new SensorVisionClient({
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
class SensorVisionClient {
    /**
     * Create a new SensorVision client
     *
     * @param config - Client configuration options
     */
    constructor(config) {
        this.config = {
            apiUrl: config.apiUrl.replace(/\/$/, ''),
            apiKey: config.apiKey,
            timeout: config.timeout ?? 30000,
            retryAttempts: config.retryAttempts ?? 3,
            retryDelay: config.retryDelay ?? 1000,
        };
        this.axios = axios_1.default.create({
            baseURL: this.config.apiUrl,
            timeout: this.config.timeout,
            headers: {
                'X-API-Key': this.config.apiKey,
                'Content-Type': 'application/json',
            },
        });
    }
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
    async sendData(deviceId, data) {
        this.validateDeviceId(deviceId);
        this.validateTelemetryData(data);
        return this.sendDataWithRetry(deviceId, data);
    }
    /**
     * Send data with automatic retry logic
     */
    async sendDataWithRetry(deviceId, data, attempt = 0) {
        try {
            const response = await this.axios.post(`/api/v1/ingest/${deviceId}`, data);
            return {
                success: true,
                message: response.data.message || 'Data ingested successfully',
                deviceId: response.data.deviceId,
                timestamp: response.data.timestamp,
            };
        }
        catch (error) {
            // Handle axios errors
            if (axios_1.default.isAxiosError(error)) {
                const axiosError = error;
                // Authentication errors - don't retry
                if (axiosError.response?.status === 401 || axiosError.response?.status === 403) {
                    throw new errors_1.AuthenticationError(`Authentication failed: ${axiosError.response.data || axiosError.message}`);
                }
                // Validation errors - don't retry
                if (axiosError.response?.status === 400) {
                    throw new errors_1.ValidationError(`Invalid data format: ${axiosError.response.data || axiosError.message}`);
                }
                // Rate limit errors - don't retry
                if (axiosError.response?.status === 429) {
                    throw new errors_1.RateLimitError(`Rate limit exceeded: ${axiosError.response.data || axiosError.message}`);
                }
                // Server errors - retry
                if (axiosError.response?.status && axiosError.response.status >= 500) {
                    if (attempt < this.config.retryAttempts - 1) {
                        await this.delay(this.config.retryDelay * Math.pow(2, attempt));
                        return this.sendDataWithRetry(deviceId, data, attempt + 1);
                    }
                    throw new errors_1.ServerError(`Server error (${axiosError.response.status}): ${axiosError.response.data || axiosError.message}`);
                }
                // Network errors - retry
                if (axiosError.code === 'ECONNABORTED' || axiosError.code === 'ENOTFOUND' || axiosError.code === 'ETIMEDOUT') {
                    if (attempt < this.config.retryAttempts - 1) {
                        await this.delay(this.config.retryDelay * Math.pow(2, attempt));
                        return this.sendDataWithRetry(deviceId, data, attempt + 1);
                    }
                    throw new errors_1.NetworkError(`Network error: ${axiosError.message}`);
                }
                // Other errors
                throw new errors_1.NetworkError(`Request failed: ${axiosError.message}`);
            }
            // Non-axios errors
            throw new errors_1.NetworkError(`Unexpected error: ${error instanceof Error ? error.message : String(error)}`);
        }
    }
    /**
     * Validate device ID
     */
    validateDeviceId(deviceId) {
        if (!deviceId || typeof deviceId !== 'string') {
            throw new errors_1.ValidationError('Device ID must be a non-empty string');
        }
        if (deviceId.length > 255) {
            throw new errors_1.ValidationError('Device ID must be less than 255 characters');
        }
    }
    /**
     * Validate telemetry data
     */
    validateTelemetryData(data) {
        if (!data || typeof data !== 'object' || Array.isArray(data)) {
            throw new errors_1.ValidationError('Telemetry data must be an object');
        }
        const keys = Object.keys(data);
        if (keys.length === 0) {
            throw new errors_1.ValidationError('Telemetry data cannot be empty');
        }
        for (const [key, value] of Object.entries(data)) {
            if (typeof key !== 'string') {
                throw new errors_1.ValidationError('Telemetry keys must be strings');
            }
            if (typeof value !== 'number' && typeof value !== 'boolean') {
                throw new errors_1.ValidationError(`Telemetry value for '${key}' must be a number or boolean, got ${typeof value}`);
            }
        }
    }
    /**
     * Delay utility for retry logic
     */
    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}
exports.SensorVisionClient = SensorVisionClient;
//# sourceMappingURL=client.js.map