import { Device, TelemetryPoint, LatestTelemetry, Rule, Alert } from '../types';

const API_BASE = '/api/v1';

class ApiService {
  private async request<T>(endpoint: string, options?: RequestInit): Promise<T> {
    const response = await fetch(`${API_BASE}${endpoint}`, {
      headers: {
        'Content-Type': 'application/json',
        ...options?.headers,
      },
      ...options,
    });

    if (!response.ok) {
      throw new Error(`API Error: ${response.statusText}`);
    }

    return response.json();
  }

  // Device Management
  async getDevices(): Promise<Device[]> {
    return this.request<Device[]>('/devices');
  }

  async getDevice(externalId: string): Promise<Device> {
    return this.request<Device>(`/devices/${externalId}`);
  }

  async createDevice(device: Omit<Device, 'status' | 'lastSeenAt'>): Promise<Device> {
    return this.request<Device>('/devices', {
      method: 'POST',
      body: JSON.stringify(device),
    });
  }

  async updateDevice(externalId: string, device: Partial<Device>): Promise<Device> {
    return this.request<Device>(`/devices/${externalId}`, {
      method: 'PUT',
      body: JSON.stringify(device),
    });
  }

  async deleteDevice(externalId: string): Promise<void> {
    await this.request(`/devices/${externalId}`, { method: 'DELETE' });
  }

  // Telemetry Data
  async queryTelemetry(
    deviceId: string,
    from: string,
    to: string
  ): Promise<TelemetryPoint[]> {
    const params = new URLSearchParams({ deviceId, from, to });
    return this.request<TelemetryPoint[]>(`/data/query?${params}`);
  }

  async getLatestTelemetry(deviceIds: string[]): Promise<LatestTelemetry[]> {
    const params = new URLSearchParams({ deviceIds: deviceIds.join(',') });
    return this.request<LatestTelemetry[]>(`/data/latest?${params}`);
  }

  async getLatestForDevice(deviceId: string): Promise<TelemetryPoint> {
    return this.request<TelemetryPoint>(`/data/latest/${deviceId}`);
  }

  // Analytics & Aggregation (to be implemented)
  async getAggregatedData(
    deviceId: string,
    variable: string,
    aggregation: 'MIN' | 'MAX' | 'AVG' | 'SUM',
    from: string,
    to: string,
    interval?: string
  ) {
    const params = new URLSearchParams({
      deviceId,
      variable,
      aggregation,
      from,
      to,
      ...(interval && { interval }),
    });
    return this.request(`/analytics/aggregate?${params}`);
  }

  // Rules Management (to be implemented)
  async getRules(): Promise<Rule[]> {
    return this.request<Rule[]>('/rules');
  }

  async createRule(rule: Omit<Rule, 'id' | 'createdAt'>): Promise<Rule> {
    return this.request<Rule>('/rules', {
      method: 'POST',
      body: JSON.stringify(rule),
    });
  }

  async updateRule(id: string, rule: Partial<Rule>): Promise<Rule> {
    return this.request<Rule>(`/rules/${id}`, {
      method: 'PUT',
      body: JSON.stringify(rule),
    });
  }

  async deleteRule(id: string): Promise<void> {
    await this.request(`/rules/${id}`, { method: 'DELETE' });
  }

  // Alerts Management (to be implemented)
  async getAlerts(): Promise<Alert[]> {
    return this.request<Alert[]>('/alerts');
  }

  async acknowledgeAlert(id: string): Promise<void> {
    await this.request(`/alerts/${id}/acknowledge`, { method: 'POST' });
  }
}

export const apiService = new ApiService();