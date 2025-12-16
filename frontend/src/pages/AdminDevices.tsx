import { useState, useEffect } from 'react';
import {
  Cpu,
  Search,
  Power,
  Trash2,
  Building2,
  Edit2,
  X,
  Check,
  LayoutGrid,
  List,
  MapPin,
  Activity,
  Clock,
  Key,
  Wifi,
  WifiOff,
  Heart,
  AlertTriangle,
} from 'lucide-react';
import { apiService, AdminDevice, AdminDeviceStats } from '../services/api';
import toast from 'react-hot-toast';

type ViewMode = 'list' | 'cards';

export const AdminDevices = () => {
  const [devices, setDevices] = useState<AdminDevice[]>([]);
  const [stats, setStats] = useState<AdminDeviceStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterOrganization, setFilterOrganization] = useState<string>('all');
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [viewMode, setViewMode] = useState<ViewMode>('list');
  const [editingDevice, setEditingDevice] = useState<AdminDevice | null>(null);
  const [editForm, setEditForm] = useState({
    name: '',
    description: '',
    location: '',
    sensorType: '',
    firmwareVersion: '',
  });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    fetchDevices();
    fetchStats();
  }, []);

  const fetchDevices = async () => {
    try {
      setLoading(true);
      const data = await apiService.getAllAdminDevices();
      setDevices(data);
    } catch (error) {
      console.error('Failed to fetch devices:', error);
      toast.error('Failed to load devices');
    } finally {
      setLoading(false);
    }
  };

  const fetchStats = async () => {
    try {
      const data = await apiService.getAdminDeviceStats();
      setStats(data);
    } catch (error) {
      console.error('Failed to fetch device stats:', error);
    }
  };

  const openEditModal = (device: AdminDevice) => {
    setEditingDevice(device);
    setEditForm({
      name: device.name || '',
      description: device.description || '',
      location: device.location || '',
      sensorType: device.sensorType || '',
      firmwareVersion: device.firmwareVersion || '',
    });
  };

  const closeEditModal = () => {
    setEditingDevice(null);
    setEditForm({
      name: '',
      description: '',
      location: '',
      sensorType: '',
      firmwareVersion: '',
    });
  };

  const handleSaveDevice = async () => {
    if (!editingDevice) return;

    try {
      setSaving(true);
      await apiService.updateAdminDevice(editingDevice.id, editForm);
      toast.success('Device updated successfully');
      await fetchDevices();
      closeEditModal();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to update device');
    } finally {
      setSaving(false);
    }
  };

  const handleToggleActive = async (deviceId: string, currentStatus: boolean) => {
    try {
      if (currentStatus) {
        await apiService.disableAdminDevice(deviceId);
        toast.success('Device disabled successfully');
      } else {
        await apiService.enableAdminDevice(deviceId);
        toast.success('Device enabled successfully');
      }
      await fetchDevices();
      await fetchStats();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to toggle device status');
    }
  };

  const handleDeleteDevice = async (deviceId: string, deviceName: string) => {
    if (!window.confirm(`Are you sure you want to move device "${deviceName}" to trash? You can restore it within 30 days.`)) {
      return;
    }

    try {
      const response = await apiService.softDeleteDevice(deviceId);
      const trashId = response.data.trashId;
      const daysRemaining = response.data.daysRemaining;

      // Show toast with undo button
      toast(
        (t) => (
          <div className="flex items-center gap-3">
            <span>Device moved to trash ({daysRemaining} days to restore)</span>
            <button
              onClick={async () => {
                try {
                  await apiService.restoreTrashItem(trashId);
                  toast.dismiss(t.id);
                  toast.success('Device restored successfully');
                  await fetchDevices();
                  await fetchStats();
                } catch (err) {
                  toast.error('Failed to restore device');
                }
              }}
              className="px-3 py-1 bg-blue-600 text-white rounded text-sm font-medium hover:bg-blue-700"
            >
              Undo
            </button>
          </div>
        ),
        { duration: 10000 }
      );
      await fetchDevices();
      await fetchStats();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to delete device');
    }
  };

  const filteredDevices = devices.filter((device) => {
    const matchesSearch =
      device.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      device.externalId.toLowerCase().includes(searchTerm.toLowerCase()) ||
      device.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      device.location?.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesOrganization =
      filterOrganization === 'all' || device.organizationName === filterOrganization;

    const matchesStatus =
      filterStatus === 'all' ||
      (filterStatus === 'active' && device.active) ||
      (filterStatus === 'inactive' && !device.active) ||
      (filterStatus === 'online' && device.status === 'ONLINE') ||
      (filterStatus === 'offline' && device.status === 'OFFLINE');

    return matchesSearch && matchesOrganization && matchesStatus;
  });

  const organizations = Array.from(new Set(devices.map((d) => d.organizationName).filter(Boolean))).sort();

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ONLINE':
        return 'bg-green-100 text-green-800';
      case 'OFFLINE':
        return 'bg-red-100 text-red-800';
      case 'IDLE':
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getHealthColor = (score: number | undefined) => {
    if (!score) return 'text-gray-400';
    if (score >= 80) return 'text-green-500';
    if (score >= 50) return 'text-yellow-500';
    return 'text-red-500';
  };

  const formatDate = (dateStr: string | undefined) => {
    if (!dateStr) return 'Never';
    return new Date(dateStr).toLocaleString();
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
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Device Management</h1>
          <p className="text-gray-600 mt-1">Manage all devices across organizations</p>
        </div>
        <div className="flex items-center space-x-4">
          {stats && (
            <div className="flex items-center space-x-2 text-sm text-gray-600">
              <span>
                Total: <span className="font-semibold">{stats.totalDevices}</span>
              </span>
              <span>•</span>
              <span>
                Active: <span className="font-semibold text-green-600">{stats.activeDevices}</span>
              </span>
              <span>•</span>
              <span>
                Online: <span className="font-semibold text-blue-600">{stats.onlineDevices}</span>
              </span>
            </div>
          )}
          {/* View Toggle */}
          <div className="flex items-center bg-gray-100 rounded-lg p-1">
            <button
              onClick={() => setViewMode('list')}
              className={`p-2 rounded-md transition-colors ${
                viewMode === 'list'
                  ? 'bg-white text-blue-600 shadow-sm'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
              title="List view"
            >
              <List className="h-5 w-5" />
            </button>
            <button
              onClick={() => setViewMode('cards')}
              className={`p-2 rounded-md transition-colors ${
                viewMode === 'cards'
                  ? 'bg-white text-blue-600 shadow-sm'
                  : 'text-gray-500 hover:text-gray-700'
              }`}
              title="Card view"
            >
              <LayoutGrid className="h-5 w-5" />
            </button>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg shadow p-4">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search devices by name, ID, or location..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          <select
            value={filterOrganization}
            onChange={(e) => setFilterOrganization(e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="all">All Organizations</option>
            {organizations.map((org) => (
              <option key={org} value={org}>
                {org}
              </option>
            ))}
          </select>

          <select
            value={filterStatus}
            onChange={(e) => setFilterStatus(e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="all">All Status</option>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
            <option value="online">Online</option>
            <option value="offline">Offline</option>
          </select>
        </div>
      </div>

      {/* Devices View */}
      {viewMode === 'list' ? (
        /* List/Table View */
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Device
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Organization
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Health
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
                {filteredDevices.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-6 py-12 text-center text-gray-500">
                      <Cpu className="h-12 w-12 mx-auto mb-4 text-gray-400" />
                      <p className="text-lg font-medium">No devices found</p>
                      <p className="text-sm mt-1">Try adjusting your search or filters</p>
                    </td>
                  </tr>
                ) : (
                  filteredDevices.map((device) => (
                    <tr key={device.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center">
                          <div className="flex-shrink-0 h-10 w-10">
                            <div
                              className={`h-10 w-10 rounded-full flex items-center justify-center ${
                                device.active ? 'bg-blue-600' : 'bg-gray-400'
                              } text-white`}
                            >
                              <Cpu className="h-5 w-5" />
                            </div>
                          </div>
                          <div className="ml-4">
                            <div className="text-sm font-medium text-gray-900">{device.name}</div>
                            <div className="text-sm text-gray-500">{device.externalId}</div>
                            {device.location && (
                              <div className="text-xs text-gray-400 flex items-center mt-1">
                                <MapPin className="h-3 w-3 mr-1" />
                                {device.location}
                              </div>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center text-sm text-gray-900">
                          <Building2 className="h-4 w-4 mr-2 text-gray-400" />
                          {device.organizationName || 'N/A'}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex flex-col gap-1">
                          <span
                            className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                              device.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                            }`}
                          >
                            {device.active ? 'Active' : 'Inactive'}
                          </span>
                          <span
                            className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(
                              device.status
                            )}`}
                          >
                            {device.status === 'ONLINE' ? (
                              <Wifi className="h-3 w-3 mr-1" />
                            ) : (
                              <WifiOff className="h-3 w-3 mr-1" />
                            )}
                            {device.status}
                          </span>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className={`flex items-center ${getHealthColor(device.healthScore)}`}>
                          <Heart className="h-4 w-4 mr-1" />
                          <span className="text-sm font-medium">{device.healthScore ?? 'N/A'}%</span>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        <div className="flex items-center">
                          <Clock className="h-4 w-4 mr-1 text-gray-400" />
                          {formatDate(device.lastSeenAt)}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <div className="flex items-center justify-end space-x-2">
                          <button
                            onClick={() => openEditModal(device)}
                            className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                            title="Edit device"
                          >
                            <Edit2 className="h-5 w-5" />
                          </button>
                          <button
                            onClick={() => handleToggleActive(device.id, device.active)}
                            className={`p-2 rounded-lg transition-colors ${
                              device.active
                                ? 'text-green-600 hover:bg-green-50'
                                : 'text-gray-600 hover:bg-gray-50'
                            }`}
                            title={device.active ? 'Disable device' : 'Enable device'}
                          >
                            <Power className="h-5 w-5" />
                          </button>
                          <button
                            onClick={() => handleDeleteDevice(device.id, device.name)}
                            className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                            title="Delete device"
                          >
                            <Trash2 className="h-5 w-5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      ) : (
        /* Card View */
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredDevices.length === 0 ? (
            <div className="col-span-full bg-white rounded-lg shadow p-12 text-center text-gray-500">
              <Cpu className="h-12 w-12 mx-auto mb-4 text-gray-400" />
              <p className="text-lg font-medium">No devices found</p>
              <p className="text-sm mt-1">Try adjusting your search or filters</p>
            </div>
          ) : (
            filteredDevices.map((device) => (
              <div
                key={device.id}
                className={`bg-white rounded-lg shadow-md border-l-4 ${
                  device.active
                    ? device.status === 'ONLINE'
                      ? 'border-green-500'
                      : 'border-yellow-500'
                    : 'border-gray-400'
                } hover:shadow-lg transition-shadow`}
              >
                <div className="p-5">
                  {/* Header */}
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex items-center">
                      <div
                        className={`h-12 w-12 rounded-full flex items-center justify-center ${
                          device.active ? 'bg-blue-600' : 'bg-gray-400'
                        } text-white`}
                      >
                        <Cpu className="h-6 w-6" />
                      </div>
                      <div className="ml-3">
                        <h3 className="text-lg font-semibold text-gray-900">{device.name}</h3>
                        <p className="text-sm text-gray-500">{device.externalId}</p>
                      </div>
                    </div>
                    <div className="flex flex-col gap-1">
                      <span
                        className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                          device.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                        }`}
                      >
                        {device.active ? 'Active' : 'Inactive'}
                      </span>
                      <span
                        className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(
                          device.status
                        )}`}
                      >
                        {device.status}
                      </span>
                    </div>
                  </div>

                  {/* Details */}
                  <div className="space-y-2 text-sm">
                    <div className="flex items-center text-gray-600">
                      <Building2 className="h-4 w-4 mr-2 text-gray-400" />
                      {device.organizationName || 'N/A'}
                    </div>
                    {device.location && (
                      <div className="flex items-center text-gray-600">
                        <MapPin className="h-4 w-4 mr-2 text-gray-400" />
                        {device.location}
                      </div>
                    )}
                    <div className="flex items-center text-gray-600">
                      <Clock className="h-4 w-4 mr-2 text-gray-400" />
                      Last seen: {formatDate(device.lastSeenAt)}
                    </div>
                    <div className={`flex items-center ${getHealthColor(device.healthScore)}`}>
                      <Heart className="h-4 w-4 mr-2" />
                      Health: {device.healthScore ?? 'N/A'}%
                    </div>
                    <div className="flex items-center text-gray-600">
                      <Key className="h-4 w-4 mr-2 text-gray-400" />
                      API Token: {device.hasApiToken ? 'Configured' : 'Not set'}
                    </div>
                    {device.sensorType && (
                      <div className="flex items-center text-gray-600">
                        <Activity className="h-4 w-4 mr-2 text-gray-400" />
                        Type: {device.sensorType}
                      </div>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex items-center justify-end space-x-2 mt-4 pt-4 border-t border-gray-100">
                    <button
                      onClick={() => openEditModal(device)}
                      className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                      title="Edit device"
                    >
                      <Edit2 className="h-5 w-5" />
                    </button>
                    <button
                      onClick={() => handleToggleActive(device.id, device.active)}
                      className={`p-2 rounded-lg transition-colors ${
                        device.active
                          ? 'text-green-600 hover:bg-green-50'
                          : 'text-gray-600 hover:bg-gray-50'
                      }`}
                      title={device.active ? 'Disable device' : 'Enable device'}
                    >
                      <Power className="h-5 w-5" />
                    </button>
                    <button
                      onClick={() => handleDeleteDevice(device.id, device.name)}
                      className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                      title="Delete device"
                    >
                      <Trash2 className="h-5 w-5" />
                    </button>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* Summary Footer */}
      <div className="bg-white rounded-lg shadow p-4">
        <div className="text-sm text-gray-600">
          Showing <span className="font-semibold">{filteredDevices.length}</span> of{' '}
          <span className="font-semibold">{devices.length}</span> devices
        </div>
      </div>

      {/* Edit Device Modal */}
      {editingDevice && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
            <div className="flex items-center justify-between px-6 py-4 border-b">
              <h3 className="text-lg font-semibold text-gray-900">Edit Device</h3>
              <button onClick={closeEditModal} className="text-gray-400 hover:text-gray-600">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="px-6 py-4 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
                <input
                  type="text"
                  value={editForm.name}
                  onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
                <textarea
                  value={editForm.description}
                  onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
                  rows={2}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Location</label>
                <input
                  type="text"
                  value={editForm.location}
                  onChange={(e) => setEditForm({ ...editForm, location: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Sensor Type</label>
                <input
                  type="text"
                  value={editForm.sensorType}
                  onChange={(e) => setEditForm({ ...editForm, sensorType: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Firmware Version
                </label>
                <input
                  type="text"
                  value={editForm.firmwareVersion}
                  onChange={(e) => setEditForm({ ...editForm, firmwareVersion: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 px-6 py-4 border-t bg-gray-50">
              <button
                onClick={closeEditModal}
                className="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleSaveDevice}
                disabled={saving || !editForm.name}
                className="flex items-center px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {saving ? (
                  <>
                    <svg
                      className="animate-spin -ml-1 mr-2 h-4 w-4 text-white"
                      fill="none"
                      viewBox="0 0 24 24"
                    >
                      <circle
                        className="opacity-25"
                        cx="12"
                        cy="12"
                        r="10"
                        stroke="currentColor"
                        strokeWidth="4"
                      />
                      <path
                        className="opacity-75"
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                      />
                    </svg>
                    Saving...
                  </>
                ) : (
                  <>
                    <Check className="h-4 w-4 mr-1" />
                    Save Changes
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminDevices;
