import { useState, useEffect } from 'react';
import { Users, Search, Power, Trash2, Shield, Mail, Calendar, Building2 } from 'lucide-react';
import { User } from '../types';
import { apiService } from '../services/api';
import toast from 'react-hot-toast';

export const AdminUsers = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterOrganization, setFilterOrganization] = useState<string>('all');

  useEffect(() => {
    fetchUsers();
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
    if (!window.confirm(`Are you sure you want to delete user "${username}"? This action cannot be undone.`)) {
      return;
    }

    try {
      await apiService.deleteUser(userId);
      toast.success('User deleted successfully');
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

    return matchesSearch && matchesOrganization;
  });

  const organizations = Array.from(new Set(users.map(u => u.organizationName))).sort();

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading users...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">User Management</h1>
          <p className="text-gray-600 mt-1">Manage system users and their access</p>
        </div>
        <div className="flex items-center space-x-2">
          <span className="text-sm text-gray-600">
            Total Users: <span className="font-semibold">{users.length}</span>
          </span>
          <span className="text-sm text-gray-600">•</span>
          <span className="text-sm text-gray-600">
            Active: <span className="font-semibold text-green-600">{users.filter(u => u.enabled).length}</span>
          </span>
          <span className="text-sm text-gray-600">•</span>
          <span className="text-sm text-gray-600">
            Inactive: <span className="font-semibold text-red-600">{users.filter(u => !u.enabled).length}</span>
          </span>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg shadow p-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
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
        </div>
      </div>

      {/* Users Table */}
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

      {/* Summary Footer */}
      <div className="bg-white rounded-lg shadow p-4">
        <div className="text-sm text-gray-600">
          Showing <span className="font-semibold">{filteredUsers.length}</span> of{' '}
          <span className="font-semibold">{users.length}</span> users
        </div>
      </div>
    </div>
  );
};

export default AdminUsers;
