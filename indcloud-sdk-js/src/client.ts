/**
 * HTTP client for IndCloud API
 */

import axios, { AxiosInstance, AxiosError } from 'axios';
import {
  ClientConfig,
  TelemetryData,
  IngestionResponse,
} from './types';
import {
  AuthenticationError,
  ValidationError,
  NetworkError,
  ServerError,
  RateLimitError,
} from './errors';

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
export class IndCloudClient {
  private config: Required<ClientConfig>;
  private axios: AxiosInstance;

  /**
   * Create a new IndCloud client
   *
   * @param config - Client configuration options
   */
  constructor(config: ClientConfig) {
    this.config = {
      apiUrl: config.apiUrl.replace(/\/$/, ''),
      apiKey: config.apiKey,
      timeout: config.timeout ?? 30000,
      retryAttempts: config.retryAttempts ?? 3,
      retryDelay: config.retryDelay ?? 1000,
    };

    this.axios = axios.create({
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
   * **DYNAMIC VARIABLES**: Send ANY variable names in the data object.
   * Variables are automatically provisioned on first use - no schema changes needed!
   *
   * @param deviceId - Unique identifier for the device
   * @param data - Telemetry data (variable names mapped to numeric values).
   *               Variable names can be anything - they're auto-created on first use.
   * @returns Response with success status and message
   * @throws {ValidationError} If device ID or data is invalid
   * @throws {AuthenticationError} If API key is invalid
   * @throws {NetworkError} If network request fails
   * @throws {ServerError} If server returns 5xx error
   * @throws {RateLimitError} If rate limit is exceeded
   *
   * @example
   * ```typescript
   * // Standard variables
   * const response = await client.sendData('sensor-001', {
   *   temperature: 23.5,
   *   humidity: 65.2,
   *   pressure: 1013.25
   * });
   *
   * // Custom variables are auto-provisioned!
   * await client.sendData('sensor-001', {
   *   soil_moisture: 45.0,
   *   light_level: 850.0,
   *   my_custom_sensor: 100.0
   * });
   * ```
   */
  async sendData(
    deviceId: string,
    data: TelemetryData
  ): Promise<IngestionResponse> {
    this.validateDeviceId(deviceId);
    this.validateTelemetryData(data);

    return this.sendDataWithRetry(deviceId, data);
  }

  /**
   * Send data with automatic retry logic
   */
  private async sendDataWithRetry(
    deviceId: string,
    data: TelemetryData,
    attempt: number = 0
  ): Promise<IngestionResponse> {
    try {
      const response = await this.axios.post(
        `/api/v1/ingest/${deviceId}`,
        data
      );

      return {
        success: true,
        message: response.data.message || 'Data ingested successfully',
        deviceId: response.data.deviceId,
        timestamp: response.data.timestamp,
      };
    } catch (error) {
      // Handle axios errors
      if (axios.isAxiosError(error)) {
        const axiosError = error as AxiosError;

        // Authentication errors - don't retry
        if (axiosError.response?.status === 401 || axiosError.response?.status === 403) {
          throw new AuthenticationError(
            `Authentication failed: ${axiosError.response.data || axiosError.message}`
          );
        }

        // Validation errors - don't retry
        if (axiosError.response?.status === 400) {
          throw new ValidationError(
            `Invalid data format: ${axiosError.response.data || axiosError.message}`
          );
        }

        // Rate limit errors - don't retry
        if (axiosError.response?.status === 429) {
          throw new RateLimitError(
            `Rate limit exceeded: ${axiosError.response.data || axiosError.message}`
          );
        }

        // Server errors - retry
        if (axiosError.response?.status && axiosError.response.status >= 500) {
          if (attempt < this.config.retryAttempts - 1) {
            await this.delay(this.config.retryDelay * Math.pow(2, attempt));
            return this.sendDataWithRetry(deviceId, data, attempt + 1);
          }
          throw new ServerError(
            `Server error (${axiosError.response.status}): ${axiosError.response.data || axiosError.message}`
          );
        }

        // Network errors - retry
        if (axiosError.code === 'ECONNABORTED' || axiosError.code === 'ENOTFOUND' || axiosError.code === 'ETIMEDOUT') {
          if (attempt < this.config.retryAttempts - 1) {
            await this.delay(this.config.retryDelay * Math.pow(2, attempt));
            return this.sendDataWithRetry(deviceId, data, attempt + 1);
          }
          throw new NetworkError(
            `Network error: ${axiosError.message}`
          );
        }

        // Other errors
        throw new NetworkError(
          `Request failed: ${axiosError.message}`
        );
      }

      // Non-axios errors
      throw new NetworkError(
        `Unexpected error: ${error instanceof Error ? error.message : String(error)}`
      );
    }
  }

  /**
   * Validate device ID
   */
  private validateDeviceId(deviceId: string): void {
    if (!deviceId || typeof deviceId !== 'string') {
      throw new ValidationError('Device ID must be a non-empty string');
    }

    if (deviceId.length > 255) {
      throw new ValidationError('Device ID must be less than 255 characters');
    }
  }

  /**
   * Validate telemetry data
   */
  private validateTelemetryData(data: TelemetryData): void {
    if (!data || typeof data !== 'object' || Array.isArray(data)) {
      throw new ValidationError('Telemetry data must be an object');
    }

    const keys = Object.keys(data);
    if (keys.length === 0) {
      throw new ValidationError('Telemetry data cannot be empty');
    }

    for (const [key, value] of Object.entries(data)) {
      if (typeof key !== 'string') {
        throw new ValidationError('Telemetry keys must be strings');
      }

      if (typeof value !== 'number') {
        throw new ValidationError(
          `Telemetry value for '${key}' must be a number, got ${typeof value}`
        );
      }
    }
  }

  /**
   * Delay utility for retry logic
   */
  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}
