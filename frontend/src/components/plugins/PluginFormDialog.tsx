import React, { useState, useEffect } from 'react';
import { X } from 'lucide-react';
import toast from 'react-hot-toast';
import dataPluginsService, {
  DataPlugin,
  CreatePluginRequest,
  PluginType,
  PluginProvider,
} from '../../services/dataPluginsService';

interface PluginFormDialogProps {
  open: boolean;
  plugin: DataPlugin | null;
  onClose: (saved: boolean) => void;
}

const PluginFormDialog: React.FC<PluginFormDialogProps> = ({
  open,
  plugin,
  onClose,
}) => {
  const [formData, setFormData] = useState<CreatePluginRequest>({
    name: '',
    description: '',
    pluginType: PluginType.WEBHOOK,
    provider: PluginProvider.HTTP_WEBHOOK,
    enabled: true,
    configuration: {},
  });
  const [configJson, setConfigJson] = useState('{}');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (plugin) {
      setFormData({
        name: plugin.name,
        description: plugin.description || '',
        pluginType: plugin.pluginType,
        provider: plugin.provider,
        enabled: plugin.enabled,
        configuration: plugin.configuration,
      });
      setConfigJson(JSON.stringify(plugin.configuration, null, 2));
    } else {
      setFormData({
        name: '',
        description: '',
        pluginType: PluginType.WEBHOOK,
        provider: PluginProvider.HTTP_WEBHOOK,
        enabled: true,
        configuration: {},
      });
      setConfigJson('{}');
    }
  }, [plugin, open]);

  const handleProviderChange = (provider: PluginProvider) => {
    setFormData((prev) => ({ ...prev, provider }));

    // Set default configuration based on provider
    const defaultConfigs: Record<PluginProvider, any> = {
      [PluginProvider.HTTP_WEBHOOK]: {
        deviceIdField: 'deviceId',
        timestampField: 'timestamp',
        variablesField: 'variables',
        metadataField: 'metadata',
      },
      [PluginProvider.LORAWAN_TTN]: {
        deviceIdPrefix: '',
        deviceIdSuffix: '',
      },
      [PluginProvider.CSV_FILE]: {
        deviceIdColumn: 'deviceId',
        timestampColumn: 'timestamp',
        timestampFormat: 'ISO',
        skipHeader: true,
        variableColumns: [],
      },
      [PluginProvider.MODBUS_TCP]: {},
      [PluginProvider.SIGFOX]: {},
      [PluginProvider.PARTICLE_CLOUD]: {},
      [PluginProvider.CUSTOM_PARSER]: {},
      [PluginProvider.MQTT_CUSTOM]: {},
    };

    setConfigJson(JSON.stringify(defaultConfigs[provider], null, 2));
  };

  const handleSave = async () => {
    try {
      // Parse configuration JSON
      let config;
      try {
        config = JSON.parse(configJson);
      } catch (e) {
        toast.error('Invalid JSON configuration');
        return;
      }

      const request: CreatePluginRequest = {
        ...formData,
        configuration: config,
      };

      setSaving(true);

      if (plugin) {
        await dataPluginsService.updatePlugin(plugin.id, request);
        toast.success('Plugin updated successfully');
      } else {
        await dataPluginsService.createPlugin(request);
        toast.success('Plugin created successfully');
      }

      onClose(true);
    } catch (error: any) {
      console.error('Failed to save plugin:', error);
      toast.error(error.response?.data?.message || 'Failed to save plugin');
    } finally {
      setSaving(false);
    }
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-xl max-w-3xl w-full max-h-[90vh] flex flex-col">
        <div className="bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center">
          <h2 className="text-xl font-bold text-gray-900">
            {plugin ? 'Edit Plugin' : 'Create New Plugin'}
          </h2>
          <button
            onClick={() => onClose(false)}
            className="p-1 hover:bg-gray-100 rounded"
          >
            <X className="w-6 h-6 text-gray-600" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          <div className="space-y-4">
            {/* Name */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Name *
              </label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) =>
                  setFormData({ ...formData, name: e.target.value })
                }
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                placeholder="my-plugin"
                required
              />
            </div>

            {/* Description */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <textarea
                value={formData.description}
                onChange={(e) =>
                  setFormData({ ...formData, description: e.target.value })
                }
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
                rows={2}
                placeholder="Optional description"
              />
            </div>

            {/* Provider */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Provider *
              </label>
              <select
                value={formData.provider}
                onChange={(e) =>
                  handleProviderChange(e.target.value as PluginProvider)
                }
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500"
              >
                <option value={PluginProvider.HTTP_WEBHOOK}>
                  HTTP Webhook
                </option>
                <option value={PluginProvider.LORAWAN_TTN}>
                  LoRaWAN (The Things Network)
                </option>
                <option value={PluginProvider.CSV_FILE}>
                  CSV File Import
                </option>
              </select>
            </div>

            {/* Plugin Type (auto-set based on provider) */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Plugin Type
              </label>
              <input
                type="text"
                value={formData.pluginType}
                className="w-full px-3 py-2 border border-gray-300 rounded-md bg-gray-50"
                disabled
              />
            </div>

            {/* Enabled */}
            <div className="flex items-center">
              <input
                type="checkbox"
                id="enabled"
                checked={formData.enabled}
                onChange={(e) =>
                  setFormData({ ...formData, enabled: e.target.checked })
                }
                className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              />
              <label
                htmlFor="enabled"
                className="ml-2 block text-sm text-gray-700"
              >
                Enable this plugin
              </label>
            </div>

            {/* Configuration JSON */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Configuration (JSON) *
              </label>
              <textarea
                value={configJson}
                onChange={(e) => setConfigJson(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 font-mono text-sm"
                rows={12}
                placeholder="{}"
              />
              <p className="text-xs text-gray-500 mt-1">
                Provider-specific configuration in JSON format
              </p>
            </div>
          </div>
        </div>

        <div className="border-t border-gray-200 px-6 py-4 flex justify-end gap-3 bg-gray-50">
          <button
            onClick={() => onClose(false)}
            className="px-4 py-2 text-gray-700 border border-gray-300 rounded-md hover:bg-gray-50"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            disabled={saving || !formData.name}
            className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {saving ? 'Saving...' : plugin ? 'Update' : 'Create'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default PluginFormDialog;
