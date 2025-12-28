import { Device, DeviceTokenResponse, TelemetryPoint, LatestTelemetry, Rule, Alert, Dashboard, Widget, WidgetCreateRequest, DashboardCreateRequest, IssueSubmission, IssueSubmissionRequest, IssueStatus, AdminIssue, IssueComment, IssueCommentRequest, Playlist, PlaylistCreateRequest, PlaylistUpdateRequest, PhoneNumber, PhoneNumberAddRequest, PhoneNumberVerifyRequest, SmsSettings, SmsSettingsUpdateRequest, SmsDeliveryLog, User, Organization, PluginRegistry, InstalledPlugin, PluginRating, DeviceVariable, VariableValue, VariableStatistics } from '../types';

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
        localStorage.removeItem('accessToken');
        window.location.href = '/login';
        throw new Error('Session expired. Please login again.');
      }

      try {
        const errorData = await response.json();
        let errorMessage = errorData.detail || errorData.title || response.statusText;
        if (errorData.developerMessage) {
          errorMessage += `\n\nDeveloper Info: ${errorData.developerMessage}`;
        }
        if (errorData.errorType) {
          errorMessage += ` (${errorData.errorType})`;
        }
        throw new Error(errorMessage);
      } catch (parseError) {
        if (parseError instanceof Error && parseError.message.includes('Developer Info')) {
          throw parseError;
        }
        throw new Error(`API Error: ${response.status} ${response.statusText}`);
      }
    }

    if (response.status === 204 || response.headers.get('content-length') === '0') {
      return undefined as T;
    }

    return response.json();
  }

  // Generic HTTP methods for use by other services
  async get<T>(endpoint: string): Promise<{ data: T }> {
    const data = await this.request<T>(endpoint, { method: 'GET' });
    return { data };
  }

  async post<T>(endpoint: string, body?: any): Promise<{ data: T }> {
    const data = await this.request<T>(endpoint, {
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
    });
    return { data };
  }

  async put<T>(endpoint: string, body?: any): Promise<{ data: T }> {
    const data = await this.request<T>(endpoint, {
      method: 'PUT',
      body: body ? JSON.stringify(body) : undefined,
    });
    return { data };
  }

  async patch<T>(endpoint: string, body?: any): Promise<{ data: T }> {
    const data = await this.request<T>(endpoint, {
      method: 'PATCH',
      body: body ? JSON.stringify(body) : undefined,
    });
    return { data };
  }

  async delete<T = void>(endpoint: string): Promise<{ data: T }> {
    const data = await this.request<T>(endpoint, { method: 'DELETE' });
    return { data };
  }

  // Support / Issues
  async submitIssue(issue: IssueSubmissionRequest): Promise<IssueSubmission> {
    return this.request<IssueSubmission>('/support/issues', {
      method: 'POST',
      body: JSON.stringify(issue),
    });
  }

  async markTicketAsViewed(issueId: number): Promise<void> {
    await this.request<void>(`/support/issues/${issueId}/mark-viewed`, {
      method: 'POST',
    });
  }

  async getUnreadTicketCount(): Promise<{ unreadCount: number }> {
    return this.request<{ unreadCount: number }>('/support/issues/unread-count');
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

  async getAdminIssues(): Promise<AdminIssue[]> {
    return this.request<AdminIssue[]>('/admin/support/issues');
  }

  async getUserIssues(): Promise<IssueSubmission[]> {
    return this.request<IssueSubmission[]>('/support/issues');
  }

  async getIssueById(issueId: number): Promise<IssueSubmission> {
    return this.request<IssueSubmission>(`/support/issues/${issueId}`);
  }

  async getUserIssueComments(issueId: number): Promise<IssueComment[]> {
    return this.request<IssueComment[]>(`/support/issues/${issueId}/comments`);
  }

  async addUserComment(issueId: number, comment: IssueCommentRequest, attachment?: File): Promise<IssueComment> {
    const token = localStorage.getItem('accessToken');
    const formData = new FormData();
    formData.append('message', comment.message);

    if (attachment) {
      formData.append('attachment', attachment);
    }

    const response = await fetch(`${API_BASE}/support/issues/${issueId}/comments`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
      body: formData,
    });

    if (!response.ok) {
      throw new Error('Failed to add comment');
    }

    return response.json();
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

  // Device Management
  async getDevices(): Promise<Device[]> {
    return this.request<Device[]>('/devices');
  }

  async getDevice(deviceId: string): Promise<Device> {
    return this.request<Device>(`/devices/${deviceId}`);
  }

  async createDevice(device: Partial<Device>): Promise<Device> {
    return this.request<Device>('/devices', {
      method: 'POST',
      body: JSON.stringify(device),
    });
  }

  async updateDevice(deviceId: string, device: Partial<Device>): Promise<Device> {
    return this.request<Device>(`/devices/${deviceId}`, {
      method: 'PUT',
      body: JSON.stringify(device),
    });
  }

  async deleteDevice(deviceId: string): Promise<void> {
    await this.request(`/devices/${deviceId}`, {
      method: 'DELETE',
    });
  }

  // Device Tokens
  async getDeviceTokenInfo(deviceId: string): Promise<DeviceTokenResponse> {
    return this.request<DeviceTokenResponse>(`/devices/${deviceId}/token`);
  }

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

  async revokeDeviceToken(deviceId: string): Promise<DeviceTokenResponse> {
    return this.request<DeviceTokenResponse>(`/devices/${deviceId}/token`, {
      method: 'DELETE',
    });
  }

  // Connectivity Checks (for Integration Wizard)
  async checkMqttConnectivity(): Promise<{
    status: string;
    message: string;
    reachable: boolean;
    host?: string;
    port?: number;
    guidance?: string;
  }> {
    return this.request('/connectivity/mqtt');
  }

  async checkHttpConnectivity(): Promise<{
    status: string;
    message: string;
    reachable: boolean;
    guidance?: string;
  }> {
    return this.request('/connectivity/http');
  }

  // Telemetry
  async queryTelemetry(deviceId: string, start: string, end: string): Promise<TelemetryPoint[]> {
    const params = new URLSearchParams({
      deviceId,
      start,
      end,
    });
    return this.request<TelemetryPoint[]>(`/data/query?${params}`);
  }

  async getLatestTelemetry(deviceIds: string[]): Promise<LatestTelemetry[]> {
    const params = new URLSearchParams({
      deviceIds: deviceIds.join(','),
    });
    return this.request<LatestTelemetry[]>(`/data/latest?${params}`);
  }

  async getLatestForDevice(deviceId: string): Promise<TelemetryPoint> {
    return this.request<TelemetryPoint>(`/data/latest/${deviceId}`);
  }

  // Rules
  async getRules(): Promise<Rule[]> {
    return this.request<Rule[]>('/rules');
  }

  async createRule(rule: Partial<Rule>): Promise<Rule> {
    return this.request<Rule>('/rules', {
      method: 'POST',
      body: JSON.stringify(rule),
    });
  }

  async updateRule(ruleId: string, rule: Partial<Rule>): Promise<Rule> {
    return this.request<Rule>(`/rules/${ruleId}`, {
      method: 'PUT',
      body: JSON.stringify(rule),
    });
  }

  async deleteRule(ruleId: string): Promise<void> {
    await this.request(`/rules/${ruleId}`, {
      method: 'DELETE',
    });
  }

  // Alerts
  async getAlerts(): Promise<Alert[]> {
    return this.request<Alert[]>('/alerts');
  }

  async acknowledgeAlert(alertId: string): Promise<void> {
    await this.request(`/alerts/${alertId}/acknowledge`, {
      method: 'POST',
    });
  }

  // Analytics
  async getAggregatedData(deviceId: string, variable: string, aggregation: string, start: string, end: string, interval: string): Promise<any> {
    const params = new URLSearchParams({
      deviceId,
      variable,
      aggregation,
      from: start,  // Backend expects 'from' parameter
      to: end,      // Backend expects 'to' parameter
      interval,
    });
    return this.request<any>(`/analytics/aggregate?${params}`);
  }

  // Events
  async getEvents(params: Record<string, string | number>): Promise<any> {
    const queryParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      queryParams.append(key, value.toString());
    });
    return this.request<any>(`/events?${queryParams}`);
  }

  // Notifications
  async getNotificationPreferences(): Promise<any[]> {
    return this.request<any[]>('/notifications/preferences');
  }

  async getNotificationLogs(page: number, size: number): Promise<any> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    return this.request<any>(`/notifications/logs?${params}`);
  }

  async getNotificationStats(): Promise<any> {
    return this.request<any>('/notifications/stats');
  }

  async saveNotificationPreference(preference: any): Promise<any> {
    return this.request<any>('/notifications/preferences', {
      method: 'POST',
      body: JSON.stringify(preference),
    });
  }

  async deleteNotificationPreference(channel: string): Promise<void> {
    await this.request(`/notifications/preferences/${channel}`, {
      method: 'DELETE',
    });
  }

  // Dashboards
  async getDashboards(): Promise<Dashboard[]> {
    return this.request<Dashboard[]>('/dashboards');
  }

  async getDefaultDashboard(): Promise<Dashboard> {
    return this.request<Dashboard>('/dashboards/default');
  }

  async getDashboard(dashboardId: number): Promise<Dashboard> {
    return this.request<Dashboard>(`/dashboards/${dashboardId}`);
  }

  async createDashboard(dashboard: DashboardCreateRequest): Promise<Dashboard> {
    return this.request<Dashboard>('/dashboards', {
      method: 'POST',
      body: JSON.stringify(dashboard),
    });
  }

  async updateDashboard(dashboardId: number, dashboard: Partial<Dashboard>): Promise<Dashboard> {
    return this.request<Dashboard>(`/dashboards/${dashboardId}`, {
      method: 'PUT',
      body: JSON.stringify(dashboard),
    });
  }

  async deleteDashboard(dashboardId: number): Promise<void> {
    await this.request(`/dashboards/${dashboardId}`, {
      method: 'DELETE',
    });
  }

  async createWidget(dashboardId: number, widget: WidgetCreateRequest): Promise<Widget> {
    return this.request<Widget>(`/dashboards/${dashboardId}/widgets`, {
      method: 'POST',
      body: JSON.stringify(widget),
    });
  }

  async updateWidget(dashboardId: number, widgetId: number, widget: Partial<Widget>): Promise<Widget> {
    return this.request<Widget>(`/dashboards/${dashboardId}/widgets/${widgetId}`, {
      method: 'PUT',
      body: JSON.stringify(widget),
    });
  }

  async deleteWidget(dashboardId: number, widgetId: number): Promise<void> {
    await this.request(`/dashboards/${dashboardId}/widgets/${widgetId}`, {
      method: 'DELETE',
    });
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

  // Role Management
  async getAllRoles(): Promise<{ id: number; name: string; description: string }[]> {
    return this.request<{ id: number; name: string; description: string }[]>('/admin/users/roles');
  }

  async updateUserRoles(userId: number, roles: string[]): Promise<{ success: boolean; data: User; message: string }> {
    return this.request(`/admin/users/${userId}/roles`, {
      method: 'PUT',
      body: JSON.stringify({ roles }),
    });
  }

  async addRoleToUser(userId: number, roleName: string): Promise<{ success: boolean; data: User; message: string }> {
    return this.request(`/admin/users/${userId}/roles/${encodeURIComponent(roleName)}`, {
      method: 'POST',
    });
  }

  async removeRoleFromUser(userId: number, roleName: string): Promise<{ success: boolean; data: User; message: string }> {
    return this.request(`/admin/users/${userId}/roles/${encodeURIComponent(roleName)}`, {
      method: 'DELETE',
    });
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

  // ========== Device Variables (EAV Pattern - Dynamic Variables) ==========

  /**
   * Get all dynamic variables for a device.
   * Variables are auto-provisioned when telemetry is received.
   */
  async getDeviceVariables(deviceId: string): Promise<DeviceVariable[]> {
    return this.request(`/devices/${deviceId}/variables`);
  }

  /**
   * Get latest values for all variables of a device.
   * Returns a map of variable names to their most recent values.
   */
  async getDeviceLatestValues(deviceId: string): Promise<Record<string, number>> {
    return this.request(`/devices/${deviceId}/variables/latest`);
  }

  /**
   * Get a specific variable details.
   */
  async getDeviceVariable(deviceId: string, variableId: number): Promise<DeviceVariable> {
    return this.request(`/devices/${deviceId}/variables/${variableId}`);
  }

  /**
   * Get time-series history for a variable.
   */
  async getVariableHistory(
    deviceId: string,
    variableId: number,
    startTime?: string,
    endTime?: string
  ): Promise<VariableValue[]> {
    const params = new URLSearchParams();
    if (startTime) params.append('startTime', startTime);
    if (endTime) params.append('endTime', endTime);
    const query = params.toString();
    return this.request(`/devices/${deviceId}/variables/${variableId}/values${query ? `?${query}` : ''}`);
  }

  /**
   * Get latest N values for a variable.
   */
  async getVariableLatestValues(
    deviceId: string,
    variableId: number,
    count = 100
  ): Promise<VariableValue[]> {
    return this.request(`/devices/${deviceId}/variables/${variableId}/values/latest?count=${count}`);
  }

  /**
   * Get statistics (avg, min, max, sum, count) for a variable.
   */
  async getVariableStatistics(
    deviceId: string,
    variableId: number,
    startTime?: string,
    endTime?: string
  ): Promise<VariableStatistics> {
    const params = new URLSearchParams();
    if (startTime) params.append('startTime', startTime);
    if (endTime) params.append('endTime', endTime);
    const query = params.toString();
    return this.request(`/devices/${deviceId}/variables/${variableId}/statistics${query ? `?${query}` : ''}`);
  }

  /**
   * Update variable metadata (display name, unit, color, etc.)
   */
  async updateDeviceVariable(
    deviceId: string,
    variableId: number,
    updates: Partial<Pick<DeviceVariable, 'displayName' | 'description' | 'unit' | 'icon' | 'color' | 'decimalPlaces' | 'minValue' | 'maxValue'>>
  ): Promise<DeviceVariable> {
    return this.request(`/devices/${deviceId}/variables/${variableId}`, {
      method: 'PUT',
      body: JSON.stringify(updates),
    });
  }

  // ========== Device Groups ==========

  /**
   * Get all device groups for the current user's organization.
   */
  async getDeviceGroups(): Promise<Array<{ id: number; name: string; description?: string }>> {
    return this.request('/device-groups');
  }

  /**
   * Create a new device group.
   */
  async createDeviceGroup(data: { name: string; description?: string }): Promise<{ id: number; name: string; description?: string }> {
    return this.request('/device-groups', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  /**
   * Add a device to a group.
   */
  async addDeviceToGroup(groupId: number, deviceId: string): Promise<void> {
    return this.request(`/device-groups/${groupId}/devices/${deviceId}`, {
      method: 'POST',
    });
  }

  /**
   * Remove a device from a group.
   */
  async removeDeviceFromGroup(groupId: number, deviceId: string): Promise<void> {
    return this.request(`/device-groups/${groupId}/devices/${deviceId}`, {
      method: 'DELETE',
    });
  }

  // ========== Device Tags ==========

  /**
   * Get all device tags for the current user's organization.
   */
  async getDeviceTags(): Promise<Array<{ id: number; name: string; color: string }>> {
    return this.request('/device-tags');
  }

  /**
   * Create a new device tag.
   */
  async createDeviceTag(data: { name: string; color: string }): Promise<{ id: number; name: string; color: string }> {
    return this.request('/device-tags', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  /**
   * Add a tag to a device.
   */
  async addTagToDevice(tagId: number, deviceId: string): Promise<void> {
    return this.request(`/device-tags/${tagId}/devices/${deviceId}`, {
      method: 'POST',
    });
  }

  /**
   * Remove a tag from a device.
   */
  async removeTagFromDevice(tagId: number, deviceId: string): Promise<void> {
    return this.request(`/device-tags/${tagId}/devices/${deviceId}`, {
      method: 'DELETE',
    });
  }

  // ========== Admin Device Management ==========

  /**
   * Get all devices across all organizations (admin only).
   */
  async getAllAdminDevices(): Promise<AdminDevice[]> {
    return this.request<AdminDevice[]>('/admin/devices');
  }

  /**
   * Get a specific device by ID (admin only).
   */
  async getAdminDevice(deviceId: string): Promise<AdminDevice> {
    return this.request<AdminDevice>(`/admin/devices/${deviceId}`);
  }

  /**
   * Get a device by external ID (admin only).
   */
  async getAdminDeviceByExternalId(externalId: string): Promise<AdminDevice> {
    return this.request<AdminDevice>(`/admin/devices/external/${externalId}`);
  }

  /**
   * Enable a device (admin only).
   */
  async enableAdminDevice(deviceId: string): Promise<{ success: boolean; data: AdminDevice; message: string }> {
    return this.request(`/admin/devices/${deviceId}/enable`, {
      method: 'PUT',
    });
  }

  /**
   * Disable a device (admin only).
   */
  async disableAdminDevice(deviceId: string): Promise<{ success: boolean; data: AdminDevice; message: string }> {
    return this.request(`/admin/devices/${deviceId}/disable`, {
      method: 'PUT',
    });
  }

  /**
   * Update a device (admin only).
   */
  async updateAdminDevice(deviceId: string, data: {
    name?: string;
    description?: string;
    location?: string;
    sensorType?: string;
    firmwareVersion?: string;
  }): Promise<{ success: boolean; data: AdminDevice; message: string }> {
    return this.request(`/admin/devices/${deviceId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  /**
   * Delete a device (admin only).
   */
  async deleteAdminDevice(deviceId: string): Promise<void> {
    await this.request(`/admin/devices/${deviceId}`, {
      method: 'DELETE',
    });
  }

  /**
   * Get devices by organization (admin only).
   */
  async getAdminDevicesByOrganization(organizationId: number): Promise<AdminDevice[]> {
    return this.request<AdminDevice[]>(`/admin/devices/organization/${organizationId}`);
  }

  /**
   * Get device statistics (admin only).
   */
  async getAdminDeviceStats(): Promise<AdminDeviceStats> {
    return this.request<AdminDeviceStats>('/admin/devices/stats');
  }

  // ========== Admin Trash Management ==========

  /**
   * Get all trash items (admin only).
   */
  async getAllTrashItems(): Promise<TrashItem[]> {
    return this.request<TrashItem[]>('/admin/trash');
  }

  /**
   * Get trash items by entity type (admin only).
   */
  async getTrashItemsByType(entityType: string): Promise<TrashItem[]> {
    return this.request<TrashItem[]>(`/admin/trash/type/${entityType}`);
  }

  /**
   * Get trash statistics (admin only).
   */
  async getTrashStats(): Promise<TrashStats> {
    return this.request<TrashStats>('/admin/trash/stats');
  }

  /**
   * Restore an item from trash (admin only).
   */
  async restoreTrashItem(trashId: number): Promise<void> {
    await this.request<void>(`/admin/trash/${trashId}/restore`, {
      method: 'POST',
    });
  }

  /**
   * Soft delete user (admin only). Returns soft delete response with undo info.
   */
  async softDeleteUser(userId: number, reason?: string): Promise<{ success: boolean; data: SoftDeleteResponse; message: string }> {
    const params = reason ? `?reason=${encodeURIComponent(reason)}` : '';
    return this.request<{ success: boolean; data: SoftDeleteResponse; message: string }>(`/admin/users/${userId}${params}`, {
      method: 'DELETE',
    });
  }

  /**
   * Soft delete device (admin only). Returns soft delete response with undo info.
   */
  async softDeleteDevice(deviceId: string, reason?: string): Promise<{ success: boolean; data: SoftDeleteResponse; message: string }> {
    const params = reason ? `?reason=${encodeURIComponent(reason)}` : '';
    return this.request<{ success: boolean; data: SoftDeleteResponse; message: string }>(`/admin/devices/${deviceId}${params}`, {
      method: 'DELETE',
    });
  }

  /**
   * Soft delete organization (admin only). Returns soft delete response with undo info.
   */
  async softDeleteOrganization(organizationId: number, reason?: string): Promise<{ success: boolean; data: SoftDeleteResponse; message: string }> {
    const params = reason ? `?reason=${encodeURIComponent(reason)}` : '';
    return this.request<{ success: boolean; data: SoftDeleteResponse; message: string }>(`/admin/organizations/${organizationId}${params}`, {
      method: 'DELETE',
    });
  }
}

// Admin Device Types
export interface AdminDevice {
  id: string;
  externalId: string;
  name: string;
  description?: string;
  active: boolean;
  location?: string;
  sensorType?: string;
  firmwareVersion?: string;
  status: string;
  lastSeenAt?: string;
  healthScore?: number;
  lastHealthCheckAt?: string;
  organizationId?: number;
  organizationName?: string;
  hasApiToken: boolean;
  tokenCreatedAt?: string;
  tokenLastUsedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminDeviceStats {
  totalDevices: number;
  activeDevices: number;
  inactiveDevices: number;
  onlineDevices: number;
  offlineDevices: number;
}

// Trash/Soft Delete Types
export interface TrashItem {
  id: number;
  entityType: 'USER' | 'DEVICE' | 'ORGANIZATION';
  entityId: string;
  entityName: string;
  entitySnapshot: Record<string, unknown>;
  deletedAt: string;
  deletedBy: string;
  deletionReason?: string;
  expiresAt: string;
  daysRemaining: number;
  organizationId?: number;
  organizationName?: string;
}

export interface TrashStats {
  totalItems: number;
  users: number;
  devices: number;
  organizations: number;
}

export interface SoftDeleteResponse {
  trashId: number;
  entityType: string;
  entityName: string;
  expiresAt: string;
  daysRemaining: number;
}

export const apiService = new ApiService();