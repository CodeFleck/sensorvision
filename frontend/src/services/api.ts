import { Device, DeviceTokenResponse, TelemetryPoint, LatestTelemetry, Rule, Alert, Dashboard, Widget, WidgetCreateRequest, DashboardCreateRequest, Event, EventType, EventSeverity, NotificationPreference, NotificationPreferenceRequest, NotificationLog, NotificationStats, NotificationChannel, IssueSubmission, IssueSubmissionRequest, IssueStatus, AdminIssue, IssueComment, IssueCommentRequest, Playlist, PlaylistCreateRequest, PlaylistUpdateRequest, PhoneNumber, PhoneNumberAddRequest, PhoneNumberVerifyRequest, SmsSettings, SmsSettingsUpdateRequest, SmsDeliveryLog, User, Organization, PluginRegistry, InstalledPlugin, PluginRating } from '../types';

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

  async performBulkDeviceOperation(operation: string, deviceIds: string[]): Promise<{
    totalRequested: number;
    successCount: number;
    failureCount: number;
    message: string;
  }> {
    return this.request('/devices/bulk', {
      method: 'POST',
      body: JSON.stringify({
        deviceIds,
        operation,
      }),
    });
  }

  // Device Token Management
  async generateDeviceToken(deviceId: string): Promise<DeviceTokenResponse> {
    return this.request<DeviceTokenResponse>(`/devices/${deviceId}/token/generate`, {
      method: 'POST',
    });
  }

  async rotateDeviceToken(deviceId: string): Promise<DeviceTokenResponse> {
    return this.request<DeviceTokenResponse>(`/devices/${deviceId}/token/rotate`, {
      method: 'POST',
    });
  }

  async getDeviceTokenInfo(deviceId: string): Promise<DeviceTokenResponse> {
    return this.request<DeviceTokenResponse>(`/devices/${deviceId}/token`);
  }

  async revokeDeviceToken(deviceId: string): Promise<DeviceTokenResponse> {
    return this.request<DeviceTokenResponse>(`/devices/${deviceId}/token`, {
      method: 'DELETE',
    });
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

  // Issue Comments (User)
  async getUserIssueComments(issueId: number): Promise<IssueComment[]> {
    return this.request<IssueComment[]>(`/support/issues/${issueId}/comments`);
  }

  async addUserComment(issueId: number, comment: IssueCommentRequest, file?: File): Promise<IssueComment> {
    // If there's a file, use FormData instead of JSON
    if (file) {
      const formData = new FormData();
      formData.append('message', comment.message);
      formData.append('internal', String(comment.internal || false));
      formData.append('file', file);

      const token = localStorage.getItem('accessToken');
      const headers: Record<string, string> = {};
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const response = await fetch(`${API_BASE}/support/issues/${issueId}/comments`, {
        method: 'POST',
        headers,
        body: formData,
      });

      if (!response.ok) {
        if (response.status === 401) {
          localStorage.removeItem('accessToken');
          window.location.href = '/login';
          throw new Error('Session expired. Please login again.');
        }
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.detail || errorData.message || 'Failed to add comment');
      }

      return response.json();
    }

    // No file - use regular JSON request
    return this.request<IssueComment>(`/support/issues/${issueId}/comments`, {
      method: 'POST',
      body: JSON.stringify(comment),
    });
  }

  async downloadCommentAttachment(commentId: number): Promise<Blob> {
    const token = localStorage.getItem('accessToken');
    const headers: Record<string, string> = {};
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE}/support/comments/${commentId}/attachment`, {
      headers,
    });

    if (!response.ok) {
      throw new Error('Failed to download attachment');
    }

    return response.blob();
  }

  async markTicketAsViewed(issueId: number): Promise<void> {
    await this.request<void>(`/support/issues/${issueId}/mark-viewed`, {
      method: 'POST',
    });
  }

  async getUnreadTicketCount(): Promise<{ unreadCount: number }> {
    return this.request<{ unreadCount: number }>('/support/issues/unread-count');
  }

  // Admin Issue Management
  async getAdminIssues(status?: IssueStatus): Promise<AdminIssue[]> {
    const query = status ? `?status=${status}` : '';
    return this.request<AdminIssue[]>(`/admin/support/issues${query}`);
  }

  async getAdminIssueById(id: number): Promise<IssueSubmission> {
    return this.request<IssueSubmission>(`/admin/support/issues/${id}`);
  }

  async updateIssueStatus(id: number, status: IssueStatus): Promise<IssueSubmission> {
    return this.request<IssueSubmission>(`/admin/support/issues/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    });
  }

  async getAdminIssueComments(issueId: number): Promise<IssueComment[]> {
    return this.request<IssueComment[]>(`/admin/support/issues/${issueId}/comments`);
  }

  async addAdminComment(issueId: number, comment: IssueCommentRequest): Promise<IssueComment> {
    return this.request<IssueComment>(`/admin/support/issues/${issueId}/comments`, {
      method: 'POST',
      body: JSON.stringify(comment),
    });
  }

  async getIssueScreenshot(issueId: number): Promise<Blob> {
    const token = localStorage.getItem('accessToken');
    const headers: Record<string, string> = {};
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE}/admin/support/issues/${issueId}/screenshot`, {
      headers,
    });
    if (!response.ok) {
      throw new Error('Failed to fetch screenshot');
    }
    return response.blob();
  }

  // Canned Responses Management
  async getCannedResponses(params?: { category?: string; sortByPopularity?: boolean; includeInactive?: boolean }): Promise<any[]> {
    const queryParams = new URLSearchParams();
    if (params?.category) queryParams.append('category', params.category);
    if (params?.sortByPopularity) queryParams.append('sortByPopularity', 'true');
    if (params?.includeInactive) queryParams.append('includeInactive', 'true');

    const query = queryParams.toString();
    return this.request<any[]>(`/admin/canned-responses${query ? `?${query}` : ''}`);
  }

  async getCannedResponseById(id: number): Promise<any> {
    return this.request<any>(`/admin/canned-responses/${id}`);
  }

  async createCannedResponse(data: { title: string; body: string; category?: string; active?: boolean }): Promise<any> {
    return this.request<any>('/admin/canned-responses', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async updateCannedResponse(id: number, data: { title: string; body: string; category?: string; active?: boolean }): Promise<any> {
    return this.request<any>(`/admin/canned-responses/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async deleteCannedResponse(id: number): Promise<void> {
    await this.request(`/admin/canned-responses/${id}`, {
      method: 'DELETE',
    });
  }

  async markCannedResponseAsUsed(id: number): Promise<void> {
    await this.request(`/admin/canned-responses/${id}/use`, {
      method: 'POST',
    });
  }

  // Playlist Management
  async getPlaylists(): Promise<Playlist[]> {
    return this.request<Playlist[]>('/playlists');
  }

  async getPlaylist(id: number): Promise<Playlist> {
    return this.request<Playlist>(`/playlists/${id}`);
  }

  async createPlaylist(data: PlaylistCreateRequest): Promise<Playlist> {
    return this.request<Playlist>('/playlists', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async updatePlaylist(id: number, data: PlaylistUpdateRequest): Promise<Playlist> {
    return this.request<Playlist>(`/playlists/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async deletePlaylist(id: number): Promise<void> {
    await this.request(`/playlists/${id}`, {
      method: 'DELETE',
    });
  }

  // Phone Number Management
  async getPhoneNumbers(): Promise<PhoneNumber[]> {
    return this.request<PhoneNumber[]>('/phone-numbers');
  }

  async addPhoneNumber(data: PhoneNumberAddRequest): Promise<{ success: boolean; data: PhoneNumber; message: string }> {
    return this.request('/phone-numbers', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async verifyPhoneNumber(phoneId: string, data: PhoneNumberVerifyRequest): Promise<{ success: boolean; data: string; message: string }> {
    return this.request(`/phone-numbers/${phoneId}/verify`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async resendVerificationCode(phoneId: string): Promise<{ success: boolean; message: string }> {
    return this.request(`/phone-numbers/${phoneId}/resend-code`, {
      method: 'POST',
    });
  }

  async setPrimaryPhone(phoneId: string): Promise<{ success: boolean; message: string }> {
    return this.request(`/phone-numbers/${phoneId}/set-primary`, {
      method: 'PUT',
    });
  }

  async togglePhoneEnabled(phoneId: string): Promise<{ success: boolean; message: string }> {
    return this.request(`/phone-numbers/${phoneId}/toggle`, {
      method: 'PUT',
    });
  }

  async deletePhoneNumber(phoneId: string): Promise<void> {
    await this.request(`/phone-numbers/${phoneId}`, {
      method: 'DELETE',
    });
  }

  // SMS Settings Management (Admin only)
  async getSmsSettings(): Promise<SmsSettings> {
    return this.request<SmsSettings>('/sms-settings');
  }

  async updateSmsSettings(data: SmsSettingsUpdateRequest): Promise<SmsSettings> {
    return this.request<SmsSettings>('/sms-settings', {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async resetMonthlySmsCounters(): Promise<{ success: boolean; message: string }> {
    return this.request('/sms-settings/reset-monthly-counters', {
      method: 'POST',
    });
  }

  // SMS Delivery Logs (optional - for dashboard)
  async getSmsDeliveryLogs(limit?: number, offset?: number): Promise<SmsDeliveryLog[]> {
    const params = new URLSearchParams();
    if (limit) params.append('limit', limit.toString());
    if (offset) params.append('offset', offset.toString());
    return this.request<SmsDeliveryLog[]>(`/sms-delivery-logs?${params}`);
  }

  // Admin User Management
  async getAllUsers(): Promise<User[]> {
    return this.request<User[]>('/admin/users');
  }

  async getUser(userId: number): Promise<User> {
    return this.request<User>(`/admin/users/${userId}`);
  }

  async enableUser(userId: number): Promise<{ success: boolean; data: User; message: string }> {
    return this.request(`/admin/users/${userId}/enable`, {
      method: 'PUT',
    });
  }

  async disableUser(userId: number): Promise<{ success: boolean; data: User; message: string }> {
    return this.request(`/admin/users/${userId}/disable`, {
      method: 'PUT',
    });
  }

  async updateUser(userId: number, data: { firstName?: string; lastName?: string; email?: string }): Promise<{ success: boolean; data: User; message: string }> {
    return this.request(`/admin/users/${userId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async deleteUser(userId: number): Promise<void> {
    await this.request(`/admin/users/${userId}`, {
      method: 'DELETE',
    });
  }

  async getUsersByOrganization(organizationId: number): Promise<User[]> {
    return this.request<User[]>(`/admin/users/organization/${organizationId}`);
  }

  // Admin Organization Management
  async getAllOrganizations(): Promise<Organization[]> {
    return this.request<Organization[]>('/admin/organizations');
  }

  async getOrganization(organizationId: number): Promise<Organization> {
    return this.request<Organization>(`/admin/organizations/${organizationId}`);
  }

  async enableOrganization(organizationId: number): Promise<{ success: boolean; data: Organization; message: string }> {
    return this.request(`/admin/organizations/${organizationId}/enable`, {
      method: 'PUT',
    });
  }

  async disableOrganization(organizationId: number): Promise<{ success: boolean; data: Organization; message: string }> {
    return this.request(`/admin/organizations/${organizationId}/disable`, {
      method: 'PUT',
    });
  }

  async updateOrganization(organizationId: number, data: { name?: string; description?: string }): Promise<{ success: boolean; data: Organization; message: string }> {
    return this.request(`/admin/organizations/${organizationId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async deleteOrganization(organizationId: number): Promise<void> {
    await this.request(`/admin/organizations/${organizationId}`, {
      method: 'DELETE',
    });
  }

  // Global Rules Management
  async getGlobalRules(): Promise<any[]> {
    return this.request('/global-rules');
  }

  async getGlobalRule(ruleId: string): Promise<any> {
    return this.request(`/global-rules/${ruleId}`);
  }

  async createGlobalRule(rule: any): Promise<any> {
    return this.request('/global-rules', {
      method: 'POST',
      body: JSON.stringify(rule),
    });
  }

  async updateGlobalRule(ruleId: string, rule: any): Promise<any> {
    return this.request(`/global-rules/${ruleId}`, {
      method: 'PUT',
      body: JSON.stringify(rule),
    });
  }

  async deleteGlobalRule(ruleId: string): Promise<void> {
    await this.request(`/global-rules/${ruleId}`, {
      method: 'DELETE',
    });
  }

  async toggleGlobalRule(ruleId: string): Promise<any> {
    return this.request(`/global-rules/${ruleId}/toggle`, {
      method: 'POST',
    });
  }

  async evaluateGlobalRule(ruleId: string): Promise<void> {
    await this.request(`/global-rules/${ruleId}/evaluate`, {
      method: 'POST',
    });
  }

  // Global Alerts Management
  async getGlobalAlerts(params?: { unacknowledgedOnly?: boolean; page?: number; size?: number }): Promise<any> {
    const searchParams = new URLSearchParams();
    if (params?.unacknowledgedOnly) searchParams.append('unacknowledgedOnly', 'true');
    if (params?.page !== undefined) searchParams.append('page', params.page.toString());
    if (params?.size !== undefined) searchParams.append('size', params.size.toString());

    const query = searchParams.toString();
    return this.request(`/global-alerts${query ? `?${query}` : ''}`);
  }

  async getGlobalAlert(alertId: string): Promise<any> {
    return this.request(`/global-alerts/${alertId}`);
  }

  async acknowledgeGlobalAlert(alertId: string): Promise<any> {
    return this.request(`/global-alerts/${alertId}/acknowledge`, {
      method: 'POST',
    });
  }

  async resolveGlobalAlert(alertId: string, resolutionNote?: string): Promise<any> {
    return this.request(`/global-alerts/${alertId}/resolve`, {
      method: 'POST',
      body: JSON.stringify({ resolutionNote }),
    });
  }

  async getGlobalAlertStats(): Promise<{ unacknowledged: number; unresolved: number }> {
    return this.request('/global-alerts/stats');
  }

  // Plugin Marketplace Management

  async getAllPlugins(params?: { search?: string; category?: string; sort?: string }): Promise<PluginRegistry[]> {
    const queryParams = new URLSearchParams();
    if (params?.search) queryParams.append('search', params.search);
    if (params?.category) queryParams.append('category', params.category);
    if (params?.sort) queryParams.append('sort', params.sort);

    const url = `/marketplace/plugins${queryParams.toString() ? `?${queryParams.toString()}` : ''}`;
    return this.request(url);
  }

  async getPlugin(pluginKey: string): Promise<PluginRegistry> {
    return this.request(`/marketplace/plugins/${pluginKey}`);
  }

  async installPlugin(pluginKey: string, config?: Record<string, unknown>): Promise<InstalledPlugin> {
    return this.request(`/marketplace/plugins/${pluginKey}/install`, {
      method: 'POST',
      body: JSON.stringify({ configuration: config }),
    });
  }

  async getInstalledPlugins(): Promise<InstalledPlugin[]> {
    return this.request('/marketplace/plugins/installed');
  }

  async activatePlugin(pluginKey: string): Promise<InstalledPlugin> {
    return this.request(`/marketplace/plugins/${pluginKey}/activate`, {
      method: 'POST',
    });
  }

  async deactivatePlugin(pluginKey: string): Promise<InstalledPlugin> {
    return this.request(`/marketplace/plugins/${pluginKey}/deactivate`, {
      method: 'POST',
    });
  }

  async uninstallPlugin(pluginKey: string): Promise<void> {
    return this.request(`/marketplace/plugins/${pluginKey}`, {
      method: 'DELETE',
    });
  }

  async updatePluginConfiguration(installedPluginId: number, config: Record<string, unknown>): Promise<InstalledPlugin> {
    return this.request(`/marketplace/plugins/installed/${installedPluginId}/configuration`, {
      method: 'PUT',
      body: JSON.stringify(config),
    });
  }

  async ratePlugin(pluginKey: string, rating: number, reviewText?: string): Promise<PluginRating> {
    return this.request(`/marketplace/plugins/${pluginKey}/rate`, {
      method: 'POST',
      body: JSON.stringify({ rating, reviewText }),
    });
  }

  async getPluginRatings(pluginKey: string): Promise<PluginRating[]> {
    return this.request(`/marketplace/plugins/${pluginKey}/ratings`);
  }
}

export const apiService = new ApiService();