import { apiService } from './api';

export interface DataPlugin {
  id: number;
  name: string;
  description?: string;
  pluginType: PluginType;
  provider: PluginProvider;
  enabled: boolean;
  configuration: Record<string, any>;
  createdByUsername?: string;
  createdAt: string;
  updatedAt: string;
}

export enum PluginType {
  PROTOCOL_PARSER = 'PROTOCOL_PARSER',
  WEBHOOK = 'WEBHOOK',
  INTEGRATION = 'INTEGRATION',
  CSV_IMPORT = 'CSV_IMPORT',
}

export enum PluginProvider {
  LORAWAN_TTN = 'LORAWAN_TTN',
  MODBUS_TCP = 'MODBUS_TCP',
  SIGFOX = 'SIGFOX',
  PARTICLE_CLOUD = 'PARTICLE_CLOUD',
  HTTP_WEBHOOK = 'HTTP_WEBHOOK',
  CSV_FILE = 'CSV_FILE',
  CUSTOM_PARSER = 'CUSTOM_PARSER',
  MQTT_CUSTOM = 'MQTT_CUSTOM',
}

export interface PluginExecution {
  id: number;
  pluginId: number;
  pluginName: string;
  executedAt: string;
  status: ExecutionStatus;
  recordsProcessed: number;
  errorMessage?: string;
  durationMs?: number;
}

export enum ExecutionStatus {
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  PARTIAL = 'PARTIAL',
}

export interface CreatePluginRequest {
  name: string;
  description?: string;
  pluginType: PluginType;
  provider: PluginProvider;
  enabled: boolean;
  configuration: Record<string, any>;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

const dataPluginsService = {
  /**
   * Get all data plugins
   */
  async getPlugins(page: number = 0, size: number = 20): Promise<PageResponse<DataPlugin>> {
    const response = await apiService.request(`/plugins?page=${page}&size=${size}`, { method: 'GET' });
    return response as unknown as PageResponse<DataPlugin>;
  },

  /**
   * Get a single plugin by ID
   */
  async getPlugin(id: number): Promise<DataPlugin> {
    const response = await apiService.request<DataPlugin>(`/plugins/${id}`, { method: 'GET' });
    return response;
  },

  /**
   * Create a new plugin
   */
  async createPlugin(plugin: CreatePluginRequest): Promise<DataPlugin> {
    const response = await apiService.request<DataPlugin>('/plugins', {
      method: 'POST',
      body: JSON.stringify(plugin),
    });
    return response;
  },

  /**
   * Update an existing plugin
   */
  async updatePlugin(id: number, plugin: CreatePluginRequest): Promise<DataPlugin> {
    const response = await apiService.request<DataPlugin>(`/plugins/${id}`, {
      method: 'PUT',
      body: JSON.stringify(plugin),
    });
    return response;
  },

  /**
   * Delete a plugin
   */
  async deletePlugin(id: number): Promise<void> {
    await apiService.request<void>(`/plugins/${id}`, { method: 'DELETE' });
  },

  /**
   * Get execution history for a plugin
   */
  async getExecutions(pluginId: number, page: number = 0, size: number = 20): Promise<PageResponse<PluginExecution>> {
    const response = await apiService.request(`/plugins/${pluginId}/executions?page=${page}&size=${size}`, { method: 'GET' });
    return response as unknown as PageResponse<PluginExecution>;
  },

  /**
   * Get available plugin providers
   */
  async getProviders(): Promise<Record<string, string>> {
    const response = await apiService.request<Record<string, string>>('/plugins/providers', { method: 'GET' });
    return response;
  },
};

export default dataPluginsService;
