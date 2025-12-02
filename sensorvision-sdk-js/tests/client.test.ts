/**
 * Unit tests for SensorVisionClient
 */

import axios from 'axios';
import { SensorVisionClient } from '../src/client';
import {
  AuthenticationError,
  ValidationError,
  NetworkError,
  ServerError,
  RateLimitError,
} from '../src/errors';

// Mock axios
jest.mock('axios');
const mockedAxios = axios as jest.Mocked<typeof axios>;

describe('SensorVisionClient', () => {
  let client: SensorVisionClient;

  beforeEach(() => {
    // Clear all mocks before each test
    jest.clearAllMocks();

    // Setup mock axios instance
    const mockAxiosInstance = {
      post: jest.fn(),
    };
    mockedAxios.create.mockReturnValue(mockAxiosInstance as any);

    client = new SensorVisionClient({
      apiUrl: 'http://test.local:8080',
      apiKey: 'test-api-key',
    });
  });

  describe('constructor', () => {
    it('should initialize with required config', () => {
      const testClient = new SensorVisionClient({
        apiUrl: 'http://localhost:8080',
        apiKey: 'my-key',
      });
      expect(testClient).toBeDefined();
    });

    it('should strip trailing slash from apiUrl', () => {
      mockedAxios.create.mockClear();
      new SensorVisionClient({
        apiUrl: 'http://localhost:8080/',
        apiKey: 'my-key',
      });
      expect(mockedAxios.create).toHaveBeenCalledWith(
        expect.objectContaining({
          baseURL: 'http://localhost:8080',
        })
      );
    });

    it('should use default timeout if not provided', () => {
      mockedAxios.create.mockClear();
      new SensorVisionClient({
        apiUrl: 'http://localhost:8080',
        apiKey: 'my-key',
      });
      expect(mockedAxios.create).toHaveBeenCalledWith(
        expect.objectContaining({
          timeout: 30000,
        })
      );
    });

    it('should use custom timeout when provided', () => {
      mockedAxios.create.mockClear();
      new SensorVisionClient({
        apiUrl: 'http://localhost:8080',
        apiKey: 'my-key',
        timeout: 60000,
      });
      expect(mockedAxios.create).toHaveBeenCalledWith(
        expect.objectContaining({
          timeout: 60000,
        })
      );
    });
  });

  describe('sendData', () => {
    it('should successfully send telemetry data', async () => {
      const mockResponse = {
        data: {
          message: 'Data ingested successfully',
          deviceId: 'sensor-001',
          timestamp: '2024-01-01T12:00:00Z',
        },
      };

      const mockAxiosInstance = (mockedAxios.create as jest.Mock).mock.results[0].value;
      mockAxiosInstance.post.mockResolvedValue(mockResponse);

      const response = await client.sendData('sensor-001', {
        temperature: 23.5,
        humidity: 65.2,
      });

      expect(response.success).toBe(true);
      expect(response.message).toBe('Data ingested successfully');
      expect(response.deviceId).toBe('sensor-001');
    });

    it('should validate device ID is not empty', async () => {
      await expect(
        client.sendData('', { temperature: 23.5 })
      ).rejects.toThrow(ValidationError);
    });

    it('should validate device ID length', async () => {
      const longId = 'a'.repeat(256);
      await expect(
        client.sendData(longId, { temperature: 23.5 })
      ).rejects.toThrow(ValidationError);
    });

    it('should validate telemetry data is not empty', async () => {
      await expect(client.sendData('sensor-001', {})).rejects.toThrow(
        ValidationError
      );
    });

    it('should validate telemetry values are numeric or boolean', async () => {
      await expect(
        client.sendData('sensor-001', { temperature: 'hot' as any })
      ).rejects.toThrow(ValidationError);
    });

    it('should accept boolean values in telemetry data', async () => {
      const mockResponse = {
        data: {
          message: 'Data ingested successfully',
          deviceId: 'sensor-001',
          timestamp: '2024-01-01T12:00:00Z',
        },
      };

      const mockAxiosInstance = (mockedAxios.create as jest.Mock).mock.results[0].value;
      mockAxiosInstance.post.mockResolvedValue(mockResponse);

      const response = await client.sendData('sensor-001', {
        temperature: 23.5,
        isOnline: true,
      });

      expect(response.success).toBe(true);
    });
  });

  describe('error handling', () => {
    it('should throw AuthenticationError on 401', async () => {
      const mockAxiosInstance = (mockedAxios.create as jest.Mock).mock.results[0].value;
      const error = {
        isAxiosError: true,
        response: {
          status: 401,
          data: 'Invalid API key',
        },
        message: 'Request failed',
      };
      mockAxiosInstance.post.mockRejectedValue(error);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(
        client.sendData('sensor-001', { temperature: 23.5 })
      ).rejects.toThrow(AuthenticationError);
    });

    it('should throw AuthenticationError on 403', async () => {
      const mockAxiosInstance = (mockedAxios.create as jest.Mock).mock.results[0].value;
      const error = {
        isAxiosError: true,
        response: {
          status: 403,
          data: 'Forbidden',
        },
        message: 'Request failed',
      };
      mockAxiosInstance.post.mockRejectedValue(error);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(
        client.sendData('sensor-001', { temperature: 23.5 })
      ).rejects.toThrow(AuthenticationError);
    });

    it('should throw ValidationError on 400', async () => {
      const mockAxiosInstance = (mockedAxios.create as jest.Mock).mock.results[0].value;
      const error = {
        isAxiosError: true,
        response: {
          status: 400,
          data: 'Invalid data format',
        },
        message: 'Request failed',
      };
      mockAxiosInstance.post.mockRejectedValue(error);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(
        client.sendData('sensor-001', { temperature: 23.5 })
      ).rejects.toThrow(ValidationError);
    });

    it('should throw RateLimitError on 429', async () => {
      const mockAxiosInstance = (mockedAxios.create as jest.Mock).mock.results[0].value;
      const error = {
        isAxiosError: true,
        response: {
          status: 429,
          data: 'Rate limit exceeded',
        },
        message: 'Request failed',
      };
      mockAxiosInstance.post.mockRejectedValue(error);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(
        client.sendData('sensor-001', { temperature: 23.5 })
      ).rejects.toThrow(RateLimitError);
    });

    it('should throw ServerError on 500', async () => {
      const mockAxiosInstance = (mockedAxios.create as jest.Mock).mock.results[0].value;
      const error = {
        isAxiosError: true,
        response: {
          status: 500,
          data: 'Internal server error',
        },
        message: 'Request failed',
      };
      mockAxiosInstance.post.mockRejectedValue(error);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(
        client.sendData('sensor-001', { temperature: 23.5 })
      ).rejects.toThrow(ServerError);
    });

    it('should throw NetworkError on connection failure', async () => {
      const mockAxiosInstance = (mockedAxios.create as jest.Mock).mock.results[0].value;
      const error = {
        isAxiosError: true,
        code: 'ECONNABORTED',
        message: 'Connection aborted',
      };
      mockAxiosInstance.post.mockRejectedValue(error);
      mockedAxios.isAxiosError.mockReturnValue(true);

      await expect(
        client.sendData('sensor-001', { temperature: 23.5 })
      ).rejects.toThrow(NetworkError);
    });
  });
});
