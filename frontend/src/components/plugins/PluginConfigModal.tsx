import React, { useState, useEffect } from 'react';
import { X, Save, Loader, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';
import pluginMarketplaceService from '../../services/pluginMarketplaceService';

interface PluginConfigModalProps {
  open: boolean;
  pluginKey: string;
  onClose: () => void;
}

const PluginConfigModal: React.FC<PluginConfigModalProps> = ({ open, pluginKey, onClose }) => {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [configuration, setConfiguration] = useState<any>({});
  const [configSchema, setConfigSchema] = useState<any>(null);
  const [pluginInfo, setPluginInfo] = useState<any>(null);

  useEffect(() => {
    if (open && pluginKey) {
      loadConfiguration();
    }
  }, [open, pluginKey]);

  const loadConfiguration = async () => {
    try {
      setLoading(true);
      const [plugin, defaultConfig] = await Promise.all([
        pluginMarketplaceService.getPlugin(pluginKey),
        pluginMarketplaceService.getDefaultConfiguration(pluginKey),
      ]);

      setPluginInfo(plugin);
      setConfigSchema(plugin.configSchema);

      // Try to get existing configuration from installed plugins
      try {
        const installed = await pluginMarketplaceService.getInstalledPlugins();
        const installedPlugin = installed.find((p) => p.pluginKey === pluginKey);
        if (installedPlugin && installedPlugin.configuration) {
          setConfiguration(installedPlugin.configuration);
        } else {
          setConfiguration(defaultConfig || {});
        }
      } catch {
        setConfiguration(defaultConfig || {});
      }
    } catch (error) {
      console.error('Failed to load plugin configuration:', error);
      toast.error('Failed to load plugin configuration');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      await pluginMarketplaceService.updateConfiguration(pluginKey, configuration);
      toast.success('Configuration saved successfully');
      onClose();
    } catch (error) {
      console.error('Failed to save configuration:', error);
      toast.error('Failed to save configuration');
    } finally {
      setSaving(false);
    }
  };

  const handleConfigChange = (key: string, value: any) => {
    setConfiguration((prev: any) => ({
      ...prev,
      [key]: value,
    }));
  };

  const renderConfigField = (key: string, schema: any) => {
    const value = configuration[key] || '';
    const fieldType = schema.type || 'string';
    const label = schema.title || key;
    const description = schema.description;
    const required = schema.required || false;

    switch (fieldType) {
      case 'string':
        if (schema.enum) {
          // Select dropdown for enums
          return (
            <div key={key} className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {label}
                {required && <span className="text-red-500 ml-1">*</span>}
              </label>
              {description && <p className="text-xs text-gray-500 mb-2">{description}</p>}
              <select
                value={value}
                onChange={(e) => handleConfigChange(key, e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                required={required}
              >
                <option value="">Select...</option>
                {schema.enum.map((option: string) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </select>
            </div>
          );
        } else if (schema.format === 'textarea') {
          // Text area
          return (
            <div key={key} className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {label}
                {required && <span className="text-red-500 ml-1">*</span>}
              </label>
              {description && <p className="text-xs text-gray-500 mb-2">{description}</p>}
              <textarea
                value={value}
                onChange={(e) => handleConfigChange(key, e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                rows={4}
                required={required}
              />
            </div>
          );
        } else {
          // Text input
          return (
            <div key={key} className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {label}
                {required && <span className="text-red-500 ml-1">*</span>}
              </label>
              {description && <p className="text-xs text-gray-500 mb-2">{description}</p>}
              <input
                type={schema.format === 'password' ? 'password' : 'text'}
                value={value}
                onChange={(e) => handleConfigChange(key, e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                placeholder={schema.placeholder}
                required={required}
              />
            </div>
          );
        }

      case 'number':
      case 'integer':
        return (
          <div key={key} className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {label}
              {required && <span className="text-red-500 ml-1">*</span>}
            </label>
            {description && <p className="text-xs text-gray-500 mb-2">{description}</p>}
            <input
              type="number"
              value={value}
              onChange={(e) => handleConfigChange(key, parseFloat(e.target.value))}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              min={schema.minimum}
              max={schema.maximum}
              step={fieldType === 'integer' ? 1 : 0.01}
              required={required}
            />
          </div>
        );

      case 'boolean':
        return (
          <div key={key} className="mb-4">
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={value === true}
                onChange={(e) => handleConfigChange(key, e.target.checked)}
                className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
              />
              <span className="text-sm font-medium text-gray-700">{label}</span>
            </label>
            {description && <p className="text-xs text-gray-500 mt-1 ml-6">{description}</p>}
          </div>
        );

      case 'object':
        // Render nested object fields
        return (
          <div key={key} className="mb-4 p-4 border border-gray-200 rounded-lg">
            <h4 className="text-sm font-semibold text-gray-900 mb-3">
              {label}
              {required && <span className="text-red-500 ml-1">*</span>}
            </h4>
            {description && <p className="text-xs text-gray-500 mb-3">{description}</p>}
            {schema.properties &&
              Object.keys(schema.properties).map((nestedKey) =>
                renderConfigField(`${key}.${nestedKey}`, schema.properties[nestedKey])
              )}
          </div>
        );

      default:
        // JSON input for complex types
        return (
          <div key={key} className="mb-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">
              {label}
              {required && <span className="text-red-500 ml-1">*</span>}
            </label>
            {description && <p className="text-xs text-gray-500 mb-2">{description}</p>}
            <textarea
              value={typeof value === 'string' ? value : JSON.stringify(value, null, 2)}
              onChange={(e) => {
                try {
                  const parsed = JSON.parse(e.target.value);
                  handleConfigChange(key, parsed);
                } catch {
                  handleConfigChange(key, e.target.value);
                }
              }}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 font-mono text-sm"
              rows={6}
              required={required}
            />
            <p className="text-xs text-gray-500 mt-1">JSON format</p>
          </div>
        );
    }
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex items-center justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:block sm:p-0">
        {/* Background overlay */}
        <div
          className="fixed inset-0 transition-opacity bg-gray-500 bg-opacity-75"
          onClick={onClose}
        />

        {/* Modal panel */}
        <div className="inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-2xl sm:w-full">
          {/* Header */}
          <div className="bg-gray-50 px-6 py-4 border-b border-gray-200">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-xl font-semibold text-gray-900">Plugin Configuration</h2>
                {pluginInfo && (
                  <p className="text-sm text-gray-600 mt-1">{pluginInfo.name}</p>
                )}
              </div>
              <button
                onClick={onClose}
                className="text-gray-400 hover:text-gray-600 transition-colors"
              >
                <X className="w-6 h-6" />
              </button>
            </div>
          </div>

          {/* Body */}
          <div className="px-6 py-6 max-h-[60vh] overflow-y-auto">
            {loading ? (
              <div className="flex items-center justify-center py-8">
                <Loader className="w-8 h-8 text-blue-600 animate-spin" />
              </div>
            ) : configSchema && configSchema.properties ? (
              <form onSubmit={(e) => e.preventDefault()}>
                {Object.keys(configSchema.properties).map((key) =>
                  renderConfigField(key, configSchema.properties[key])
                )}
              </form>
            ) : (
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                <div className="flex items-start gap-3">
                  <AlertCircle className="w-5 h-5 text-yellow-600 flex-shrink-0 mt-0.5" />
                  <div>
                    <p className="text-yellow-800 font-medium">No Configuration Schema</p>
                    <p className="text-yellow-700 text-sm mt-1">
                      This plugin does not require configuration or has no schema defined.
                    </p>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="bg-gray-50 px-6 py-4 flex items-center justify-end gap-3 border-t border-gray-200">
            <button
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition-colors"
              disabled={saving}
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              disabled={saving || loading}
              className="flex items-center gap-2 px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {saving ? (
                <>
                  <Loader className="w-4 h-4 animate-spin" />
                  Saving...
                </>
              ) : (
                <>
                  <Save className="w-4 h-4" />
                  Save Configuration
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PluginConfigModal;
