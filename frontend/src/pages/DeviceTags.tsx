import React, { useState, useEffect, useCallback } from 'react';
import { apiService } from '../services/api';
import { MultiSelect, MultiSelectOption } from '../components/MultiSelect';
import { Users, ChevronDown, ChevronUp } from 'lucide-react';

interface DeviceTag {
  id: number;
  name: string;
  color: string;
  deviceCount?: number;
  deviceIds?: string[];
  createdAt: string;
}

interface Device {
  id: number;
  externalId: string;
  name: string;
}

const PRESET_COLORS = [
  '#FF5733', '#33FF57', '#3357FF', '#FF33F5', '#F5FF33',
  '#33FFF5', '#FF8C33', '#8C33FF', '#33FF8C', '#FF3333',
  '#33FF33', '#3333FF', '#FFFF33', '#33FFFF', '#FF33FF',
];

const DeviceTags: React.FC = () => {
  const [tags, setTags] = useState<DeviceTag[]>([]);
  const [devices, setDevices] = useState<Device[]>([]);
  const [showModal, setShowModal] = useState(false);
  const [editingTag, setEditingTag] = useState<DeviceTag | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    color: '#FF5733',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [expandedTagId, setExpandedTagId] = useState<number | null>(null);
  const [savingDevices, setSavingDevices] = useState(false);

  useEffect(() => {
    loadTags();
    loadDevices();
  }, []);

  const loadTags = async () => {
    try {
      setLoading(true);
      const response = await apiService.get('/device-tags');
      setTags(response.data as DeviceTag[]);
      setError(null);
    } catch (err: unknown) {
      setError('Failed to load device tags');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const loadDevices = async () => {
    try {
      const response = await apiService.get('/devices');
      setDevices(response.data as Device[]);
    } catch (err) {
      console.error('Failed to load devices:', err);
    }
  };

  const deviceOptions: MultiSelectOption[] = devices.map(d => ({
    value: d.externalId,
    label: `${d.name} (${d.externalId})`,
  }));

  const handleDeviceSelectionChange = useCallback(async (tagId: number, currentDeviceIds: string[], newDeviceIds: string[]) => {
    const toAdd = newDeviceIds.filter(id => !currentDeviceIds.includes(id));
    const toRemove = currentDeviceIds.filter(id => !newDeviceIds.includes(id));

    if (toAdd.length === 0 && toRemove.length === 0) return;

    setSavingDevices(true);
    setError(null);

    try {
      // Add tag to new devices
      for (const deviceId of toAdd) {
        await apiService.post(`/device-tags/${tagId}/devices/${deviceId}`);
      }
      // Remove tag from devices
      for (const deviceId of toRemove) {
        await apiService.delete(`/device-tags/${tagId}/devices/${deviceId}`);
      }
      await loadTags();
    } catch (err: unknown) {
      const message = err instanceof Error && 'response' in err
        ? ((err as { response?: { data?: { message?: string } } }).response?.data?.message || 'Failed to update device assignments')
        : 'Failed to update device assignments';
      setError(message);
    } finally {
      setSavingDevices(false);
    }
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      if (editingTag) {
        await apiService.put(`/device-tags/${editingTag.id}`, formData);
      } else {
        await apiService.post('/device-tags', formData);
      }
      await loadTags();
      handleCloseModal();
    } catch (err: unknown) {
      const message = err instanceof Error && 'response' in err
        ? ((err as { response?: { data?: { message?: string } } }).response?.data?.message || 'Failed to save device tag')
        : 'Failed to save device tag';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this tag?')) {
      return;
    }

    try {
      await apiService.delete(`/device-tags/${id}`);
      await loadTags();
    } catch (err: unknown) {
      const message = err instanceof Error && 'response' in err
        ? ((err as { response?: { data?: { message?: string } } }).response?.data?.message || 'Failed to delete device tag')
        : 'Failed to delete device tag';
      setError(message);
    }
  };

  const handleEdit = (tag: DeviceTag) => {
    setEditingTag(tag);
    setFormData({
      name: tag.name,
      color: tag.color || '#FF5733',
    });
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingTag(null);
    setFormData({ name: '', color: '#FF5733' });
    setError(null);
  };

  const handleCreateNew = () => {
    setEditingTag(null);
    setFormData({ name: '', color: '#FF5733' });
    setShowModal(true);
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold">Device Tags</h1>
        <button
          onClick={handleCreateNew}
          className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700"
        >
          + Create Tag
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
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead className="bg-gray-50 dark:bg-gray-900">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Tag
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Color
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Devices
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Created
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
              {tags.map((tag) => {
                const isExpanded = expandedTagId === tag.id;
                const tagDeviceIds = tag.deviceIds || [];
                return (
                  <React.Fragment key={tag.id}>
                    <tr className="hover:bg-gray-50 dark:hover:bg-gray-700">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center">
                          <span
                            className="inline-block px-3 py-1 rounded-full text-white text-sm font-medium"
                            style={{ backgroundColor: tag.color }}
                          >
                            {tag.name}
                          </span>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center gap-2">
                          <div
                            className="w-6 h-6 rounded border border-gray-300 dark:border-gray-600"
                            style={{ backgroundColor: tag.color }}
                          />
                          <span className="text-sm text-gray-600 dark:text-gray-400">{tag.color}</span>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-600 dark:text-gray-400">
                        {tag.deviceCount || 0}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500 dark:text-gray-400">
                        {new Date(tag.createdAt).toLocaleDateString()}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <button
                          onClick={() => setExpandedTagId(isExpanded ? null : tag.id)}
                          className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 mr-4 inline-flex items-center gap-1"
                          title="Manage devices with this tag"
                        >
                          <Users className="h-4 w-4" />
                          {isExpanded ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
                        </button>
                        <button
                          onClick={() => handleEdit(tag)}
                          className="text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300 mr-4"
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => handleDelete(tag.id)}
                          className="text-red-600 hover:text-red-800 dark:text-red-400 dark:hover:text-red-300"
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                    {isExpanded && (
                      <tr className="bg-gray-50 dark:bg-gray-750">
                        <td colSpan={5} className="px-6 py-4">
                          <div className="flex items-start gap-4">
                            <div className="flex-1 max-w-md">
                              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                                Devices with this tag:
                              </label>
                              <MultiSelect
                                options={deviceOptions}
                                selected={tagDeviceIds}
                                onChange={(newSelection) => handleDeviceSelectionChange(tag.id, tagDeviceIds, newSelection)}
                                placeholder="Select devices to tag..."
                                searchPlaceholder="Search devices..."
                                disabled={savingDevices}
                                aria-label={`Select devices for tag ${tag.name}`}
                              />
                              {savingDevices && (
                                <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">Saving changes...</p>
                              )}
                            </div>
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {tags.length === 0 && !loading && (
        <div className="text-center py-12">
          <p className="text-gray-500 mb-4">No tags yet</p>
          <button
            onClick={handleCreateNew}
            className="bg-blue-600 text-white px-6 py-2 rounded-md hover:bg-blue-700"
          >
            Create Your First Tag
          </button>
        </div>
      )}

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-8 max-w-md w-full mx-4">
            <h2 className="text-2xl font-bold mb-4">
              {editingTag ? 'Edit Device Tag' : 'Create Device Tag'}
            </h2>
            <form onSubmit={handleSubmit}>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Tag Name
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="critical, outdoor, maintenance-required"
                  required
                />
              </div>

              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Color
                </label>
                <div className="flex items-center gap-3 mb-3">
                  <input
                    type="color"
                    value={formData.color}
                    onChange={(e) => setFormData({ ...formData, color: e.target.value })}
                    className="h-10 w-20 rounded border border-gray-300 cursor-pointer"
                  />
                  <input
                    type="text"
                    value={formData.color}
                    onChange={(e) => setFormData({ ...formData, color: e.target.value })}
                    className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="#FF5733"
                    pattern="^#[0-9A-Fa-f]{6}$"
                    required
                  />
                </div>
                <div className="flex flex-wrap gap-2">
                  {PRESET_COLORS.map((color) => (
                    <button
                      key={color}
                      type="button"
                      onClick={() => setFormData({ ...formData, color })}
                      className={`w-8 h-8 rounded border-2 ${
                        formData.color === color ? 'border-gray-900' : 'border-gray-300'
                      }`}
                      style={{ backgroundColor: color }}
                      title={color}
                    />
                  ))}
                </div>
              </div>

              <div className="mb-6">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Preview
                </label>
                <div className="p-4 bg-gray-50 rounded-md">
                  <span
                    className="inline-block px-3 py-1 rounded-full text-white text-sm font-medium"
                    style={{ backgroundColor: formData.color }}
                  >
                    {formData.name || 'Tag Name'}
                  </span>
                </div>
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
                  {loading ? 'Saving...' : editingTag ? 'Update' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default DeviceTags;
