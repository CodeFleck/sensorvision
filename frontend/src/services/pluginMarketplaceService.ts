import axios from './axiosConfig';

export interface PluginRegistry {
  key: string;
  name: string;
  description: string;
  author: string;
  version: string;
  category: 'DATA_INGESTION' | 'NOTIFICATION' | 'ANALYTICS' | 'INTEGRATION' | 'UTILITY';
  iconUrl?: string;
  websiteUrl?: string;
  documentationUrl?: string;
  repositoryUrl?: string;
  isOfficial: boolean;
  isVerified: boolean;
  downloads: number;
  averageRating: number;
  ratingCount: number;
  tags: string[];
  screenshots: string[];
  configurationSchema?: any;
  compatibleVersions: string[];
  createdAt: string;
  updatedAt: string;
}

export interface InstalledPlugin {
  id: string;
  pluginKey: string;
  pluginName: string;
  version: string;
  status: 'PENDING' | 'INSTALLED' | 'ACTIVE' | 'FAILED' | 'UNINSTALLED';
  isActive: boolean;
  configuration?: any;
  installedAt: string;
  lastActivatedAt?: string;
  errorMessage?: string;
}

export interface PluginRating {
  rating: number;
  review?: string;
}

class PluginMarketplaceService {
  private readonly BASE_URL = '/api/v1/plugins';

  // Get all marketplace plugins
  async getAllPlugins(params?: {
    category?: string;
    search?: string;
    official?: boolean;
  }): Promise<PluginRegistry[]> {
    const response = await axios.get(this.BASE_URL, { params });
    return response.data;
  }

  // Get plugin details
  async getPlugin(key: string): Promise<PluginRegistry> {
    const response = await axios.get(`${this.BASE_URL}/${key}`);
    return response.data;
  }

  // Get installed plugins
  async getInstalledPlugins(): Promise<InstalledPlugin[]> {
    const response = await axios.get(`${this.BASE_URL}/installed`);
    return response.data;
  }

  // Install plugin
  async installPlugin(key: string): Promise<InstalledPlugin> {
    const response = await axios.post(`${this.BASE_URL}/${key}/install`);
    return response.data;
  }

  // Uninstall plugin
  async uninstallPlugin(key: string): Promise<void> {
    await axios.delete(`${this.BASE_URL}/${key}`);
  }

  // Activate plugin
  async activatePlugin(key: string): Promise<InstalledPlugin> {
    const response = await axios.post(`${this.BASE_URL}/${key}/activate`);
    return response.data;
  }

  // Deactivate plugin
  async deactivatePlugin(key: string): Promise<InstalledPlugin> {
    const response = await axios.post(`${this.BASE_URL}/${key}/deactivate`);
    return response.data;
  }

  // Update plugin configuration
  async updateConfiguration(key: string, configuration: any): Promise<InstalledPlugin> {
    const response = await axios.put(`${this.BASE_URL}/${key}/configuration`, configuration);
    return response.data;
  }

  // Get default configuration
  async getDefaultConfiguration(key: string): Promise<any> {
    const response = await axios.get(`${this.BASE_URL}/${key}/default-config`);
    return response.data;
  }

  // Rate plugin
  async ratePlugin(key: string, rating: PluginRating): Promise<void> {
    await axios.post(`${this.BASE_URL}/${key}/rate`, rating);
  }

  // Get most popular plugins
  async getMostPopular(limit: number = 10): Promise<PluginRegistry[]> {
    const plugins = await this.getAllPlugins();
    return plugins
      .sort((a, b) => b.downloads - a.downloads)
      .slice(0, limit);
  }

  // Get top rated plugins
  async getTopRated(limit: number = 10): Promise<PluginRegistry[]> {
    const plugins = await this.getAllPlugins();
    return plugins
      .filter(p => p.ratingCount > 0)
      .sort((a, b) => b.averageRating - a.averageRating)
      .slice(0, limit);
  }

  // Get recent plugins
  async getRecent(limit: number = 10): Promise<PluginRegistry[]> {
    const plugins = await this.getAllPlugins();
    return plugins
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, limit);
  }

  // Search plugins
  async searchPlugins(query: string): Promise<PluginRegistry[]> {
    return this.getAllPlugins({ search: query });
  }

  // Filter by category
  async getPluginsByCategory(category: string): Promise<PluginRegistry[]> {
    return this.getAllPlugins({ category });
  }

  // Get official plugins only
  async getOfficialPlugins(): Promise<PluginRegistry[]> {
    return this.getAllPlugins({ official: true });
  }
}

export default new PluginMarketplaceService();
