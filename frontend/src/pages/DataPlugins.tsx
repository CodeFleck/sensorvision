import React, { useState, useEffect } from 'react';
import {
  Plus,
  Edit,
  Trash2,
  Power,
  PowerOff,
  History,
  Loader,
} from 'lucide-react';
import toast from 'react-hot-toast';
import dataPluginsService, {
  DataPlugin,
  PluginType,
  PluginProvider,
} from '../services/dataPluginsService';
import PluginFormDialog from '../components/plugins/PluginFormDialog';
import ExecutionHistoryDialog from '../components/plugins/ExecutionHistoryDialog';

const DataPlugins: React.FC = () => {
  const [plugins, setPlugins] = useState<DataPlugin[]>([]);
  const [loading, setLoading] = useState(true);
  const [formDialogOpen, setFormDialogOpen] = useState(false);
  const [historyDialogOpen, setHistoryDialogOpen] = useState(false);
  const [selectedPlugin, setSelectedPlugin] = useState<DataPlugin | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  useEffect(() => {
    loadPlugins();
  }, [page]);

  const loadPlugins = async () => {
    try {
      setLoading(true);
      const response = await dataPluginsService.getPlugins(page, 20);
      setPlugins(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (error) {
      console.error('Failed to load plugins:', error);
      toast.error('Failed to load plugins');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setSelectedPlugin(null);
    setFormDialogOpen(true);
  };

  const handleEdit = (plugin: DataPlugin) => {
    setSelectedPlugin(plugin);
    setFormDialogOpen(true);
  };

  const handleDelete = async (plugin: DataPlugin) => {
    if (!confirm(`Are you sure you want to delete the plugin "${plugin.name}"?`)) {
      return;
    }

    try {
      await dataPluginsService.deletePlugin(plugin.id);
      toast.success('Plugin deleted successfully');
      loadPlugins();
    } catch (error) {
      console.error('Failed to delete plugin:', error);
      toast.error('Failed to delete plugin');
    }
  };

  const handleToggleEnabled = async (plugin: DataPlugin) => {
    try {
      await dataPluginsService.updatePlugin(plugin.id, {
        name: plugin.name,
        description: plugin.description,
        pluginType: plugin.pluginType,
        provider: plugin.provider,
        enabled: !plugin.enabled,
        configuration: plugin.configuration,
      });
      toast.success(`Plugin ${!plugin.enabled ? 'enabled' : 'disabled'}`);
      loadPlugins();
    } catch (error) {
      console.error('Failed to toggle plugin:', error);
      toast.error('Failed to update plugin');
    }
  };

  const handleViewHistory = (plugin: DataPlugin) => {
    setSelectedPlugin(plugin);
    setHistoryDialogOpen(true);
  };

  const handleFormClose = (saved: boolean) => {
    setFormDialogOpen(false);
    setSelectedPlugin(null);
    if (saved) {
      loadPlugins();
    }
  };

  const getPluginTypeColor = (type: PluginType): string => {
    switch (type) {
      case PluginType.WEBHOOK:
        return 'bg-blue-100 text-blue-800';
      case PluginType.PROTOCOL_PARSER:
        return 'bg-green-100 text-green-800';
      case PluginType.INTEGRATION:
        return 'bg-purple-100 text-purple-800';
      case PluginType.CSV_IMPORT:
        return 'bg-orange-100 text-orange-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const formatPluginType = (type: PluginType): string => {
    return type.replace(/_/g, ' ');
  };

  const getProviderDisplayName = (provider: PluginProvider): string => {
    const names: Record<PluginProvider, string> = {
      [PluginProvider.LORAWAN_TTN]: 'LoRaWAN (TTN)',
      [PluginProvider.HTTP_WEBHOOK]: 'HTTP Webhook',
      [PluginProvider.CSV_FILE]: 'CSV Import',
      [PluginProvider.MODBUS_TCP]: 'Modbus TCP',
      [PluginProvider.SIGFOX]: 'Sigfox',
      [PluginProvider.PARTICLE_CLOUD]: 'Particle Cloud',
      [PluginProvider.CUSTOM_PARSER]: 'Custom Parser',
      [PluginProvider.MQTT_CUSTOM]: 'MQTT Custom',
    };
    return names[provider] || provider;
  };

  return (
    <div className="p-6">
      <div className="mb-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Data Plugins</h1>
            <p className="text-gray-600 mt-1">
              Configure data ingestion from various sources and formats
            </p>
          </div>
          <button
            onClick={handleCreate}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
          >
            <Plus className="w-4 h-4" />
            New Plugin
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center items-center py-12">
          <Loader className="w-8 h-8 text-blue-600 animate-spin" />
        </div>
      ) : plugins.length === 0 ? (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-8 text-center">
          <p className="text-blue-800 mb-4">
            No data plugins configured yet. Create your first plugin to start ingesting data from external sources.
          </p>
          <button
            onClick={handleCreate}
            className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
          >
            <Plus className="w-4 h-4" />
            Create Plugin
          </button>
        </div>
      ) : (
        <>
          <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Name
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Provider
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Type
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {plugins.map((plugin) => (
                  <tr key={plugin.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4">
                      <div>
                        <div className="text-sm font-medium text-gray-900">
                          {plugin.name}
                        </div>
                        {plugin.description && (
                          <div className="text-sm text-gray-500">
                            {plugin.description}
                          </div>
                        )}
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className="text-sm text-gray-900">
                        {getProviderDisplayName(plugin.provider)}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span
                        className={`inline-flex px-2 py-1 text-xs font-medium rounded ${getPluginTypeColor(
                          plugin.pluginType
                        )}`}
                      >
                        {formatPluginType(plugin.pluginType)}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span
                        className={`inline-flex items-center gap-1 px-2 py-1 text-xs font-medium rounded ${
                          plugin.enabled
                            ? 'bg-green-100 text-green-800'
                            : 'bg-gray-100 text-gray-800'
                        }`}
                      >
                        {plugin.enabled ? (
                          <Power className="w-3 h-3" />
                        ) : (
                          <PowerOff className="w-3 h-3" />
                        )}
                        {plugin.enabled ? 'Enabled' : 'Disabled'}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right space-x-2">
                      <button
                        onClick={() => handleViewHistory(plugin)}
                        className="inline-flex items-center p-1 text-gray-600 hover:text-blue-600"
                        title="View execution history"
                      >
                        <History className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleToggleEnabled(plugin)}
                        className={`inline-flex items-center p-1 ${
                          plugin.enabled
                            ? 'text-gray-600 hover:text-orange-600'
                            : 'text-gray-600 hover:text-green-600'
                        }`}
                        title={plugin.enabled ? 'Disable' : 'Enable'}
                      >
                        {plugin.enabled ? (
                          <PowerOff className="w-4 h-4" />
                        ) : (
                          <Power className="w-4 h-4" />
                        )}
                      </button>
                      <button
                        onClick={() => handleEdit(plugin)}
                        className="inline-flex items-center p-1 text-gray-600 hover:text-blue-600"
                        title="Edit"
                      >
                        <Edit className="w-4 h-4" />
                      </button>
                      <button
                        onClick={() => handleDelete(plugin)}
                        className="inline-flex items-center p-1 text-gray-600 hover:text-red-600"
                        title="Delete"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div className="mt-4 flex items-center justify-between">
              <span className="text-sm text-gray-700">
                Showing {page * 20 + 1} to {Math.min((page + 1) * 20, totalElements)} of {totalElements} plugins
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page === 0}
                  className="px-3 py-1 text-sm border border-gray-300 rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Previous
                </button>
                <button
                  onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                  disabled={page >= totalPages - 1}
                  className="px-3 py-1 text-sm border border-gray-300 rounded hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </>
      )}

      <PluginFormDialog
        open={formDialogOpen}
        plugin={selectedPlugin}
        onClose={handleFormClose}
      />

      {selectedPlugin && (
        <ExecutionHistoryDialog
          open={historyDialogOpen}
          plugin={selectedPlugin}
          onClose={() => setHistoryDialogOpen(false)}
        />
      )}
    </div>
  );
};

export default DataPlugins;
