import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { apiService } from './api';
import type { Device, DeviceTokenResponse, Rule, Alert } from '../types';

describe('ApiService', () => {
  let fetchMock: ReturnType<typeof vi.fn>;

  // Helper to create mock response with proper headers
  const createMockResponse = (data: any, options?: { status?: number; ok?: boolean }) => ({
    ok: options?.ok ?? true,
    status: options?.status ?? 200,
    json: async () => data,
    headers: new Headers(),
  });

  beforeEach(() => {
    // Mock localStorage
    const localStorageMock = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn(),
      length: 0,
      key: vi.fn(),
    };
    vi.stubGlobal('localStorage', localStorageMock);

    // Mock fetch
    fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    // Mock window.location
    delete (window as any).location;
    window.location = { href: '' } as any;
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('Authentication and Authorization', () => {
    it('should include Authorization header when token exists', async () => {
      // Given
      const mockToken = 'test-access-token';
      vi.mocked(localStorage.getItem).mockReturnValue(mockToken);

      fetchMock.mockResolvedValue(createMockResponse([]));

      // When
      await apiService.getDevices();

      // Then
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/devices',
        expect.objectContaining({
          headers: expect.objectContaining({
            'Authorization': `Bearer ${mockToken}`,
          }),
        })
      );
    });

    it('should not include Authorization header when token does not exist', async () => {
      // Given
      vi.mocked(localStorage.getItem).mockReturnValue(null);

      fetchMock.mockResolvedValue(createMockResponse([]));

      // When
      await apiService.getDevices();

      // Then
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/devices',
        expect.objectContaining({
          headers: expect.not.objectContaining({
            'Authorization': expect.anything(),
          }),
        })
      );
    });

    it('should clear token and redirect on 401 Unauthorized', async () => {
      // Given
      vi.mocked(localStorage.getItem).mockReturnValue('expired-token');

      fetchMock.mockResolvedValue({
        ok: false,
        status: 401,
        statusText: 'Unauthorized',
      });

      // When/Then
      await expect(apiService.getDevices()).rejects.toThrow('Session expired. Please login again.');
      expect(localStorage.removeItem).toHaveBeenCalledWith('accessToken');
      expect(window.location.href).toBe('/login');
    });
  });

  describe('Error Handling', () => {
    it('should parse and throw ProblemDetail error with developer message', async () => {
      // Given
      fetchMock.mockResolvedValue({
        ok: false,
        status: 400,
        statusText: 'Bad Request',
        json: async () => ({
          title: 'Validation Error',
          detail: 'Device ID is required',
          developerMessage: 'Field validation failed for deviceId',
          errorType: 'ValidationException',
        }),
      });

      // When/Then
      await expect(apiService.createDevice({ externalId: '', name: 'Test' }))
        .rejects.toThrow('Device ID is required\n\nDeveloper Info: Field validation failed for deviceId (ValidationException)');
    });

    it('should fall back to status text when error response cannot be parsed', async () => {
      // Given
      fetchMock.mockResolvedValue({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
        json: async () => {
          throw new Error('Invalid JSON');
        },
      });

      // When/Then
      await expect(apiService.getDevices())
        .rejects.toThrow('API Error: 500 Internal Server Error');
    });

    it('should handle network errors', async () => {
      // Given
      fetchMock.mockRejectedValue(new Error('Network error'));

      // When/Then
      await expect(apiService.getDevices())
        .rejects.toThrow('Network error');
    });
  });

  describe('Response Handling', () => {
    it('should parse JSON response for successful requests', async () => {
      // Given
      const mockDevices: Device[] = [
        {
          id: '550e8400-e29b-41d4-a716-446655440001',
          externalId: 'device-001',
          name: 'Test Device',
          status: 'ONLINE',
        },
      ];

      fetchMock.mockResolvedValue(createMockResponse(mockDevices));

      // When
      const result = await apiService.getDevices();

      // Then
      expect(result).toEqual(mockDevices);
    });

    it('should return undefined for 204 No Content responses', async () => {
      // Given
      fetchMock.mockResolvedValue({
        ok: true,
        status: 204,
        headers: {
          get: () => '0',
        },
      });

      // When
      const result = await apiService.deleteDevice('device-001');

      // Then
      expect(result).toBeUndefined();
    });

    it('should handle empty response body (content-length: 0)', async () => {
      // Given
      fetchMock.mockResolvedValue({
        ok: true,
        status: 200,
        headers: {
          get: (header: string) => header === 'content-length' ? '0' : null,
        },
      });

      // When
      const result = await apiService.deleteDevice('device-001');

      // Then
      expect(result).toBeUndefined();
    });
  });

  describe('Device Management', () => {
    it('should get all devices', async () => {
      // Given
      const mockDevices: Device[] = [
        { id: '550e8400-e29b-41d4-a716-446655440001', externalId: 'device-001', name: 'Device 1', status: 'ONLINE' },
        { id: '550e8400-e29b-41d4-a716-446655440002', externalId: 'device-002', name: 'Device 2', status: 'OFFLINE' },
      ];

      fetchMock.mockResolvedValue(createMockResponse(mockDevices));

      // When
      const result = await apiService.getDevices();

      // Then
      expect(fetchMock).toHaveBeenCalledWith('/api/v1/devices', expect.any(Object));
      expect(result).toEqual(mockDevices);
    });

    it('should get device by external ID', async () => {
      // Given
      const mockDevice: Device = {
        id: '550e8400-e29b-41d4-a716-446655440001',
        externalId: 'device-001',
        name: 'Test Device',
        status: 'ONLINE',
      };

      fetchMock.mockResolvedValue(createMockResponse(mockDevice));

      // When
      const result = await apiService.getDevice('device-001');

      // Then
      expect(fetchMock).toHaveBeenCalledWith('/api/v1/devices/device-001', expect.any(Object));
      expect(result).toEqual(mockDevice);
    });

    it('should create device with POST request', async () => {
      // Given
      const newDevice = { externalId: 'device-001', name: 'New Device' };
      const createdDevice: Device = { id: '550e8400-e29b-41d4-a716-446655440001', ...newDevice, status: 'UNKNOWN' };

      fetchMock.mockResolvedValue(createMockResponse(createdDevice));

      // When
      const result = await apiService.createDevice(newDevice);

      // Then
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/devices',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(newDevice),
        })
      );
      expect(result).toEqual(createdDevice);
    });

    it('should update device with PUT request', async () => {
      // Given
      const updates = { name: 'Updated Device' };
      const updatedDevice: Device = {
        id: '550e8400-e29b-41d4-a716-446655440001',
        externalId: 'device-001',
        name: 'Updated Device',
        status: 'ONLINE',
      };

      fetchMock.mockResolvedValue(createMockResponse(updatedDevice));

      // When
      const result = await apiService.updateDevice('device-001', updates);

      // Then
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/devices/device-001',
        expect.objectContaining({
          method: 'PUT',
          body: JSON.stringify(updates),
        })
      );
      expect(result).toEqual(updatedDevice);
    });

    it('should delete device with DELETE request', async () => {
      // Given
      fetchMock.mockResolvedValue({
        ok: true,
        status: 204,
        headers: { get: () => '0' },
      });

      // When
      await apiService.deleteDevice('device-001');

      // Then
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/devices/device-001',
        expect.objectContaining({
          method: 'DELETE',
        })
      );
    });
  });

  describe('Device Token Management', () => {
    it('should generate device token', async () => {
      // Given
      const mockResponse: DeviceTokenResponse = {
        token: 'new-token-123',
        maskedToken: 'new***123',
        expiresAt: null,
        message: 'Token generated successfully',
        success: true,
      };

      fetchMock.mockResolvedValue(createMockResponse(mockResponse));

      // When
      const result = await apiService.generateDeviceToken('device-001');

      // Then
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/devices/device-001/token/generate',
        expect.objectContaining({ method: 'POST' })
      );
      expect(result).toEqual(mockResponse);
    });

    it('should rotate device token', async () => {
      // Given
      const mockResponse: DeviceTokenResponse = {
        token: 'rotated-token-456',
        maskedToken: 'rotated***456',
        expiresAt: null,
        message: 'Token rotated successfully',
        success: true,
      };

      fetchMock.mockResolvedValue(createMockResponse(mockResponse));

      // When
      const result = await apiService.rotateDeviceToken('device-001');

      // Then
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/devices/device-001/token/rotate',
        expect.objectContaining({ method: 'POST' })
      );
      expect(result).toEqual(mockResponse);
    });

    it('should get device token info', async () => {
      // Given
      const mockResponse: DeviceTokenResponse = {
        maskedToken: 'existing***789',
        expiresAt: null,
        message: 'Token info retrieved',
        success: true,
      };

      fetchMock.mockResolvedValue(createMockResponse(mockResponse));

      // When
      const result = await apiService.getDeviceTokenInfo('device-001');

      // Then
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/devices/device-001/token',
        expect.any(Object)
      );
      expect(result).toEqual(mockResponse);
    });

    it('should revoke device token', async () => {
      // Given
      const mockResponse: DeviceTokenResponse = {
        message: 'Token revoked successfully',
        success: true,
      };

      fetchMock.mockResolvedValue(createMockResponse(mockResponse));

      // When
      const result = await apiService.revokeDeviceToken('device-001');

      // Then
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/devices/device-001/token',
        expect.objectContaining({ method: 'DELETE' })
      );
      expect(result).toEqual(mockResponse);
    });
  });

  describe('Telemetry Data', () => {
    it('should query telemetry data with date range', async () => {
      // Given
      const mockData = [
        { deviceId: 'device-001', timestamp: '2024-01-01T00:00:00Z', kwConsumption: 100.5 },
      ];

      fetchMock.mockResolvedValue(createMockResponse(mockData));

      // When
      const result = await apiService.queryTelemetry(
        'device-001',
        '2024-01-01T00:00:00Z',
        '2024-01-02T00:00:00Z'
      );

      // Then
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/data/query?'),
        expect.any(Object)
      );
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('deviceId=device-001'),
        expect.any(Object)
      );
      expect(result).toEqual(mockData);
    });

    it('should get latest telemetry for multiple devices', async () => {
      // Given
      const mockData = [
        { deviceId: 'device-001', latest: { deviceId: 'device-001', timestamp: '2024-01-01T00:00:00Z' } },
        { deviceId: 'device-002', latest: { deviceId: 'device-002', timestamp: '2024-01-01T00:00:00Z' } },
      ];

      fetchMock.mockResolvedValue(createMockResponse(mockData));

      // When
      const result = await apiService.getLatestTelemetry(['device-001', 'device-002']);

      // Then
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/data/latest?'),
        expect.any(Object)
      );
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('deviceIds=device-001%2Cdevice-002'),
        expect.any(Object)
      );
      expect(result).toEqual(mockData);
    });

    it('should get latest telemetry for single device', async () => {
      // Given
      const mockData = { deviceId: 'device-001', timestamp: '2024-01-01T00:00:00Z', kwConsumption: 100.5 };

      fetchMock.mockResolvedValue(createMockResponse(mockData));

      // When
      const result = await apiService.getLatestForDevice('device-001');

      // Then
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/data/latest/device-001',
        expect.any(Object)
      );
      expect(result).toEqual(mockData);
    });
  });

  describe('Rules Management', () => {
    it('should get all rules', async () => {
      // Given
      const mockRules: Rule[] = [
        {
          id: 'rule-001',
          name: 'High Voltage',
          deviceId: 'device-001',
          variable: 'voltage',
          operator: 'GT',
          threshold: 250,
          enabled: true,
          createdAt: '2024-01-01T00:00:00Z',
        },
      ];

      fetchMock.mockResolvedValue(createMockResponse(mockRules));

      // When
      const result = await apiService.getRules();

      // Then
      expect(fetchMock).toHaveBeenCalledWith('/api/v1/rules', expect.any(Object));
      expect(result).toEqual(mockRules);
    });

    it('should create rule', async () => {
      // Given
      const newRule = {
        name: 'Low Voltage',
        deviceId: 'device-001',
        variable: 'voltage',
        operator: 'LT' as const,
        threshold: 200,
        enabled: true,
      };

      const createdRule: Rule = {
        ...newRule,
        id: 'rule-002',
        createdAt: '2024-01-01T00:00:00Z',
      };

      fetchMock.mockResolvedValue(createMockResponse(createdRule));

      // When
      const result = await apiService.createRule(newRule);

      // Then
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/rules',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(newRule),
        })
      );
      expect(result).toEqual(createdRule);
    });

    it('should update rule', async () => {
      // Given
      const updates = { threshold: 300 };
      const updatedRule: Rule = {
        id: 'rule-001',
        name: 'High Voltage',
        deviceId: 'device-001',
        variable: 'voltage',
        operator: 'GT',
        threshold: 300,
        enabled: true,
        createdAt: '2024-01-01T00:00:00Z',
      };

      fetchMock.mockResolvedValue(createMockResponse(updatedRule));

      // When
      const result = await apiService.updateRule('rule-001', updates);

      // Then
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/rules/rule-001',
        expect.objectContaining({
          method: 'PUT',
          body: JSON.stringify(updates),
        })
      );
      expect(result).toEqual(updatedRule);
    });

    it('should delete rule', async () => {
      // Given
      fetchMock.mockResolvedValue({
        ok: true,
        status: 204,
        headers: { get: () => '0' },
      });

      // When
      await apiService.deleteRule('rule-001');

      // Then
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/rules/rule-001',
        expect.objectContaining({ method: 'DELETE' })
      );
    });
  });

  describe('Alerts Management', () => {
    it('should get all alerts', async () => {
      // Given
      const mockAlerts: Alert[] = [
        {
          id: 'alert-001',
          ruleId: 'rule-001',
          ruleName: 'High Voltage',
          deviceId: 'device-001',
          message: 'Voltage exceeded threshold',
          severity: 'HIGH',
          timestamp: '2024-01-01T00:00:00Z',
          acknowledged: false,
        },
      ];

      fetchMock.mockResolvedValue(createMockResponse(mockAlerts));

      // When
      const result = await apiService.getAlerts();

      // Then
      expect(fetchMock).toHaveBeenCalledWith('/api/v1/alerts', expect.any(Object));
      expect(result).toEqual(mockAlerts);
    });

    it('should acknowledge alert', async () => {
      // Given
      fetchMock.mockResolvedValue({
        ok: true,
        status: 204,
        headers: { get: () => '0' },
      });

      // When
      await apiService.acknowledgeAlert('alert-001');

      // Then
      expect(fetchMock).toHaveBeenCalledWith(
        '/api/v1/alerts/alert-001/acknowledge',
        expect.objectContaining({ method: 'POST' })
      );
    });
  });

  describe('Analytics', () => {
    it('should get aggregated data with all parameters', async () => {
      // Given
      const mockData = { min: 100, max: 500, avg: 300 };

      fetchMock.mockResolvedValue(createMockResponse(mockData));

      // When
      const result = await apiService.getAggregatedData(
        'device-001',
        'kwConsumption',
        'AVG',
        '2024-01-01T00:00:00Z',
        '2024-01-02T00:00:00Z',
        '1h'
      );

      // Then
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('/api/v1/analytics/aggregate?'),
        expect.any(Object)
      );
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('deviceId=device-001'),
        expect.any(Object)
      );
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('variable=kwConsumption'),
        expect.any(Object)
      );
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining('aggregation=AVG'),
        expect.any(Object)
      );
      expect(result).toEqual(mockData);
    });
  });
});
