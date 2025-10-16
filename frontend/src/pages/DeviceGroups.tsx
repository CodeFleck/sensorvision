import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';

interface DeviceGroup {
  id: number;
  name: string;
  description: string;
  deviceCount?: number;
  createdAt: string;
}

// interface Device {
//   id: number;
//   externalId: string;
//   name: string;
// }

const DeviceGroups: React.FC = () => {
  const [groups, setGroups] = useState<DeviceGroup[]>([]);
  // const [devices, setDevices] = useState<Device[]>([]);
  const [showModal, setShowModal] = useState(false);
  const [editingGroup, setEditingGroup] = useState<DeviceGroup | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    description: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadGroups();
    loadDevices();
  }, []);

  const loadGroups = async () => {
    try {
      setLoading(true);
      const response = await apiService.get('/device-groups');
      setGroups(response.data as DeviceGroup[]);
      setError(null);
    } catch (err: unknown) {
      setError('Failed to load device groups');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const loadDevices = async () => {
    try {
      const response = await apiService.get('/devices');
      // setDevices(response.data);
      console.log('Devices loaded:', response.data);
    } catch (err) {
      console.error('Failed to load devices:', err);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      if (editingGroup) {
        await apiService.put(`/device-groups/${editingGroup.id}`, formData);
      } else {
        await apiService.post('/device-groups', formData);
      }
      await loadGroups();
      handleCloseModal();
    } catch (err: unknown) {
      const message = err instanceof Error && 'response' in err
        ? ((err as { response?: { data?: { message?: string } } }).response?.data?.message || 'Failed to save device group')
        : 'Failed to save device group';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this device group?')) {
      return;
    }

    try {
      await apiService.delete(`/device-groups/${id}`);
      await loadGroups();
    } catch (err: unknown) {
      const message = err instanceof Error && 'response' in err
        ? ((err as { response?: { data?: { message?: string } } }).response?.data?.message || 'Failed to delete device group')
        : 'Failed to delete device group';
      setError(message);
    }
  };

  const handleEdit = (group: DeviceGroup) => {
    setEditingGroup(group);
    setFormData({
      name: group.name,
      description: group.description || '',
    });
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingGroup(null);
    setFormData({ name: '', description: '' });
    setError(null);
  };

  const handleCreateNew = () => {
    setEditingGroup(null);
    setFormData({ name: '', description: '' });
    setShowModal(true);
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold">Device Groups</h1>
        <button
          onClick={handleCreateNew}
          className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700"
        >
          + Create Group
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4">
          {error}
        </div>
      )}

      {loading && !showModal ? (
        <div className="text-center py-8">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {groups.map((group) => (
            <div key={group.id} className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition">
              <div className="flex justify-between items-start mb-4">
                <h3 className="text-xl font-semibold text-gray-800">{group.name}</h3>
                <div className="flex gap-2">
                  <button
                    onClick={() => handleEdit(group)}
                    className="text-blue-600 hover:text-blue-800"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => handleDelete(group.id)}
                    className="text-red-600 hover:text-red-800"
                  >
                    Delete
                  </button>
                </div>
              </div>
              {group.description && (
                <p className="text-gray-600 mb-4">{group.description}</p>
              )}
              <div className="flex items-center text-sm text-gray-500">
                <span className="font-medium">{group.deviceCount || 0}</span>
                <span className="ml-1">devices</span>
              </div>
              <div className="text-xs text-gray-400 mt-2">
                Created: {new Date(group.createdAt).toLocaleDateString()}
              </div>
            </div>
          ))}
        </div>
      )}

      {groups.length === 0 && !loading && (
        <div className="text-center py-12">
          <p className="text-gray-500 mb-4">No device groups yet</p>
          <button
            onClick={handleCreateNew}
            className="bg-blue-600 text-white px-6 py-2 rounded-md hover:bg-blue-700"
          >
            Create Your First Group
          </button>
        </div>
      )}

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-8 max-w-md w-full mx-4">
            <h2 className="text-2xl font-bold mb-4">
              {editingGroup ? 'Edit Device Group' : 'Create Device Group'}
            </h2>
            <form onSubmit={handleSubmit}>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Group Name
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="Building A Sensors"
                  required
                />
              </div>
              <div className="mb-6">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Description
                </label>
                <textarea
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="All sensors located in Building A"
                  rows={3}
                />
              </div>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={handleCloseModal}
                  className="flex-1 bg-gray-200 text-gray-700 py-2 rounded-md hover:bg-gray-300"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="flex-1 bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 disabled:bg-gray-400"
                >
                  {loading ? 'Saving...' : editingGroup ? 'Update' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default DeviceGroups;
