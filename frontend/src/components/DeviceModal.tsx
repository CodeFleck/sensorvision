import { useState } from 'react';
import { X } from 'lucide-react';
import { Device } from '../types';
import { apiService } from '../services/api';

interface DeviceModalProps {
  device: Device | null;
  onClose: () => void;
}

export const DeviceModal = ({ device, onClose }: DeviceModalProps) => {
  const [formData, setFormData] = useState({
    externalId: device?.externalId || '',
    name: device?.name || '',
    location: device?.location || '',
    sensorType: device?.sensorType || '',
    firmwareVersion: device?.firmwareVersion || '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [deviceIdError, setDeviceIdError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Prevent submission if device ID has validation errors
    if (deviceIdError) {
      return;
    }

    setLoading(true);
    setError(null);

    try {
      if (device) {
        // Update existing device
        await apiService.updateDevice(device.externalId, formData);
      } else {
        // Create new device
        await apiService.createDevice(formData);
      }
      onClose();
    } catch (error) {
      setError(error instanceof Error ? error.message : 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;

    // Validate device ID for spaces
    if (name === 'externalId') {
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
            <label htmlFor="externalId" className="block text-sm font-medium text-gray-700 mb-1">
              Device ID *
            </label>
            <input
              type="text"
              id="externalId"
              name="externalId"
              required
              disabled={!!device} // Can't change ID for existing devices
              value={formData.externalId}
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
              disabled={loading}
              className="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {loading ? 'Saving...' : device ? 'Update' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};