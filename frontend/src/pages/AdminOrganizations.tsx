import { useState, useEffect } from 'react';
import {
  Building2,
  Search,
  Power,
  Edit,
  Trash2,
  Users,
  Calendar,
  X,
  LayoutGrid,
  List,
  CheckCircle,
  XCircle,
} from 'lucide-react';
import { Organization } from '../types';
import { apiService } from '../services/api';
import toast from 'react-hot-toast';

type ViewMode = 'list' | 'cards';

export const AdminOrganizations = () => {
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [viewMode, setViewMode] = useState<ViewMode>('cards');
  const [editingOrg, setEditingOrg] = useState<Organization | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);

  useEffect(() => {
    fetchOrganizations();
  }, []);

  const fetchOrganizations = async () => {
    try {
      setLoading(true);
      const data = await apiService.getAllOrganizations();
      setOrganizations(data);
    } catch (error) {
      console.error('Failed to fetch organizations:', error);
      toast.error('Failed to load organizations');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleEnabled = async (orgId: number, currentStatus: boolean) => {
    try {
      if (currentStatus) {
        await apiService.disableOrganization(orgId);
        toast.success('Organization disabled successfully');
      } else {
        await apiService.enableOrganization(orgId);
        toast.success('Organization enabled successfully');
      }
      await fetchOrganizations();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to toggle organization status');
    }
  };

  const handleEdit = (org: Organization) => {
    setEditingOrg(org);
    setIsEditModalOpen(true);
  };

  const handleUpdate = async (orgId: number, data: { name?: string; description?: string }) => {
    try {
      await apiService.updateOrganization(orgId, data);
      toast.success('Organization updated successfully');
      setIsEditModalOpen(false);
      setEditingOrg(null);
      await fetchOrganizations();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to update organization');
    }
  };

  const handleDeleteOrganization = async (orgId: number, orgName: string) => {
    if (!window.confirm(`Are you sure you want to move organization "${orgName}" to trash? You can restore it within 30 days. This will fail if the organization has active users.`)) {
      return;
    }

    try {
      const response = await apiService.softDeleteOrganization(orgId);
      const trashId = response.data.trashId;
      const daysRemaining = response.data.daysRemaining;

      toast(
        (t) => (
          <div className="flex items-center gap-3">
            <span>Organization moved to trash ({daysRemaining} days to restore)</span>
            <button
              onClick={async () => {
                try {
                  await apiService.restoreTrashItem(trashId);
                  toast.dismiss(t.id);
                  toast.success('Organization restored successfully');
                  await fetchOrganizations();
                } catch (err) {
                  toast.error('Failed to restore organization');
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
      await fetchOrganizations();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to delete organization');
    }
  };

  const filteredOrganizations = organizations.filter(org => {
    const matchesSearch =
      org.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      org.description?.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesStatus =
      filterStatus === 'all' ||
      (filterStatus === 'active' && org.enabled) ||
      (filterStatus === 'inactive' && !org.enabled);

    return matchesSearch && matchesStatus;
  });

  // Stats
  const totalOrganizations = organizations.length;
  const activeOrganizations = organizations.filter(o => o.enabled).length;
  const inactiveOrganizations = organizations.filter(o => !o.enabled).length;
  const totalUsers = organizations.reduce((sum, o) => sum + (o.userCount || 0), 0);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading organizations...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Organization Management</h1>
          <p className="text-gray-600 mt-1">Manage organizations and their settings</p>
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
              <p className="text-sm text-gray-600">Total Organizations</p>
              <p className="text-2xl font-bold text-gray-900">{totalOrganizations}</p>
            </div>
            <div className="p-3 bg-blue-100 rounded-full">
              <Building2 className="h-6 w-6 text-blue-600" />
            </div>
          </div>
        </div>
        <div className="bg-white rounded-lg shadow p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Active Organizations</p>
              <p className="text-2xl font-bold text-green-600">{activeOrganizations}</p>
            </div>
            <div className="p-3 bg-green-100 rounded-full">
              <CheckCircle className="h-6 w-6 text-green-600" />
            </div>
          </div>
        </div>
        <div className="bg-white rounded-lg shadow p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Inactive Organizations</p>
              <p className="text-2xl font-bold text-red-600">{inactiveOrganizations}</p>
            </div>
            <div className="p-3 bg-red-100 rounded-full">
              <XCircle className="h-6 w-6 text-red-600" />
            </div>
          </div>
        </div>
        <div className="bg-white rounded-lg shadow p-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Total Users</p>
              <p className="text-2xl font-bold text-purple-600">{totalUsers}</p>
            </div>
            <div className="p-3 bg-purple-100 rounded-full">
              <Users className="h-6 w-6 text-purple-600" />
            </div>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg shadow p-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search organizations by name or description..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
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
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredOrganizations.length === 0 ? (
            <div className="col-span-full">
              <div className="bg-white rounded-lg shadow p-12 text-center">
                <Building2 className="h-12 w-12 mx-auto mb-4 text-gray-400" />
                <p className="text-lg font-medium text-gray-900">No organizations found</p>
                <p className="text-sm text-gray-500 mt-1">Try adjusting your search or filters</p>
              </div>
            </div>
          ) : (
            filteredOrganizations.map((org) => (
              <div key={org.id} className="bg-white rounded-lg shadow hover:shadow-lg transition-shadow">
                <div className="p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex-1">
                      <h3 className="text-lg font-semibold text-gray-900 flex items-center">
                        <Building2 className="h-5 w-5 mr-2 text-blue-600" />
                        {org.name}
                      </h3>
                      {org.description && (
                        <p className="text-sm text-gray-600 mt-2">{org.description}</p>
                      )}
                    </div>
                    <span
                      className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                        org.enabled
                          ? 'bg-green-100 text-green-800'
                          : 'bg-red-100 text-red-800'
                      }`}
                    >
                      {org.enabled ? 'Active' : 'Disabled'}
                    </span>
                  </div>

                  <div className="space-y-2 mb-4">
                    <div className="flex items-center text-sm text-gray-600">
                      <Users className="h-4 w-4 mr-2 text-gray-400" />
                      <span>{org.userCount || 0} user{org.userCount !== 1 ? 's' : ''}</span>
                    </div>
                    <div className="flex items-center text-sm text-gray-600">
                      <Calendar className="h-4 w-4 mr-2 text-gray-400" />
                      <span>Created {new Date(org.createdAt).toLocaleDateString()}</span>
                    </div>
                  </div>

                  <div className="flex items-center justify-between pt-4 border-t">
                    <button
                      onClick={() => handleToggleEnabled(org.id, org.enabled)}
                      className={`flex items-center space-x-1 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                        org.enabled
                          ? 'text-green-700 bg-green-50 hover:bg-green-100'
                          : 'text-gray-700 bg-gray-50 hover:bg-gray-100'
                      }`}
                    >
                      <Power className="h-4 w-4" />
                      <span>{org.enabled ? 'Disable' : 'Enable'}</span>
                    </button>

                    <div className="flex items-center space-x-2">
                      <button
                        onClick={() => handleEdit(org)}
                        className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                        title="Edit organization"
                      >
                        <Edit className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => handleDeleteOrganization(org.id, org.name)}
                        className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                        title="Delete organization"
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
                    Organization
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Users
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Created
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {filteredOrganizations.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-6 py-12 text-center text-gray-500">
                      <Building2 className="h-12 w-12 mx-auto mb-4 text-gray-400" />
                      <p className="text-lg font-medium">No organizations found</p>
                      <p className="text-sm mt-1">Try adjusting your search or filters</p>
                    </td>
                  </tr>
                ) : (
                  filteredOrganizations.map((org) => (
                    <tr key={org.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center">
                          <div className="flex-shrink-0 h-10 w-10">
                            <div className="h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center">
                              <Building2 className="h-5 w-5 text-blue-600" />
                            </div>
                          </div>
                          <div className="ml-4">
                            <div className="text-sm font-medium text-gray-900">
                              {org.name}
                            </div>
                            {org.description && (
                              <div className="text-sm text-gray-500 max-w-xs truncate">
                                {org.description}
                              </div>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center text-sm text-gray-900">
                          <Users className="h-4 w-4 mr-2 text-gray-400" />
                          {org.userCount || 0}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                            org.enabled
                              ? 'bg-green-100 text-green-800'
                              : 'bg-red-100 text-red-800'
                          }`}
                        >
                          {org.enabled ? 'Active' : 'Disabled'}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        <div className="flex items-center">
                          <Calendar className="h-4 w-4 mr-1 text-gray-400" />
                          {new Date(org.createdAt).toLocaleDateString()}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                        <div className="flex items-center justify-end space-x-2">
                          <button
                            onClick={() => handleToggleEnabled(org.id, org.enabled)}
                            className={`p-2 rounded-lg transition-colors ${
                              org.enabled
                                ? 'text-green-600 hover:bg-green-50'
                                : 'text-gray-600 hover:bg-gray-50'
                            }`}
                            title={org.enabled ? 'Disable organization' : 'Enable organization'}
                          >
                            <Power className="h-5 w-5" />
                          </button>
                          <button
                            onClick={() => handleEdit(org)}
                            className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                            title="Edit organization"
                          >
                            <Edit className="h-5 w-5" />
                          </button>
                          <button
                            onClick={() => handleDeleteOrganization(org.id, org.name)}
                            className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                            title="Delete organization"
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
          Showing <span className="font-semibold">{filteredOrganizations.length}</span> of{' '}
          <span className="font-semibold">{organizations.length}</span> organizations
        </div>
      </div>

      {/* Edit Modal */}
      {isEditModalOpen && editingOrg && (
        <EditOrganizationModal
          organization={editingOrg}
          onClose={() => {
            setIsEditModalOpen(false);
            setEditingOrg(null);
          }}
          onSave={handleUpdate}
        />
      )}
    </div>
  );
};

// Edit Organization Modal Component
interface EditOrganizationModalProps {
  organization: Organization;
  onClose: () => void;
  onSave: (orgId: number, data: { name?: string; description?: string }) => void;
}

const EditOrganizationModal = ({ organization, onClose, onSave }: EditOrganizationModalProps) => {
  const [name, setName] = useState(organization.name);
  const [description, setDescription] = useState(organization.description || '');
  const [saving, setSaving] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await onSave(organization.id, { name, description });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg max-w-md w-full p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-semibold text-gray-900">Edit Organization</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
              Organization Name *
            </label>
            <input
              type="text"
              id="name"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          <div>
            <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              placeholder="Optional description"
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
              disabled={saving}
              className="flex-1 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {saving ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AdminOrganizations;
