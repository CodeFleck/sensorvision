import { useState, useEffect } from 'react';
import { X, Zap } from 'lucide-react';
import { Device, DeviceTypeSimple } from '../types';
import { apiService } from '../services/api';

interface DeviceModalProps {
  device: Device | null;
  onClose: () => void;
}

export const DeviceModal = ({ device, onClose }: DeviceModalProps) => {
  const [formData, setFormData] = useState({
    deviceId: device?.externalId || '',
    name: device?.name || '',
    description: device?.description || '',
    location: device?.location || '',
    sensorType: device?.sensorType || '',
    firmwareVersion: device?.firmwareVersion || '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [deviceIdError, setDeviceIdError] = useState<string | null>(null);

  // Template selection state (only for new devices)
  const [deviceTypes, setDeviceTypes] = useState<DeviceTypeSimple[]>([]);
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null);
  const [loadingTemplates, setLoadingTemplates] = useState(false);
  const [applyingTemplate, setApplyingTemplate] = useState(false);
  const [templateResult, setTemplateResult] = useState<string | null>(null);

  // Fetch device types on mount (only for new devices)
  useEffect(() => {
    if (!device) {
      loadDeviceTypes();
    }
  }, [device]);

  const loadDeviceTypes = async () => {
    setLoadingTemplates(true);
    try {
      const types = await apiService.getDeviceTypes();
      setDeviceTypes(types);
    } catch (err) {
      console.error('Failed to load device types:', err);
    } finally {
      setLoadingTemplates(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Prevent submission if device ID has validation errors
    if (deviceIdError) {
      return;
    }

    setLoading(true);
    setError(null);
    setTemplateResult(null);

    try {
      // Map deviceId back to externalId for API
      const apiData = {
        externalId: formData.deviceId,
        name: formData.name,
        description: formData.description,
        location: formData.location,
        sensorType: formData.sensorType,
        firmwareVersion: formData.firmwareVersion,
      };

      if (device) {
        // Update existing device
        await apiService.updateDevice(device.externalId, apiData);
        onClose();
      } else {
        // Create new device
        await apiService.createDevice(apiData);

        // Apply template if selected
        if (selectedTemplateId) {
          setApplyingTemplate(true);
          try {
            const result = await apiService.applyDeviceTypeTemplate(selectedTemplateId, formData.deviceId);
            setTemplateResult(
              `Template applied: ${result.variablesCreated} variables, ${result.rulesCreated} rules${result.dashboardCreated ? ', dashboard created' : ''}`
            );
            // Brief delay to show success message before closing
            setTimeout(() => onClose(), 1500);
          } catch (templateError) {
            // Device was created but template failed - show warning but don't fail
            setTemplateResult(
              `Device created, but template application failed: ${templateError instanceof Error ? templateError.message : 'Unknown error'}`
            );
            setTimeout(() => onClose(), 2000);
          } finally {
            setApplyingTemplate(false);
          }
        } else {
          onClose();
        }
      }
    } catch (error) {
      setError(error instanceof Error ? error.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;

    // Validate device ID for spaces
    if (name === 'deviceId') {
      if (/\s/.test(value)) {
        setDeviceIdError('Device ID cannot contain spaces. Use hyphens or underscores instead.');
      } else {
        setDeviceIdError(null);
      }
    }

    setFormData(prev => ({
      ...prev,
      [name]: value,
    }));
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg max-w-md w-full p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold text-gray-900">
            {device ? 'Edit Device' : 'Add New Device'}
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-100 border border-red-300 text-red-700 rounded">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="deviceId" className="block text-sm font-medium text-gray-700 mb-1">
              Device ID *
            </label>
            <input
              type="text"
              id="deviceId"
              name="deviceId"
              required
              disabled={!!device} // Can't change ID for existing devices
              value={formData.deviceId}
              onChange={handleChange}
              className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 ${
                deviceIdError ? 'border-red-500' : 'border-gray-300'
              }`}
              placeholder="e.g., meter-001"
            />
            {deviceIdError && (
              <p className="mt-1 text-sm text-red-600">{deviceIdError}</p>
            )}
          </div>

          <div>
            <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
              Device Name *
            </label>
            <input
              type="text"
              id="name"
              name="name"
              required
              value={formData.name}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="e.g., Smart Meter 1"
            />
          </div>

          <div>
            <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <textarea
              id="description"
              name="description"
              value={formData.description}
              onChange={handleChange}
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="e.g., Production floor monitoring device"
            />
          </div>

          <div>
            <label htmlFor="location" className="block text-sm font-medium text-gray-700 mb-1">
              Location
            </label>
            <input
              type="text"
              id="location"
              name="location"
              value={formData.location}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="e.g., Building A - Floor 1"
            />
          </div>

          <div>
            <label htmlFor="sensorType" className="block text-sm font-medium text-gray-700 mb-1">
              Sensor Type
            </label>
            <input
              type="text"
              id="sensorType"
              name="sensorType"
              value={formData.sensorType}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="e.g., smart_meter"
            />
          </div>

          <div>
            <label htmlFor="firmwareVersion" className="block text-sm font-medium text-gray-700 mb-1">
              Firmware Version
            </label>
            <input
              type="text"
              id="firmwareVersion"
              name="firmwareVersion"
              value={formData.firmwareVersion}
              onChange={handleChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="e.g., 2.1.0"
            />
          </div>

          {/* Template Selection (only for new devices) */}
          {!device && (
            <div className="border-t border-gray-200 pt-4 mt-2">
              <div className="flex items-center gap-2 mb-3">
                <Zap className="h-4 w-4 text-blue-600" />
                <label className="block text-sm font-medium text-gray-700">
                  Apply Device Type Template (Optional)
                </label>
              </div>
              {loadingTemplates ? (
                <div className="text-sm text-gray-500">Loading templates...</div>
              ) : deviceTypes.length === 0 ? (
                <div className="text-sm text-gray-500">No templates available</div>
              ) : (
                <select
                  value={selectedTemplateId ?? ''}
                  onChange={(e) => setSelectedTemplateId(e.target.value ? Number(e.target.value) : null)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                >
                  <option value="">No template - configure manually</option>
                  {deviceTypes.map((type) => (
                    <option key={type.id} value={type.id}>
                      {type.name} ({type.variableCount} variables)
                      {type.isSystemTemplate ? ' ⭐' : ''}
                    </option>
                  ))}
                </select>
              )}
              {selectedTemplateId && (
                <p className="mt-2 text-xs text-gray-500">
                  Auto-creates variables, alert rules, and dashboard based on the selected template.
                </p>
              )}
            </div>
          )}

          {/* Template application status */}
          {templateResult && (
            <div className={`p-3 rounded-lg text-sm ${
              templateResult.includes('failed')
                ? 'bg-yellow-100 border border-yellow-300 text-yellow-700'
                : 'bg-green-100 border border-green-300 text-green-700'
            }`}>
              {templateResult}
            </div>
          )}

          <div className="flex space-x-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={loading || applyingTemplate}
              className="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {applyingTemplate
                ? 'Applying Template...'
                : loading
                  ? 'Saving...'
                  : device
                    ? 'Update'
                    : selectedTemplateId
                      ? 'Create & Apply Template'
                      : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};