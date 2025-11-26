import { useEffect, useState } from 'react';
import { Plus, Edit, Trash2, Search, Key } from 'lucide-react';
import { Device } from '../types';
import { apiService } from '../services/api';
import { DeviceModal } from '../components/DeviceModal';
import { TokenModal } from '../components/TokenModal';
import { clsx } from 'clsx';
import { formatTimeAgo } from '../utils/timeUtils';

export const Devices = () => {
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isTokenModalOpen, setIsTokenModalOpen] = useState(false);
  const [selectedDevice, setSelectedDevice] = useState<Device | null>(null);

  useEffect(() => {
    fetchDevices();
  }, []);

  const fetchDevices = async () => {
    try {
      const data = await apiService.getDevices();
      setDevices(data);
    } catch (error) {
      console.error('Failed to fetch devices:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setSelectedDevice(null);
    setIsModalOpen(true);
  };

  const handleEdit = (device: Device) => {
    setSelectedDevice(device);
    setIsModalOpen(true);
  };

  const handleManageToken = (device: Device) => {
    setSelectedDevice(device);
    setIsTokenModalOpen(true);
  };

  const handleToggleActive = async (device: Device) => {
    try {
      // Use nullish coalescing to handle undefined/null values
      // If device.active is null/undefined, treat as true (default)
      const currentActive = device.active ?? true;
      const nextActive = !currentActive;

      await apiService.updateDevice(device.externalId, {
        name: device.name,
        description: device.description,
        active: nextActive,
        location: device.location,
        sensorType: device.sensorType,
        firmwareVersion: device.firmwareVersion,
      });
      await fetchDevices();
    } catch (error) {
      console.error('Failed to toggle device active status:', error);
    }
  };

  const handleDelete = async (externalId: string) => {
    if (window.confirm('Are you sure you want to delete this device?')) {
      try {
        await apiService.deleteDevice(externalId);
        await fetchDevices();
      } catch (error) {
        console.error('Failed to delete device:', error);
      }
    }
  };

  const handleModalClose = () => {
    setIsModalOpen(false);
    setSelectedDevice(null);
    fetchDevices();
  };

  const filteredDevices = devices.filter(device =>
    device.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    device.externalId.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (device.location && device.location.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  const statusColors = {
    ONLINE: 'bg-green-100 text-green-800',
    OFFLINE: 'bg-red-100 text-red-800',
    UNKNOWN: 'bg-gray-100 text-gray-800',
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading devices...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Device Management</h1>
          <p className="text-gray-600 mt-1">Manage your IoT devices and monitor their status</p>
        </div>
        <button
          onClick={handleCreate}
          className="flex items-center space-x-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus className="h-4 w-4" />
          <span>Add Device</span>
        </button>
      </div>

      {/* Search */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
        <input
          type="text"
          placeholder="Search devices..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        />
      </div>

      {/* Device Table */}
      <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
        <table className="w-full">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Device
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Active
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Location
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Type
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Last Seen
              </th>
              <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {filteredDevices.map((device) => (
              <tr key={device.externalId} className="hover:bg-gray-50">
                <td className="px-6 py-4 whitespace-nowrap">
                  <div>
                    <div className="text-sm font-medium text-gray-900">{device.name}</div>
                    <div className="text-sm text-gray-500">{device.externalId}</div>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span
                    className={clsx(
                      'px-2 py-1 text-xs font-medium rounded-full',
                      statusColors[device.status]
                    )}
                  >
                    {device.status}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={device.active !== false}
                      onChange={() => handleToggleActive(device)}
                      className="sr-only peer"
                      aria-label="Toggle Active Status"
                    />
                    <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  </label>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {device.location || '-'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {device.sensorType || '-'}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  <span title={device.lastSeenAt ? new Date(device.lastSeenAt).toLocaleString() : undefined}>
                    {device.lastSeenAt ? formatTimeAgo(device.lastSeenAt) : '-'}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <div className="flex items-center justify-end space-x-2">
                    <button
                      onClick={() => handleManageToken(device)}
                      className="text-green-600 hover:text-green-900 p-1"
                      title="Manage API Token"
                      aria-label="Manage Token"
                    >
                      <Key className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => handleEdit(device)}
                      className="text-blue-600 hover:text-blue-900 p-1"
                      title="Edit Device"
                      aria-label="Edit"
                    >
                      <Edit className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => handleDelete(device.externalId)}
                      className="text-red-600 hover:text-red-900 p-1"
                      title="Delete Device"
                      aria-label="Delete"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {filteredDevices.length === 0 && (
          <div className="text-center py-8 text-gray-500">
            No devices found
          </div>
        )}
      </div>

      {/* Device Modal */}
      {isModalOpen && (
        <DeviceModal
          device={selectedDevice}
          onClose={handleModalClose}
        />
      )}

      {/* Token Management Modal */}
      {isTokenModalOpen && selectedDevice && (
        <TokenModal
          device={selectedDevice}
          isOpen={isTokenModalOpen}
          onClose={() => {
            setIsTokenModalOpen(false);
            setSelectedDevice(null);
          }}
        />
      )}
    </div>
  );
};