import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';

interface DashboardPermission {
  id: number;
  userId: number;
  username: string;
  email: string;
  permissionLevel: 'VIEW' | 'EDIT' | 'ADMIN';
  grantedAt: string;
  expiresAt: string | null;
}

interface DashboardSharingProps {
  dashboardId: number;
  isPublic: boolean;
  publicShareToken: string | null;
  onUpdate: () => void;
}

export const DashboardSharing: React.FC<DashboardSharingProps> = ({
  dashboardId,
  isPublic,
  publicShareToken,
  onUpdate,
}) => {
  const [permissions, setPermissions] = useState<DashboardPermission[]>([]);
  const [showAddUserModal, setShowAddUserModal] = useState(false);
  const [newPermission, setNewPermission] = useState({
    email: '',
    permissionLevel: 'VIEW' as DashboardPermission['permissionLevel'],
    expiresAt: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [shareUrl, setShareUrl] = useState<string | null>(null);

  useEffect(() => {
    loadPermissions();
    if (isPublic && publicShareToken) {
      setShareUrl(`${window.location.origin}/public/dashboard/${publicShareToken}`);
    }
  }, [dashboardId, isPublic, publicShareToken]);

  const loadPermissions = async () => {
    try {
      const response = await apiService.get(`/dashboards/${dashboardId}/permissions`);
      setPermissions(response.data);
    } catch (err) {
      console.error('Failed to load permissions:', err);
    }
  };

  const handleTogglePublic = async () => {
    try {
      setLoading(true);
      const response = await apiService.patch(`/dashboards/${dashboardId}/sharing`, {
        isPublic: !isPublic,
      });
      setShareUrl(response.data.publicShareUrl);
      onUpdate();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update sharing settings');
    } finally {
      setLoading(false);
    }
  };

  const handleAddPermission = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await apiService.post(`/dashboards/${dashboardId}/permissions`, {
        email: newPermission.email,
        permissionLevel: newPermission.permissionLevel,
        expiresAt: newPermission.expiresAt || null,
      });
      await loadPermissions();
      setShowAddUserModal(false);
      setNewPermission({ email: '', permissionLevel: 'VIEW', expiresAt: '' });
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to add permission');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdatePermission = async (
    permissionId: number,
    permissionLevel: DashboardPermission['permissionLevel']
  ) => {
    try {
      await apiService.patch(`/dashboards/${dashboardId}/permissions/${permissionId}`, {
        permissionLevel,
      });
      await loadPermissions();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update permission');
    }
  };

  const handleRevokePermission = async (permissionId: number) => {
    if (!confirm('Are you sure you want to revoke this permission?')) {
      return;
    }

    try {
      await apiService.delete(`/dashboards/${dashboardId}/permissions/${permissionId}`);
      await loadPermissions();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to revoke permission');
    }
  };

  const copyShareUrl = () => {
    if (shareUrl) {
      navigator.clipboard.writeText(shareUrl);
      alert('Share URL copied to clipboard!');
    }
  };

  return (
    <div className="space-y-6">
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      {/* Public Sharing */}
      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-semibold mb-4">Public Sharing</h3>
        <div className="flex items-center justify-between mb-4">
          <div>
            <p className="text-sm text-gray-600">
              Allow anyone with the link to view this dashboard
            </p>
          </div>
          <button
            onClick={handleTogglePublic}
            disabled={loading}
            className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
              isPublic ? 'bg-blue-600' : 'bg-gray-200'
            } ${loading ? 'opacity-50' : ''}`}
          >
            <span
              className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
                isPublic ? 'translate-x-6' : 'translate-x-1'
              }`}
            />
          </button>
        </div>

        {isPublic && shareUrl && (
          <div className="bg-gray-50 p-4 rounded-md">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Share URL
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={shareUrl}
                readOnly
                className="flex-1 px-3 py-2 border border-gray-300 rounded-md bg-white text-sm"
              />
              <button
                onClick={copyShareUrl}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 text-sm"
              >
                Copy
              </button>
            </div>
          </div>
        )}
      </div>

      {/* User Permissions */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-semibold">User Permissions</h3>
          <button
            onClick={() => setShowAddUserModal(true)}
            className="px-3 py-1 bg-blue-600 text-white rounded-md hover:bg-blue-700 text-sm"
          >
            + Add User
          </button>
        </div>

        {permissions.length === 0 ? (
          <p className="text-gray-500 text-center py-4">No user permissions set</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead>
                <tr>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                    User
                  </th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                    Permission
                  </th>
                  <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">
                    Granted
                  </th>
                  <th className="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {permissions.map((permission) => (
                  <tr key={permission.id}>
                    <td className="px-4 py-3">
                      <div>
                        <div className="text-sm font-medium text-gray-900">
                          {permission.username}
                        </div>
                        <div className="text-xs text-gray-500">{permission.email}</div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <select
                        value={permission.permissionLevel}
                        onChange={(e) =>
                          handleUpdatePermission(
                            permission.id,
                            e.target.value as DashboardPermission['permissionLevel']
                          )
                        }
                        className="text-sm border border-gray-300 rounded px-2 py-1"
                      >
                        <option value="VIEW">View</option>
                        <option value="EDIT">Edit</option>
                        <option value="ADMIN">Admin</option>
                      </select>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500">
                      {new Date(permission.grantedAt).toLocaleDateString()}
                      {permission.expiresAt && (
                        <div className="text-xs text-red-600">
                          Expires: {new Date(permission.expiresAt).toLocaleDateString()}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => handleRevokePermission(permission.id)}
                        className="text-red-600 hover:text-red-800 text-sm"
                      >
                        Revoke
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Add User Modal */}
      {showAddUserModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h2 className="text-xl font-bold mb-4">Add User Permission</h2>
            <form onSubmit={handleAddPermission}>
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  User Email
                </label>
                <input
                  type="email"
                  value={newPermission.email}
                  onChange={(e) =>
                    setNewPermission({ ...newPermission, email: e.target.value })
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="user@example.com"
                  required
                />
              </div>

              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Permission Level
                </label>
                <select
                  value={newPermission.permissionLevel}
                  onChange={(e) =>
                    setNewPermission({
                      ...newPermission,
                      permissionLevel: e.target.value as DashboardPermission['permissionLevel'],
                    })
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="VIEW">View - Read only access</option>
                  <option value="EDIT">Edit - Can modify widgets</option>
                  <option value="ADMIN">Admin - Full control</option>
                </select>
              </div>

              <div className="mb-6">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Expiration Date (optional)
                </label>
                <input
                  type="datetime-local"
                  value={newPermission.expiresAt}
                  onChange={(e) =>
                    setNewPermission({ ...newPermission, expiresAt: e.target.value })
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => {
                    setShowAddUserModal(false);
                    setNewPermission({ email: '', permissionLevel: 'VIEW', expiresAt: '' });
                  }}
                  className="flex-1 bg-gray-200 text-gray-700 py-2 rounded-md hover:bg-gray-300"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="flex-1 bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 disabled:bg-gray-400"
                >
                  {loading ? 'Adding...' : 'Add Permission'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
