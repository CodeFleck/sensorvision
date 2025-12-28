import React, { useState, useEffect } from 'react';
import {
  Search,
  Download,
  Star,
  CheckCircle,
  Package,
  Filter,
  X,
  Power,
  PowerOff,
  Trash2,
  Settings,
  Verified,
  Award,
  Loader,
} from 'lucide-react';
import toast from 'react-hot-toast';
import pluginMarketplaceService from '../services/pluginMarketplaceService';
import { PluginRegistry, InstalledPlugin } from '../types';
import PluginDetailsModal from '../components/plugins/PluginDetailsModal';
import PluginConfigModal from '../components/plugins/PluginConfigModal';

type Tab = 'marketplace' | 'installed';

const PluginMarketplace: React.FC = () => {
  // Feature flag - set to true when marketplace is ready
  const MARKETPLACE_ENABLED = false;

  const [activeTab, setActiveTab] = useState<Tab>('marketplace');
  const [marketplacePlugins, setMarketplacePlugins] = useState<PluginRegistry[]>([]);
  const [installedPlugins, setInstalledPlugins] = useState<InstalledPlugin[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [showFilters, setShowFilters] = useState(false);
  const [selectedPlugin, setSelectedPlugin] = useState<PluginRegistry | null>(null);
  const [detailsModalOpen, setDetailsModalOpen] = useState(false);
  const [configModalOpen, setConfigModalOpen] = useState(false);
  const [pluginToConfig, setPluginToConfig] = useState<string | null>(null);

  const categories = [
    'DATA_INGESTION',
    'NOTIFICATION',
    'ANALYTICS',
    'INTEGRATION',
    'UTILITY',
  ];

  useEffect(() => {
    if (MARKETPLACE_ENABLED) {
      loadData();
    } else {
      setLoading(false);
    }
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [marketplace, installed] = await Promise.all([
        pluginMarketplaceService.getAllPlugins(),
        pluginMarketplaceService.getInstalledPlugins(),
      ]);
      setMarketplacePlugins(marketplace as PluginRegistry[]);
      setInstalledPlugins(installed as InstalledPlugin[]);
    } catch (error) {
      console.error('Failed to load plugins:', error);
      toast.error('Failed to load plugins');
    } finally {
      setLoading(false);
    }
  };

  const handleInstall = async (pluginKey: string) => {
    try {
      await pluginMarketplaceService.installPlugin(pluginKey);
      toast.success('Plugin installed successfully');
      await loadData();
    } catch (error) {
      console.error('Failed to install plugin:', error);
      toast.error('Failed to install plugin');
    }
  };

  const handleUninstall = async (pluginKey: string) => {
    if (!confirm('Are you sure you want to uninstall this plugin?')) {
      return;
    }

    try {
      await pluginMarketplaceService.uninstallPlugin(pluginKey);
      toast.success('Plugin uninstalled successfully');
      await loadData();
    } catch (error) {
      console.error('Failed to uninstall plugin:', error);
      toast.error('Failed to uninstall plugin');
    }
  };

  const handleActivate = async (pluginKey: string) => {
    try {
      await pluginMarketplaceService.activatePlugin(pluginKey);
      toast.success('Plugin activated successfully');
      await loadData();
    } catch (error) {
      console.error('Failed to activate plugin:', error);
      toast.error('Failed to activate plugin');
    }
  };

  const handleDeactivate = async (pluginKey: string) => {
    try {
      await pluginMarketplaceService.deactivatePlugin(pluginKey);
      toast.success('Plugin deactivated successfully');
      await loadData();
    } catch (error) {
      console.error('Failed to deactivate plugin:', error);
      toast.error('Failed to deactivate plugin');
    }
  };

  const handleViewDetails = (plugin: PluginRegistry) => {
    setSelectedPlugin(plugin);
    setDetailsModalOpen(true);
  };

  const handleConfigure = (pluginKey: string) => {
    setPluginToConfig(pluginKey);
    setConfigModalOpen(true);
  };

  const isPluginInstalled = (pluginKey: string): boolean => {
    return installedPlugins.some((p) => p.pluginKey === pluginKey);
  };

  const getInstalledPlugin = (pluginKey: string): InstalledPlugin | undefined => {
    return installedPlugins.find((p) => p.pluginKey === pluginKey);
  };

  const filteredMarketplacePlugins = marketplacePlugins.filter((plugin) => {
    const matchesSearch =
      plugin.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      plugin.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      plugin.author?.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesCategory = !selectedCategory || plugin.category === selectedCategory;

    return matchesSearch && matchesCategory;
  });

  const filteredInstalledPlugins = installedPlugins.filter((plugin) =>
    plugin.pluginName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const getCategoryColor = (category: string): string => {
    const colors: Record<string, string> = {
      DATA_INGESTION: 'bg-blue-100 text-blue-800',
      NOTIFICATION: 'bg-green-100 text-green-800',
      ANALYTICS: 'bg-purple-100 text-purple-800',
      INTEGRATION: 'bg-orange-100 text-orange-800',
      UTILITY: 'bg-gray-100 text-gray-800',
    };
    return colors[category] || 'bg-gray-100 text-gray-800';
  };

  const formatCategory = (category: string): string => {
    return category.replace(/_/g, ' ');
  };

  const getStatusColor = (status: string): string => {
    const colors: Record<string, string> = {
      PENDING: 'bg-yellow-100 text-yellow-800',
      INSTALLED: 'bg-gray-100 text-gray-800',
      ACTIVE: 'bg-green-100 text-green-800',
      FAILED: 'bg-red-100 text-red-800',
      UNINSTALLED: 'bg-gray-100 text-gray-400',
    };
    return colors[status] || 'bg-gray-100 text-gray-800';
  };

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Plugin Marketplace</h1>
            <p className="text-gray-600 mt-1">
              Extend Industrial Cloud with powerful plugins for data ingestion, notifications, and
              integrations
            </p>
          </div>
        </div>
      </div>

      {/* Coming Soon Banner or Marketplace Content */}
      {!MARKETPLACE_ENABLED ? (
        <div className="bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50 border-2 border-blue-200 rounded-xl p-16 text-center shadow-lg">
          <div className="max-w-2xl mx-auto">
            <div className="flex justify-center mb-6">
              <div className="bg-white p-6 rounded-full shadow-md">
                <Package className="w-16 h-16 text-blue-600" />
              </div>
            </div>
            <h2 className="text-4xl font-bold text-gray-900 mb-4">Coming Soon</h2>
            <p className="text-xl text-gray-700 mb-6">
              The Plugin Marketplace is currently under development
            </p>
            <div className="bg-white/60 backdrop-blur-sm rounded-lg p-6 mb-6">
              <p className="text-gray-600 leading-relaxed">
                We&apos;re working hard to bring you an extensive collection of plugins to extend
                Industrial Cloud&apos;s capabilities. Soon you&apos;ll be able to browse, install, and manage
                plugins for data ingestion, notifications, analytics, integrations, and more.
              </p>
            </div>
            <div className="flex items-center justify-center gap-3 text-sm text-gray-500">
              <Loader className="w-4 h-4 animate-spin" />
              <span>Stay tuned for updates</span>
            </div>
          </div>
        </div>
      ) : (
        <>
          {/* Tabs */}
          <div className="mb-6 border-b border-gray-200">
            <div className="flex space-x-8">
              <button
                onClick={() => setActiveTab('marketplace')}
                className={`py-2 px-1 border-b-2 font-medium text-sm transition-colors ${
                  activeTab === 'marketplace'
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <div className="flex items-center gap-2">
                  <Package className="w-4 h-4" />
                  Marketplace
                </div>
              </button>
              <button
                onClick={() => setActiveTab('installed')}
                className={`py-2 px-1 border-b-2 font-medium text-sm transition-colors ${
                  activeTab === 'installed'
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <div className="flex items-center gap-2">
                  <CheckCircle className="w-4 h-4" />
                  Installed ({installedPlugins.length})
                </div>
              </button>
            </div>
          </div>

          {/* Search and Filters */}
          <div className="mb-6 space-y-4">
            <div className="flex gap-4">
              {/* Search */}
              <div className="flex-1 relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input
                  type="text"
                  placeholder="Search plugins..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
                {searchTerm && (
                  <button
                    onClick={() => setSearchTerm('')}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    <X className="h-4 w-4" />
                  </button>
                )}
              </div>

              {/* Filter Toggle */}
              {activeTab === 'marketplace' && (
                <button
                  onClick={() => setShowFilters(!showFilters)}
                  className={`flex items-center gap-2 px-4 py-2 border rounded-lg transition-colors ${
                    showFilters
                      ? 'bg-blue-50 border-blue-500 text-blue-600'
                      : 'border-gray-300 text-gray-700 hover:bg-gray-50'
                  }`}
                >
                  <Filter className="w-4 h-4" />
                  Filters
                </button>
              )}
            </div>

            {/* Category Filters */}
            {showFilters && activeTab === 'marketplace' && (
              <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
                <div className="flex items-center gap-2 mb-3">
                  <span className="text-sm font-medium text-gray-700">Category:</span>
                </div>
                <div className="flex flex-wrap gap-2">
                  <button
                    onClick={() => setSelectedCategory('')}
                    className={`px-3 py-1 text-sm rounded-full transition-colors ${
                      !selectedCategory
                        ? 'bg-blue-600 text-white'
                        : 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50'
                    }`}
                  >
                    All
                  </button>
                  {categories.map((category) => (
                    <button
                      key={category}
                      onClick={() => setSelectedCategory(category)}
                      className={`px-3 py-1 text-sm rounded-full transition-colors ${
                        selectedCategory === category
                          ? 'bg-blue-600 text-white'
                          : 'bg-white text-gray-700 border border-gray-300 hover:bg-gray-50'
                      }`}
                    >
                      {formatCategory(category)}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Loading State */}
          {loading ? (
            <div className="flex justify-center items-center py-12">
              <Loader className="w-8 h-8 text-blue-600 animate-spin" />
            </div>
          ) : (
            <>
              {/* Marketplace Tab */}
              {activeTab === 'marketplace' && (
            <>
              {filteredMarketplacePlugins.length === 0 ? (
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-8 text-center">
                  <Package className="w-12 h-12 text-blue-400 mx-auto mb-4" />
                  <p className="text-blue-800">No plugins found matching your criteria</p>
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {filteredMarketplacePlugins.map((plugin) => {
                    const installed = isPluginInstalled(plugin.pluginKey);
                    const installedInfo = getInstalledPlugin(plugin.pluginKey);
                    const isActive = installedInfo?.status === 'ACTIVE';

                    return (
                      <div
                        key={plugin.pluginKey}
                        className="bg-white border border-gray-200 rounded-lg p-6 hover:shadow-lg transition-shadow"
                      >
                        {/* Plugin Header */}
                        <div className="flex items-start justify-between mb-4">
                          <div className="flex items-center gap-3">
                            {plugin.iconUrl ? (
                              <img
                                src={plugin.iconUrl}
                                alt={plugin.name}
                                className="w-12 h-12 rounded-lg"
                              />
                            ) : (
                              <div className="w-12 h-12 bg-gradient-to-br from-blue-400 to-blue-600 rounded-lg flex items-center justify-center">
                                <Package className="w-6 h-6 text-white" />
                              </div>
                            )}
                            <div>
                              <h3 className="font-semibold text-gray-900 flex items-center gap-2">
                                {plugin.name}
                                {plugin.isOfficial && (
                                  <span title="Official">
                                    <Award className="w-4 h-4 text-blue-600" />
                                  </span>
                                )}
                                {plugin.isVerified && (
                                  <span title="Verified">
                                    <Verified className="w-4 h-4 text-green-600" />
                                  </span>
                                )}
                              </h3>
                              <p className="text-xs text-gray-500">by {plugin.author}</p>
                            </div>
                          </div>
                        </div>

                        {/* Description */}
                        <p className="text-sm text-gray-600 mb-4 line-clamp-3">
                          {plugin.description}
                        </p>

                        {/* Metadata */}
                        <div className="flex items-center gap-4 mb-4 text-xs text-gray-500">
                          <div className="flex items-center gap-1">
                            <Star className="w-4 h-4 text-yellow-400 fill-current" />
                            <span>
                              {plugin.ratingAverage?.toFixed(1) || '0.0'} ({plugin.ratingCount})
                            </span>
                          </div>
                          <div className="flex items-center gap-1">
                            <Download className="w-4 h-4" />
                            <span>{plugin.installationCount.toLocaleString()}</span>
                          </div>
                        </div>

                        {/* Category Badge */}
                        <div className="mb-4">
                          <span
                            className={`inline-flex px-2 py-1 text-xs font-medium rounded ${getCategoryColor(
                              plugin.category
                            )}`}
                          >
                            {formatCategory(plugin.category)}
                          </span>
                        </div>

                        {/* Actions */}
                        <div className="flex gap-2">
                          {!installed ? (
                            <>
                              <button
                                onClick={() => handleInstall(plugin.pluginKey)}
                                className="flex-1 flex items-center justify-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 transition-colors"
                              >
                                <Download className="w-4 h-4" />
                                Install
                              </button>
                              <button
                                onClick={() => handleViewDetails(plugin)}
                                className="px-4 py-2 border border-gray-300 text-gray-700 text-sm rounded-lg hover:bg-gray-50 transition-colors"
                              >
                                Details
                              </button>
                            </>
                          ) : (
                            <>
                              <button
                                onClick={() =>
                                  isActive
                                    ? handleDeactivate(plugin.pluginKey)
                                    : handleActivate(plugin.pluginKey)
                                }
                                className={`flex-1 flex items-center justify-center gap-2 px-4 py-2 text-sm rounded-lg transition-colors ${
                                  isActive
                                    ? 'bg-orange-100 text-orange-700 hover:bg-orange-200'
                                    : 'bg-green-100 text-green-700 hover:bg-green-200'
                                }`}
                              >
                                {isActive ? (
                                  <>
                                    <PowerOff className="w-4 h-4" />
                                    Deactivate
                                  </>
                                ) : (
                                  <>
                                    <Power className="w-4 h-4" />
                                    Activate
                                  </>
                                )}
                              </button>
                              <button
                                onClick={() => handleConfigure(plugin.pluginKey)}
                                className="px-3 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50"
                                title="Configure"
                              >
                                <Settings className="w-4 h-4" />
                              </button>
                            </>
                          )}
                        </div>

                        {/* Installed Badge */}
                        {installed && (
                          <div className="mt-3 flex items-center justify-center gap-2 text-xs text-green-600">
                            <CheckCircle className="w-4 h-4" />
                            Installed
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </>
          )}

          {/* Installed Tab */}
          {activeTab === 'installed' && (
            <>
              {filteredInstalledPlugins.length === 0 ? (
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-8 text-center">
                  <Package className="w-12 h-12 text-blue-400 mx-auto mb-4" />
                  <p className="text-blue-800 mb-4">No plugins installed yet</p>
                  <button
                    onClick={() => setActiveTab('marketplace')}
                    className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                  >
                    Browse Marketplace
                  </button>
                </div>
              ) : (
                <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
                  <table className="w-full">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Plugin
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Version
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Status
                        </th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Installed
                        </th>
                        <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Actions
                        </th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {filteredInstalledPlugins.map((plugin) => {
                        const isActive = plugin.status === 'ACTIVE';
                        return (
                          <tr key={plugin.id} className="hover:bg-gray-50">
                            <td className="px-6 py-4">
                              <div>
                                <div className="text-sm font-medium text-gray-900">
                                  {plugin.pluginName}
                                </div>
                                <div className="text-sm text-gray-500">{plugin.pluginKey}</div>
                              </div>
                            </td>
                            <td className="px-6 py-4">
                              <span className="text-sm text-gray-900">{plugin.version}</span>
                            </td>
                            <td className="px-6 py-4">
                              <span
                                className={`inline-flex px-2 py-1 text-xs font-medium rounded ${getStatusColor(
                                  plugin.status
                                )}`}
                              >
                                {plugin.status}
                              </span>
                            </td>
                            <td className="px-6 py-4">
                              <span className="text-sm text-gray-500">
                                {new Date(plugin.installedAt).toLocaleDateString()}
                              </span>
                            </td>
                            <td className="px-6 py-4 text-right">
                              <div className="flex items-center justify-end gap-2">
                                <button
                                  onClick={() => handleConfigure(plugin.pluginKey)}
                                  className="p-1 text-gray-600 hover:text-blue-600"
                                  title="Configure"
                                >
                                  <Settings className="w-4 h-4" />
                                </button>
                                <button
                                  onClick={() =>
                                    isActive
                                      ? handleDeactivate(plugin.pluginKey)
                                      : handleActivate(plugin.pluginKey)
                                  }
                                  className={`p-1 ${
                                    isActive
                                      ? 'text-orange-600 hover:text-orange-800'
                                      : 'text-green-600 hover:text-green-800'
                                  }`}
                                  title={isActive ? 'Deactivate' : 'Activate'}
                                >
                                  {isActive ? (
                                    <PowerOff className="w-4 h-4" />
                                  ) : (
                                    <Power className="w-4 h-4" />
                                  )}
                                </button>
                                <button
                                  onClick={() => handleUninstall(plugin.pluginKey)}
                                  className="p-1 text-red-600 hover:text-red-800"
                                  title="Uninstall"
                                >
                                  <Trash2 className="w-4 h-4" />
                                </button>
                              </div>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          )}
        </>
      )}
        </>
      )}

      {/* Plugin Details Modal */}
      {selectedPlugin && (
        <PluginDetailsModal
          open={detailsModalOpen}
          plugin={selectedPlugin}
          onClose={() => {
            setDetailsModalOpen(false);
            setSelectedPlugin(null);
          }}
          onInstall={handleInstall}
        />
      )}

      {/* Plugin Configuration Modal */}
      {pluginToConfig && (
        <PluginConfigModal
          open={configModalOpen}
          pluginKey={pluginToConfig}
          onClose={() => {
            setConfigModalOpen(false);
            setPluginToConfig(null);
            loadData();
          }}
        />
      )}
    </div>
  );
};

export default PluginMarketplace;
