import { useState, useEffect } from 'react';
import { Trash2, Search, RefreshCw, Clock, User, Monitor, Building2, AlertTriangle, RotateCcw } from 'lucide-react';
import { apiService, TrashItem, TrashStats } from '../services/api';
import toast from 'react-hot-toast';

type EntityTypeFilter = 'all' | 'USER' | 'DEVICE' | 'ORGANIZATION';

export const AdminTrash = () => {
  const [trashItems, setTrashItems] = useState<TrashItem[]>([]);
  const [stats, setStats] = useState<TrashStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState<EntityTypeFilter>('all');
  const [restoringId, setRestoringId] = useState<number | null>(null);

  useEffect(() => {
    fetchTrashItems();
    fetchStats();
  }, []);

  const fetchTrashItems = async () => {
    try {
      setLoading(true);
      const data = await apiService.getAllTrashItems();
      setTrashItems(data);
    } catch (error) {
      console.error('Failed to fetch trash items:', error);
      toast.error('Failed to load trash items');
    } finally {
      setLoading(false);
    }
  };

  const fetchStats = async () => {
    try {
      const data = await apiService.getTrashStats();
      setStats(data);
    } catch (error) {
      console.error('Failed to fetch trash stats:', error);
    }
  };

  const handleRestore = async (item: TrashItem) => {
    try {
      setRestoringId(item.id);
      await apiService.restoreTrashItem(item.id);
      toast.success(`${item.entityType.toLowerCase()} "${item.entityName}" restored successfully`);
      await fetchTrashItems();
      await fetchStats();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to restore item');
    } finally {
      setRestoringId(null);
    }
  };

  const getEntityIcon = (type: string) => {
    switch (type) {
      case 'USER':
        return <User className="h-5 w-5" />;
      case 'DEVICE':
        return <Monitor className="h-5 w-5" />;
      case 'ORGANIZATION':
        return <Building2 className="h-5 w-5" />;
      default:
        return <Trash2 className="h-5 w-5" />;
    }
  };

  const getEntityColor = (type: string) => {
    switch (type) {
      case 'USER':
        return 'bg-blue-100 text-blue-800';
      case 'DEVICE':
        return 'bg-green-100 text-green-800';
      case 'ORGANIZATION':
        return 'bg-purple-100 text-purple-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getDaysRemainingColor = (days: number) => {
    if (days <= 3) return 'text-red-600 bg-red-50';
    if (days <= 7) return 'text-orange-600 bg-orange-50';
    return 'text-gray-600 bg-gray-50';
  };

  const filteredItems = trashItems.filter((item) => {
    const matchesSearch =
      item.entityName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      item.deletedBy.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (item.deletionReason && item.deletionReason.toLowerCase().includes(searchTerm.toLowerCase()));

    const matchesType = filterType === 'all' || item.entityType === filterType;

    return matchesSearch && matchesType;
  });

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading trash items...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Trash</h1>
          <p className="text-gray-600 mt-1">
            Deleted items are kept for 30 days before permanent deletion
          </p>
        </div>
        <button
          onClick={() => {
            fetchTrashItems();
            fetchStats();
          }}
          className="flex items-center px-4 py-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
        >
          <RefreshCw className="h-4 w-4 mr-2" />
          Refresh
        </button>
      </div>

      {/* Stats Cards */}
      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-white rounded-lg shadow p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Total in Trash</p>
                <p className="text-2xl font-bold text-gray-900">{stats.totalItems}</p>
              </div>
              <div className="p-3 bg-gray-100 rounded-full">
                <Trash2 className="h-6 w-6 text-gray-600" />
              </div>
            </div>
          </div>
          <div className="bg-white rounded-lg shadow p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Deleted Users</p>
                <p className="text-2xl font-bold text-blue-600">{stats.users}</p>
              </div>
              <div className="p-3 bg-blue-100 rounded-full">
                <User className="h-6 w-6 text-blue-600" />
              </div>
            </div>
          </div>
          <div className="bg-white rounded-lg shadow p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Deleted Devices</p>
                <p className="text-2xl font-bold text-green-600">{stats.devices}</p>
              </div>
              <div className="p-3 bg-green-100 rounded-full">
                <Monitor className="h-6 w-6 text-green-600" />
              </div>
            </div>
          </div>
          <div className="bg-white rounded-lg shadow p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-600">Deleted Organizations</p>
                <p className="text-2xl font-bold text-purple-600">{stats.organizations}</p>
              </div>
              <div className="p-3 bg-purple-100 rounded-full">
                <Building2 className="h-6 w-6 text-purple-600" />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Filters */}
      <div className="bg-white rounded-lg shadow p-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search by name, deleted by, or reason..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          <div>
            <select
              value={filterType}
              onChange={(e) => setFilterType(e.target.value as EntityTypeFilter)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="all">All Types</option>
              <option value="USER">Users</option>
              <option value="DEVICE">Devices</option>
              <option value="ORGANIZATION">Organizations</option>
            </select>
          </div>
        </div>
      </div>

      {/* Warning Banner */}
      <div className="bg-amber-50 border border-amber-200 rounded-lg p-4">
        <div className="flex items-start">
          <AlertTriangle className="h-5 w-5 text-amber-500 mt-0.5 mr-3 flex-shrink-0" />
          <div>
            <h3 className="text-sm font-medium text-amber-800">Automatic Permanent Deletion</h3>
            <p className="text-sm text-amber-700 mt-1">
              Items in trash are automatically and permanently deleted after 30 days.
              This action cannot be undone. Restore items you wish to keep before they expire.
            </p>
          </div>
        </div>
      </div>

      {/* Trash Items Table */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Item
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Type
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Deleted By
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Deleted At
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Time Remaining
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {filteredItems.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-12 text-center text-gray-500">
                    <Trash2 className="h-12 w-12 mx-auto mb-4 text-gray-400" />
                    <p className="text-lg font-medium">Trash is empty</p>
                    <p className="text-sm mt-1">Deleted items will appear here</p>
                  </td>
                </tr>
              ) : (
                filteredItems.map((item) => (
                  <tr key={item.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        <div className={`flex-shrink-0 h-10 w-10 rounded-full flex items-center justify-center ${getEntityColor(item.entityType)}`}>
                          {getEntityIcon(item.entityType)}
                        </div>
                        <div className="ml-4">
                          <div className="text-sm font-medium text-gray-900">
                            {item.entityName}
                          </div>
                          <div className="text-sm text-gray-500">
                            ID: {item.entityId}
                          </div>
                          {item.deletionReason && (
                            <div className="text-xs text-gray-400 mt-1">
                              Reason: {item.deletionReason}
                            </div>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getEntityColor(item.entityType)}`}>
                        {item.entityType}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {item.deletedBy}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {new Date(item.deletedAt).toLocaleString()}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium ${getDaysRemainingColor(item.daysRemaining)}`}>
                        <Clock className="h-3 w-3 mr-1" />
                        {item.daysRemaining} days left
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right">
                      <button
                        onClick={() => handleRestore(item)}
                        disabled={restoringId === item.id}
                        className="inline-flex items-center px-3 py-1.5 border border-green-300 text-green-700 rounded-lg hover:bg-green-50 transition-colors disabled:opacity-50"
                      >
                        {restoringId === item.id ? (
                          <>
                            <RefreshCw className="h-4 w-4 mr-1 animate-spin" />
                            Restoring...
                          </>
                        ) : (
                          <>
                            <RotateCcw className="h-4 w-4 mr-1" />
                            Restore
                          </>
                        )}
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
