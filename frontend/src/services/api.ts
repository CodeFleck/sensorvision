import { Device, TelemetryPoint, LatestTelemetry, Rule, Alert, Dashboard, Widget, WidgetCreateRequest, DashboardCreateRequest, Event, EventType, EventSeverity, NotificationPreference, NotificationPreferenceRequest, NotificationLog, NotificationStats, NotificationChannel, IssueSubmission, IssueSubmissionRequest, IssueStatus } from '../types';

const API_BASE = '/api/v1';

class ApiService {
  private async request<T>(endpoint: string, options?: RequestInit): Promise<T> {
    const token = localStorage.getItem('accessToken');
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options?.headers as Record<string, string>),
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE}${endpoint}`, {
      headers,
      ...options,
    });

    if (!response.ok) {
      if (response.status === 401) {
        // Unauthorized - clear token and reload
        localStorage.removeItem('accessToken');
        window.location.href = '/login';
        throw new Error('Session expired. Please login again.');
      }

      // Try to parse error response (ProblemDetail format from backend)
      try {
        const errorData = await response.json();

        // Build user-friendly error message with developer info if available
        let errorMessage = errorData.detail || errorData.title || response.statusText;

        // Add developer message if available (for debugging)
        if (errorData.developerMessage) {
          errorMessage += `\n\nDeveloper Info: ${errorData.developerMessage}`;
        }

        // Add error type if available
        if (errorData.errorType) {
          errorMessage += ` (${errorData.errorType})`;
        }

        throw new Error(errorMessage);
      } catch (parseError) {
        // If we can't parse the error response, fall back to status text
        if (parseError instanceof Error && parseError.message.includes('Developer Info')) {
          // Re-throw our formatted error
          throw parseError;
        }
        throw new Error(`API Error: ${response.status} ${response.statusText}`);
      }
    }

    // Handle HTTP 204 No Content responses (common for DELETE operations)
    // These responses have no body, so attempting to parse JSON will fail
    if (response.status === 204 || response.headers.get('content-length') === '0') {
      return undefined as T;
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

  // Dashboard Management
  async getDashboards(): Promise<Dashboard[]> {
    return this.request<Dashboard[]>('/dashboards');
  }

  async getDashboard(id: number): Promise<Dashboard> {
    return this.request<Dashboard>(`/dashboards/${id}`);
  }

  async getDefaultDashboard(): Promise<Dashboard> {
    return this.request<Dashboard>('/dashboards/default');
  }

  async createDashboard(dashboard: DashboardCreateRequest): Promise<Dashboard> {
    return this.request<Dashboard>('/dashboards', {
      method: 'POST',
      body: JSON.stringify(dashboard),
    });
  }

  async updateDashboard(id: number, dashboard: Partial<DashboardCreateRequest>): Promise<Dashboard> {
    return this.request<Dashboard>(`/dashboards/${id}`, {
      method: 'PUT',
      body: JSON.stringify(dashboard),
    });
  }

  async deleteDashboard(id: number): Promise<void> {
    await this.request(`/dashboards/${id}`, { method: 'DELETE' });
  }

  // Widget Management
  async getDashboardWidgets(dashboardId: number): Promise<Widget[]> {
    return this.request<Widget[]>(`/dashboards/${dashboardId}/widgets`);
  }

  async createWidget(dashboardId: number, widget: WidgetCreateRequest): Promise<Widget> {
    return this.request<Widget>(`/dashboards/${dashboardId}/widgets`, {
      method: 'POST',
      body: JSON.stringify(widget),
    });
  }

  async updateWidget(dashboardId: number, widgetId: number, widget: Partial<WidgetCreateRequest>): Promise<Widget> {
    return this.request<Widget>(`/dashboards/${dashboardId}/widgets/${widgetId}`, {
      method: 'PUT',
      body: JSON.stringify(widget),
    });
  }

  async deleteWidget(dashboardId: number, widgetId: number): Promise<void> {
    await this.request(`/dashboards/${dashboardId}/widgets/${widgetId}`, { method: 'DELETE' });
  }

  // Event Management
  async getEvents(params?: {
    eventType?: EventType;
    severity?: EventSeverity;
    deviceId?: string;
    entityType?: string;
    startTime?: string;
    endTime?: string;
    page?: number;
    size?: number;
  }): Promise<{ content: Event[]; totalElements: number; totalPages: number }> {
    const queryParams = new URLSearchParams();
    if (params?.eventType) queryParams.append('eventType', params.eventType);
    if (params?.severity) queryParams.append('severity', params.severity);
    if (params?.deviceId) queryParams.append('deviceId', params.deviceId);
    if (params?.entityType) queryParams.append('entityType', params.entityType);
    if (params?.startTime) queryParams.append('startTime', params.startTime);
    if (params?.endTime) queryParams.append('endTime', params.endTime);
    if (params?.page !== undefined) queryParams.append('page', params.page.toString());
    if (params?.size !== undefined) queryParams.append('size', params.size.toString());

    return this.request(`/events?${queryParams.toString()}`);
  }

  async getRecentEvents(hours = 24): Promise<Event[]> {
    return this.request(`/events/recent?hours=${hours}`);
  }

  async getEventStatisticsByType(hours = 24): Promise<Record<string, number>> {
    return this.request(`/events/statistics/by-type?hours=${hours}`);
  }

  async getEventStatisticsBySeverity(hours = 24): Promise<Record<string, number>> {
    return this.request(`/events/statistics/by-severity?hours=${hours}`);
  }

  // Notification Management
  async getNotificationPreferences(): Promise<NotificationPreference[]> {
    return this.request<NotificationPreference[]>('/notifications/preferences');
  }

  async saveNotificationPreference(preference: NotificationPreferenceRequest): Promise<NotificationPreference> {
    return this.request<NotificationPreference>('/notifications/preferences', {
      method: 'POST',
      body: JSON.stringify(preference),
    });
  }

  async deleteNotificationPreference(channel: NotificationChannel): Promise<void> {
    await this.request(`/notifications/preferences/${channel}`, { method: 'DELETE' });
  }

  async getNotificationLogs(page = 0, size = 20): Promise<{ content: NotificationLog[]; totalElements: number; totalPages: number }> {
    return this.request(`/notifications/logs?page=${page}&size=${size}`);
  }

  async getNotificationStats(): Promise<NotificationStats> {
    return this.request<NotificationStats>('/notifications/stats');
  }

  // Generic HTTP methods for stub/placeholder pages
  async get<T = unknown>(endpoint: string): Promise<{ data: T }> {
    const result = await this.request<T>(endpoint);
    return { data: result };
  }

  async post<T = unknown>(endpoint: string, data?: unknown): Promise<{ data: T }> {
    const result = await this.request<T>(endpoint, {
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    });
    return { data: result };
  }

  async put<T = unknown>(endpoint: string, data?: unknown): Promise<{ data: T }> {
    const result = await this.request<T>(endpoint, {
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    });
    return { data: result };
  }

  async patch<T = unknown>(endpoint: string, data?: unknown): Promise<{ data: T }> {
    const result = await this.request<T>(endpoint, {
      method: 'PATCH',
      body: data ? JSON.stringify(data) : undefined,
    });
    return { data: result };
  }

  async delete<T = unknown>(endpoint: string): Promise<{ data: T }> {
    const result = await this.request<T>(endpoint, {
      method: 'DELETE',
    });
    return { data: result };
  }

  // Issue Submission Management
  async submitIssue(issue: IssueSubmissionRequest): Promise<IssueSubmission> {
    return this.request<IssueSubmission>('/support/issues', {
      method: 'POST',
      body: JSON.stringify(issue),
    });
  }

  async getUserIssues(): Promise<IssueSubmission[]> {
    return this.request<IssueSubmission[]>('/support/issues');
  }

  async getIssueById(id: number): Promise<IssueSubmission> {
    return this.request<IssueSubmission>(`/support/issues/${id}`);
  }

  async getUserIssuesByStatus(status: IssueStatus): Promise<IssueSubmission[]> {
    return this.request<IssueSubmission[]>(`/support/issues/status/${status}`);
  }

  async getUserIssueCount(): Promise<{ count: number }> {
    return this.request<{ count: number }>('/support/issues/count');
  }
}

export const apiService = new ApiService();