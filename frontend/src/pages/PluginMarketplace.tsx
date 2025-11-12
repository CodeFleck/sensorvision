import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';
import { PluginRegistry, PluginCategory, InstalledPlugin } from '../types';
import toast from 'react-hot-toast';

const PluginMarketplace: React.FC = () => {
  const [plugins, setPlugins] = useState<PluginRegistry[]>([]);
  const [installedPlugins, setInstalledPlugins] = useState<InstalledPlugin[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<PluginCategory | ''>('');
  const [selectedPlugin, setSelectedPlugin] = useState<PluginRegistry | null>(null);
  const [showInstallModal, setShowInstallModal] = useState(false);
  const [installing, setInstalling] = useState(false);

  const categories: { value: PluginCategory | ''; label: string }[] = [
    { value: '', label: 'All Categories' },
    { value: 'PROTOCOL_PARSER', label: 'Protocol Parsers' },
    { value: 'INTEGRATION', label: 'Integrations' },
    { value: 'NOTIFICATION', label: 'Notifications' },
    { value: 'WIDGET', label: 'Widgets' },
    { value: 'ML_MODEL', label: 'ML Models' },
    { value: 'BUSINESS_LOGIC', label: 'Business Logic' },
  ];

  useEffect(() => {
    fetchData();
  }, [searchTerm, selectedCategory]);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [pluginsData, installedData] = await Promise.all([
        apiService.getAllPlugins({
          search: searchTerm || undefined,
          category: selectedCategory || undefined,
        }),
        apiService.getInstalledPlugins(),
      ]);

      // Mark plugins as installed if they exist in installedPlugins
      const pluginsWithInstallStatus = pluginsData.map(plugin => ({
        ...plugin,
        isInstalled: installedData.some(ip => ip.pluginKey === plugin.pluginKey),
        installedPluginId: installedData.find(ip => ip.pluginKey === plugin.pluginKey)?.id,
      }));

      setPlugins(pluginsWithInstallStatus);
      setInstalledPlugins(installedData);
    } catch (error) {
      console.error('Failed to fetch plugins:', error);
      toast.error('Failed to load plugins');
    } finally {
      setLoading(false);
    }
  };

  const handleInstall = async (plugin: PluginRegistry) => {
    setSelectedPlugin(plugin);
    setShowInstallModal(true);
  };

  const confirmInstall = async () => {
    if (!selectedPlugin) return;

    try {
      setInstalling(true);
      await apiService.installPlugin(selectedPlugin.pluginKey);
      toast.success(`${selectedPlugin.name} installed successfully`);
      setShowInstallModal(false);
      setSelectedPlugin(null);
      await fetchData();
    } catch (error: any) {
      console.error('Failed to install plugin:', error);
      toast.error(error.message || 'Failed to install plugin');
    } finally {
      setInstalling(false);
    }
  };

  const handleActivate = async (installedPluginId: number) => {
    try {
      await apiService.activatePlugin(installedPluginId);
      toast.success('Plugin activated successfully');
      await fetchData();
    } catch (error: any) {
      console.error('Failed to activate plugin:', error);
      toast.error(error.message || 'Failed to activate plugin');
    }
  };

  const handleDeactivate = async (installedPluginId: number) => {
    try {
      await apiService.deactivatePlugin(installedPluginId);
      toast.success('Plugin deactivated successfully');
      await fetchData();
    } catch (error: any) {
      console.error('Failed to deactivate plugin:', error);
      toast.error(error.message || 'Failed to deactivate plugin');
    }
  };

  const handleUninstall = async (installedPluginId: number, pluginName: string) => {
    if (!confirm(`Are you sure you want to uninstall ${pluginName}?`)) return;

    try {
      await apiService.uninstallPlugin(installedPluginId);
      toast.success(`${pluginName} uninstalled successfully`);
      await fetchData();
    } catch (error: any) {
      console.error('Failed to uninstall plugin:', error);
      toast.error(error.message || 'Failed to uninstall plugin');
    }
  };

  const getCategoryBadgeColor = (category: PluginCategory): string => {
    const colors: Record<PluginCategory, string> = {
      PROTOCOL_PARSER: 'bg-blue-100 text-blue-800',
      INTEGRATION: 'bg-green-100 text-green-800',
      NOTIFICATION: 'bg-purple-100 text-purple-800',
      WIDGET: 'bg-yellow-100 text-yellow-800',
      ML_MODEL: 'bg-pink-100 text-pink-800',
      BUSINESS_LOGIC: 'bg-gray-100 text-gray-800',
    };
    return colors[category] || 'bg-gray-100 text-gray-800';
  };

  const getInstalledPluginStatus = (pluginKey: string) => {
    return installedPlugins.find(ip => ip.pluginKey === pluginKey);
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Plugin Marketplace</h1>
        <p className="text-gray-600">Extend SensorVision with protocol parsers, integrations, and more</p>
      </div>

      {/* Search and Filters */}
      <div className="bg-white rounded-lg shadow-sm p-6 mb-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Search Plugins
            </label>
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search by name or description..."
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Category
            </label>
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value as PluginCategory | '')}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              {categories.map(cat => (
                <option key={cat.value} value={cat.value}>{cat.label}</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Plugin Grid */}
      {loading ? (
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          <p className="mt-4 text-gray-600">Loading plugins...</p>
        </div>
      ) : plugins.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-lg shadow-sm">
          <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
          </svg>
          <h3 className="mt-4 text-lg font-medium text-gray-900">No plugins found</h3>
          <p className="mt-2 text-gray-600">Try adjusting your search or filter criteria</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {plugins.map(plugin => {
            const installedStatus = getInstalledPluginStatus(plugin.pluginKey);
            const isActive = installedStatus?.status === 'ACTIVE';

            return (
              <div key={plugin.id} className="bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow">
                <div className="p-6">
                  {/* Header */}
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <h3 className="text-lg font-semibold text-gray-900">{plugin.name}</h3>
                        {plugin.isOfficial && (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 text-blue-800">
                            Official
                          </span>
                        )}
                        {plugin.isVerified && (
                          <svg className="h-4 w-4 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                            <path fillRule="evenodd" d="M6.267 3.455a3.066 3.066 0 001.745-.723 3.066 3.066 0 013.976 0 3.066 3.066 0 001.745.723 3.066 3.066 0 012.812 2.812c.051.643.304 1.254.723 1.745a3.066 3.066 0 010 3.976 3.066 3.066 0 00-.723 1.745 3.066 3.066 0 01-2.812 2.812 3.066 3.066 0 00-1.745.723 3.066 3.066 0 01-3.976 0 3.066 3.066 0 00-1.745-.723 3.066 3.066 0 01-2.812-2.812 3.066 3.066 0 00-.723-1.745 3.066 3.066 0 010-3.976 3.066 3.066 0 00.723-1.745 3.066 3.066 0 012.812-2.812zm7.44 5.252a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                          </svg>
                        )}
                      </div>
                      <span className={`inline-block px-2 py-1 rounded text-xs font-medium ${getCategoryBadgeColor(plugin.category)}`}>
                        {plugin.category.replace('_', ' ')}
                      </span>
                    </div>
                    <span className="text-sm text-gray-500">v{plugin.version}</span>
                  </div>

                  {/* Description */}
                  <p className="text-gray-600 text-sm mb-4 line-clamp-3">
                    {plugin.description || 'No description available'}
                  </p>

                  {/* Stats */}
                  <div className="flex items-center gap-4 text-sm text-gray-500 mb-4">
                    <div className="flex items-center gap-1">
                      <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                        <path d="M9 2a1 1 0 000 2h2a1 1 0 100-2H9z" />
                        <path fillRule="evenodd" d="M4 5a2 2 0 012-2 3 3 0 003 3h2a3 3 0 003-3 2 2 0 012 2v11a2 2 0 01-2 2H6a2 2 0 01-2-2V5zm9.707 5.707a1 1 0 00-1.414-1.414L9 12.586l-1.293-1.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                      </svg>
                      <span>{plugin.installationCount} installs</span>
                    </div>
                    {plugin.ratingAverage && (
                      <div className="flex items-center gap-1">
                        <svg className="h-4 w-4 text-yellow-400" fill="currentColor" viewBox="0 0 20 20">
                          <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                        </svg>
                        <span>{plugin.ratingAverage.toFixed(1)} ({plugin.ratingCount})</span>
                      </div>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex gap-2">
                    {!plugin.isInstalled ? (
                      <button
                        onClick={() => handleInstall(plugin)}
                        className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm font-medium"
                      >
                        Install
                      </button>
                    ) : (
                      <>
                        {isActive ? (
                          <button
                            onClick={() => handleDeactivate(plugin.installedPluginId!)}
                            className="flex-1 px-4 py-2 bg-yellow-600 text-white rounded-lg hover:bg-yellow-700 transition-colors text-sm font-medium"
                          >
                            Deactivate
                          </button>
                        ) : (
                          <button
                            onClick={() => handleActivate(plugin.installedPluginId!)}
                            className="flex-1 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors text-sm font-medium"
                          >
                            Activate
                          </button>
                        )}
                        <button
                          onClick={() => handleUninstall(plugin.installedPluginId!, plugin.name)}
                          className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors text-sm font-medium"
                        >
                          Uninstall
                        </button>
                      </>
                    )}
                  </div>

                  {/* Author */}
                  {plugin.author && (
                    <div className="mt-4 pt-4 border-t border-gray-100 text-xs text-gray-500">
                      by {plugin.author}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Install Confirmation Modal */}
      {showInstallModal && selectedPlugin && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
            <div className="p-6">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">
                Install {selectedPlugin.name}?
              </h3>
              <p className="text-gray-600 mb-6">
                This will install version {selectedPlugin.version} of {selectedPlugin.name}.
                You can configure and activate it after installation.
              </p>
              <div className="flex gap-3">
                <button
                  onClick={() => {
                    setShowInstallModal(false);
                    setSelectedPlugin(null);
                  }}
                  disabled={installing}
                  className="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50"
                >
                  Cancel
                </button>
                <button
                  onClick={confirmInstall}
                  disabled={installing}
                  className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
                >
                  {installing ? 'Installing...' : 'Install'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PluginMarketplace;
