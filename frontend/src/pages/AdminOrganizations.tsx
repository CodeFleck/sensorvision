import { useState, useEffect } from 'react';
import { Building2, Search, Power, Edit, Trash2, Users, Calendar, Plus, X } from 'lucide-react';
import { Organization } from '../types';
import { apiService } from '../services/api';
import toast from 'react-hot-toast';

export const AdminOrganizations = () => {
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
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
    if (!window.confirm(`Are you sure you want to delete organization "${orgName}"? This action cannot be undone and will fail if the organization has users.`)) {
      return;
    }

    try {
      await apiService.deleteOrganization(orgId);
      toast.success('Organization deleted successfully');
      await fetchOrganizations();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to delete organization');
    }
  };

  const filteredOrganizations = organizations.filter(org =>
    org.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    org.description?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading organizations...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Organization Management</h1>
          <p className="text-gray-600 mt-1">Manage organizations and their settings</p>
        </div>
        <div className="flex items-center space-x-2">
          <span className="text-sm text-gray-600">
            Total Organizations: <span className="font-semibold">{organizations.length}</span>
          </span>
          <span className="text-sm text-gray-600">•</span>
          <span className="text-sm text-gray-600">
            Active: <span className="font-semibold text-green-600">{organizations.filter(o => o.enabled).length}</span>
          </span>
          <span className="text-sm text-gray-600">•</span>
          <span className="text-sm text-gray-600">
            Inactive: <span className="font-semibold text-red-600">{organizations.filter(o => !o.enabled).length}</span>
          </span>
        </div>
      </div>

      {/* Search */}
      <div className="bg-white rounded-lg shadow p-4">
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
      </div>

      {/* Organizations Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredOrganizations.length === 0 ? (
          <div className="col-span-full">
            <div className="bg-white rounded-lg shadow p-12 text-center">
              <Building2 className="h-12 w-12 mx-auto mb-4 text-gray-400" />
              <p className="text-lg font-medium text-gray-900">No organizations found</p>
              <p className="text-sm text-gray-500 mt-1">Try adjusting your search</p>
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
