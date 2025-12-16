import { useState, useEffect } from 'react';
import {
  Users,
  Search,
  Power,
  Trash2,
  Shield,
  Mail,
  Calendar,
  Building2,
  UserCog,
  X,
  Check,
  LayoutGrid,
  List,
  UserCheck,
  UserX,
} from 'lucide-react';
import { User } from '../types';
import { apiService } from '../services/api';
import toast from 'react-hot-toast';

interface RoleInfo {
  id: number;
  name: string;
  description: string;
}

type ViewMode = 'list' | 'cards';

export const AdminUsers = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterOrganization, setFilterOrganization] = useState<string>('all');
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [viewMode, setViewMode] = useState<ViewMode>('list');
  const [availableRoles, setAvailableRoles] = useState<RoleInfo[]>([]);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [selectedRoles, setSelectedRoles] = useState<string[]>([]);
  const [savingRoles, setSavingRoles] = useState(false);

  useEffect(() => {
    fetchUsers();
    fetchRoles();
  }, []);

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const data = await apiService.getAllUsers();
      setUsers(data);
    } catch (error) {
      console.error('Failed to fetch users:', error);
      toast.error('Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  const fetchRoles = async () => {
    try {
      const roles = await apiService.getAllRoles();
      setAvailableRoles(roles);
    } catch (error) {
      console.error('Failed to fetch roles:', error);
    }
  };

  const openRoleEditor = (user: User) => {
    setEditingUser(user);
    setSelectedRoles([...user.roles]);
  };

  const closeRoleEditor = () => {
    setEditingUser(null);
    setSelectedRoles([]);
  };

  const toggleRole = (roleName: string) => {
    setSelectedRoles(prev =>
      prev.includes(roleName)
        ? prev.filter(r => r !== roleName)
        : [...prev, roleName]
    );
  };

  const handleSaveRoles = async () => {
    if (!editingUser) return;

    try {
      setSavingRoles(true);
      await apiService.updateUserRoles(editingUser.id, selectedRoles);
      toast.success('User roles updated successfully');
      await fetchUsers();
      closeRoleEditor();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to update roles');
    } finally {
      setSavingRoles(false);
    }
  };

  const handleToggleEnabled = async (userId: number, currentStatus: boolean) => {
    try {
      if (currentStatus) {
        await apiService.disableUser(userId);
        toast.success('User disabled successfully');
      } else {
        await apiService.enableUser(userId);
        toast.success('User enabled successfully');
      }
      await fetchUsers();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to toggle user status');
    }
  };

  const handleDeleteUser = async (userId: number, username: string) => {
    if (!window.confirm(`Are you sure you want to move user "${username}" to trash? You can restore it within 30 days.`)) {
      return;
    }

    try {
      const response = await apiService.softDeleteUser(userId);
      const trashId = response.data.trashId;
      const daysRemaining = response.data.daysRemaining;

      toast(
        (t) => (
          <div className="flex items-center gap-3">
            <span>User moved to trash ({daysRemaining} days to restore)</span>
            <button
              onClick={async () => {
                try {
                  await apiService.restoreTrashItem(trashId);
                  toast.dismiss(t.id);
                  toast.success('User restored successfully');
                  await fetchUsers();
                } catch (err) {
                  toast.error('Failed to restore user');
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
      await fetchUsers();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to delete user');
    }
  };

  const filteredUsers = users.filter(user => {
    const matchesSearch =
      user.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.firstName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.lastName?.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesOrganization =
      filterOrganization === 'all' ||
      user.organizationName === filterOrganization;

    const matchesStatus =
      filterStatus === 'all' ||
      (filterStatus === 'active' && user.enabled) ||
      (filterStatus === 'inactive' && !user.enabled);

    return matchesSearch && matchesOrganization && matchesStatus;
  });

  const organizations = Array.from(new Set(users.map(u => u.organizationName))).sort();

  // Stats
  const totalUsers = users.length;
  const activeUsers = users.filter(u => u.enabled).length;
  const inactiveUsers = users.filter(u => !u.enabled).length;
  const adminUsers = users.filter(u => u.roles.includes('ROLE_ADMIN')).length;

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading users...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">User Management</h1>
          <p className="text-gray-600 mt-1">Manage system users and their access</p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setViewMode('list')}
            className={`p-2 rounded-lg transition-colors ${
              viewMode === 'list'
                ? 'bg-blue-100 text-blue-600'
                : 'text-gray-400 hover:text-gray-600 hover:bg-gray-100'
            }`}
            title="List view"
          >
            <List className="h-5 w-5" />
          </button>
          <button
            onClick={() => setViewMode('cards')}
            className={`p-2 rounded-lg transition-colors ${
              viewMode === 'cards'
                ? 'bg-blue-100 text-blue-600'
                : 'text-gray-400 hover:text-gray-600 hover:bg-gray-100'
            }`}
            title="Card view"
          >
            <LayoutGrid className="h-5 w-5" />
          </button>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-lg shadow p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Total Users</p>
              <p className="text-2xl font-bold text-gray-900">{totalUsers}</p>
            </div>
            <div className="p-3 bg-blue-100 rounded-full">
              <Users className="h-6 w-6 text-blue-600" />
            </div>
          </div>
        </div>
        <div className="bg-white rounded-lg shadow p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Active Users</p>
              <p className="text-2xl font-bold text-green-600">{activeUsers}</p>
            </div>
            <div className="p-3 bg-green-100 rounded-full">
              <UserCheck className="h-6 w-6 text-green-600" />
            </div>
          </div>
        </div>
        <div className="bg-white rounded-lg shadow p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Inactive Users</p>
              <p className="text-2xl font-bold text-red-600">{inactiveUsers}</p>
            </div>
            <div className="p-3 bg-red-100 rounded-full">
              <UserX className="h-6 w-6 text-red-600" />
            </div>
          </div>
        </div>
        <div className="bg-white rounded-lg shadow p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Admin Users</p>
              <p className="text-2xl font-bold text-purple-600">{adminUsers}</p>
            </div>
            <div className="p-3 bg-purple-100 rounded-full">
              <Shield className="h-6 w-6 text-purple-600" />
            </div>
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
              placeholder="Search users by name, username, or email..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          <div>
            <select
              value={filterOrganization}
              onChange={(e) => setFilterOrganization(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="all">All Organizations</option>
              {organizations.map((org) => (
                <option key={org} value={org}>{org}</option>
              ))}
            </select>
          </div>

          <div>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="all">All Status</option>
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
            </select>
          </div>
        </div>
      </div>

      {/* Card View */}
      {viewMode === 'cards' && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredUsers.length === 0 ? (
            <div className="col-span-full bg-white rounded-lg shadow p-12 text-center">
              <Users className="h-12 w-12 mx-auto mb-4 text-gray-400" />
              <p className="text-lg font-medium text-gray-900">No users found</p>
              <p className="text-sm text-gray-500 mt-1">Try adjusting your search or filters</p>
            </div>
          ) : (
            filteredUsers.map((user) => (
              <div key={user.id} className="bg-white rounded-lg shadow hover:shadow-md transition-shadow">
                <div className="p-4">
                  <div className="flex items-start justify-between">
                    <div className="flex items-center">
                      <div className="h-12 w-12 rounded-full bg-blue-600 flex items-center justify-center text-white font-semibold text-lg">
                        {user.firstName?.[0]}{user.lastName?.[0]}
                      </div>
                      <div className="ml-3">
                        <h3 className="text-sm font-semibold text-gray-900">
                          {user.firstName} {user.lastName}
                        </h3>
                        <p className="text-sm text-gray-500">@{user.username}</p>
                      </div>
                    </div>
                    <span
                      className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                        user.enabled
                          ? 'bg-green-100 text-green-800'
                          : 'bg-red-100 text-red-800'
                      }`}
                    >
                      {user.enabled ? 'Active' : 'Inactive'}
                    </span>
                  </div>

                  <div className="mt-4 space-y-2">
                    <div className="flex items-center text-sm text-gray-600">
                      <Mail className="h-4 w-4 mr-2 text-gray-400" />
                      {user.email}
                    </div>
                    <div className="flex items-center text-sm text-gray-600">
                      <Building2 className="h-4 w-4 mr-2 text-gray-400" />
                      {user.organizationName}
                    </div>
                    <div className="flex flex-wrap gap-1 mt-2">
                      {user.roles.map((role) => (
                        <span
                          key={role}
                          className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                            role === 'ROLE_ADMIN'
                              ? 'bg-purple-100 text-purple-800'
                              : 'bg-gray-100 text-gray-800'
                          }`}
                        >
                          {role === 'ROLE_ADMIN' && <Shield className="h-3 w-3 mr-1" />}
                          {role.replace('ROLE_', '')}
                        </span>
                      ))}
                    </div>
                  </div>

                  <div className="mt-4 pt-4 border-t flex items-center justify-between">
                    <div className="text-xs text-gray-500">
                      {user.lastLoginAt ? (
                        <span className="flex items-center">
                          <Calendar className="h-3 w-3 mr-1" />
                          Last login: {new Date(user.lastLoginAt).toLocaleDateString()}
                        </span>
                      ) : (
                        <span>Never logged in</span>
                      )}
                    </div>
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => openRoleEditor(user)}
                        className="p-1.5 text-blue-600 hover:bg-blue-50 rounded transition-colors"
                        title="Edit roles"
                      >
                        <UserCog className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => handleToggleEnabled(user.id, user.enabled)}
                        className={`p-1.5 rounded transition-colors ${
                          user.enabled
                            ? 'text-green-600 hover:bg-green-50'
                            : 'text-gray-600 hover:bg-gray-50'
                        }`}
                        title={user.enabled ? 'Disable user' : 'Enable user'}
                      >
                        <Power className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => handleDeleteUser(user.id, user.username)}
                        className="p-1.5 text-red-600 hover:bg-red-50 rounded transition-colors"
                        title="Delete user"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* List View */}
      {viewMode === 'list' && (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    User
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Organization
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Roles
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Last Login
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filteredUsers.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="px-6 py-12 text-center text-gray-500">
                      <Users className="h-12 w-12 mx-auto mb-4 text-gray-400" />
                      <p className="text-lg font-medium">No users found</p>
                      <p className="text-sm mt-1">Try adjusting your search or filters</p>
                    </td>
                  </tr>
                ) : (
                  filteredUsers.map((user) => (
                    <tr key={user.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center">
                          <div className="flex-shrink-0 h-10 w-10">
                            <div className="h-10 w-10 rounded-full bg-blue-600 flex items-center justify-center text-white font-semibold">
                              {user.firstName?.[0]}{user.lastName?.[0]}
                            </div>
                          </div>
                          <div className="ml-4">
                            <div className="text-sm font-medium text-gray-900">
                              {user.firstName} {user.lastName}
                            </div>
                            <div className="text-sm text-gray-500">
                              @{user.username}
                            </div>
                            <div className="text-sm text-gray-500 flex items-center mt-1">
                              <Mail className="h-3 w-3 mr-1" />
                              {user.email}
                            </div>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center text-sm text-gray-900">
                          <Building2 className="h-4 w-4 mr-2 text-gray-400" />
                          {user.organizationName}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex flex-wrap gap-1">
                          {user.roles.map((role) => (
                            <span
                              key={role}
                              className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                                role === 'ROLE_ADMIN'
                                  ? 'bg-purple-100 text-purple-800'
                                  : 'bg-gray-100 text-gray-800'
                              }`}
                            >
                              {role === 'ROLE_ADMIN' && <Shield className="h-3 w-3 mr-1" />}
                              {role.replace('ROLE_', '')}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex flex-col gap-1">
                          <span
                            className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                              user.enabled
                                ? 'bg-green-100 text-green-800'
                                : 'bg-red-100 text-red-800'
                            }`}
                          >
                            {user.enabled ? 'Active' : 'Disabled'}
                          </span>
                          {user.emailVerified ? (
                            <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                              Email Verified
                            </span>
                          ) : (
                            <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
                              Email Unverified
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {user.lastLoginAt ? (
                          <div className="flex items-center">
                            <Calendar className="h-4 w-4 mr-1 text-gray-400" />
                            {new Date(user.lastLoginAt).toLocaleDateString()}
                          </div>
                        ) : (
                          <span className="text-gray-400">Never</span>
                        )}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <div className="flex items-center justify-end space-x-2">
                          <button
                            onClick={() => openRoleEditor(user)}
                            className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                            title="Edit roles"
                          >
                            <UserCog className="h-5 w-5" />
                          </button>
                          <button
                            onClick={() => handleToggleEnabled(user.id, user.enabled)}
                            className={`p-2 rounded-lg transition-colors ${
                              user.enabled
                                ? 'text-green-600 hover:bg-green-50'
                                : 'text-gray-600 hover:bg-gray-50'
                            }`}
                            title={user.enabled ? 'Disable user' : 'Enable user'}
                          >
                            <Power className="h-5 w-5" />
                          </button>
                          <button
                            onClick={() => handleDeleteUser(user.id, user.username)}
                            className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                            title="Delete user"
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
      )}

      {/* Summary Footer */}
      <div className="bg-white rounded-lg shadow p-4">
        <div className="text-sm text-gray-600">
          Showing <span className="font-semibold">{filteredUsers.length}</span> of{' '}
          <span className="font-semibold">{users.length}</span> users
        </div>
      </div>

      {/* Role Editing Modal */}
      {editingUser && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
            <div className="flex items-center justify-between px-6 py-4 border-b">
              <h3 className="text-lg font-semibold text-gray-900">
                Edit Roles for {editingUser.firstName} {editingUser.lastName}
              </h3>
              <button
                onClick={closeRoleEditor}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="px-6 py-4">
              <p className="text-sm text-gray-600 mb-4">
                Select the roles for <span className="font-medium">@{editingUser.username}</span>
              </p>

              <div className="space-y-3">
                {availableRoles.map((role) => (
                  <label
                    key={role.id}
                    className={`flex items-start p-3 rounded-lg border cursor-pointer transition-colors ${
                      selectedRoles.includes(role.name)
                        ? 'border-blue-500 bg-blue-50'
                        : 'border-gray-200 hover:bg-gray-50'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={selectedRoles.includes(role.name)}
                      onChange={() => toggleRole(role.name)}
                      className="mt-1 h-4 w-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                    />
                    <div className="ml-3">
                      <div className="flex items-center">
                        {role.name === 'ROLE_ADMIN' && <Shield className="h-4 w-4 mr-1 text-purple-600" />}
                        {role.name === 'ROLE_DEVELOPER' && <UserCog className="h-4 w-4 mr-1 text-cyan-600" />}
                        <span className={`font-medium ${
                          role.name === 'ROLE_ADMIN' ? 'text-purple-700' :
                          role.name === 'ROLE_DEVELOPER' ? 'text-cyan-700' :
                          'text-gray-900'
                        }`}>
                          {role.name.replace('ROLE_', '')}
                        </span>
                      </div>
                      {role.description && (
                        <p className="text-sm text-gray-500 mt-1">{role.description}</p>
                      )}
                    </div>
                  </label>
                ))}
              </div>

              {selectedRoles.length === 0 && (
                <p className="text-sm text-amber-600 mt-3">
                  Warning: User must have at least one role assigned.
                </p>
              )}
            </div>

            <div className="flex items-center justify-end gap-3 px-6 py-4 border-t bg-gray-50">
              <button
                onClick={closeRoleEditor}
                className="px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleSaveRoles}
                disabled={savingRoles || selectedRoles.length === 0}
                className="flex items-center px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {savingRoles ? (
                  <>
                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                    Saving...
                  </>
                ) : (
                  <>
                    <Check className="h-4 w-4 mr-1" />
                    Save Roles
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

export default AdminUsers;
