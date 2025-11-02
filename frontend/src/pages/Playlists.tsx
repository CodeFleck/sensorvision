import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Playlist, Dashboard, PlaylistCreateRequest, TransitionEffect } from '../types';
import { apiService } from '../services/api';

export const Playlists: React.FC = () => {
  const navigate = useNavigate();
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [dashboards, setDashboards] = useState<Dashboard[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingPlaylist, setEditingPlaylist] = useState<Playlist | null>(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [playlistsData, dashboardsData] = await Promise.all([
        apiService.getPlaylists(),
        apiService.getDashboards(),
      ]);
      setPlaylists(playlistsData);
      setDashboards(dashboardsData);
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (confirm('Are you sure you want to delete this playlist?')) {
      try {
        await apiService.deletePlaylist(id);
        await loadData();
      } catch (error) {
        console.error('Failed to delete playlist:', error);
        alert('Failed to delete playlist');
      }
    }
  };

  const handlePlayPlaylist = (playlistId: number) => {
    navigate(`/playlist-player/${playlistId}`);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex justify-between items-start">
        <div>
          <h1 className="text-3xl font-bold text-gray-800">Dashboard Playlists</h1>
          <p className="text-gray-600 mt-1">
            Create playlists to cycle through multiple dashboards automatically on production floor displays
          </p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Create Playlist
        </button>
      </div>

      {/* Playlists Grid */}
      {playlists.length === 0 ? (
        <div className="text-center py-12 bg-gray-50 rounded-lg">
          <svg className="w-16 h-16 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
          <h3 className="text-xl font-semibold text-gray-700 mb-2">No Playlists Yet</h3>
          <p className="text-gray-500">Create a playlist to start cycling through dashboards</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {playlists.map((playlist) => (
            <div key={playlist.id} className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow">
              {/* Playlist Header */}
              <div className="mb-4">
                <h3 className="text-xl font-bold text-gray-800 mb-1">{playlist.name}</h3>
                {playlist.description && (
                  <p className="text-sm text-gray-600">{playlist.description}</p>
                )}
              </div>

              {/* Playlist Info */}
              <div className="space-y-2 text-sm mb-4">
                <div className="flex items-center gap-2 text-gray-600">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                  </svg>
                  <span>{playlist.items.length} dashboard{playlist.items.length !== 1 ? 's' : ''}</span>
                </div>

                <div className="flex items-center gap-2 text-gray-600">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span>
                    {playlist.items.reduce((sum, item) => sum + item.displayDurationSeconds, 0)}s total duration
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  {playlist.loopEnabled && (
                    <span className="px-2 py-1 bg-green-100 text-green-700 text-xs rounded-full">Loop</span>
                  )}
                  {playlist.transitionEffect !== 'none' && (
                    <span className="px-2 py-1 bg-purple-100 text-purple-700 text-xs rounded-full capitalize">
                      {playlist.transitionEffect}
                    </span>
                  )}
                  {playlist.isPublic && (
                    <span className="px-2 py-1 bg-blue-100 text-blue-700 text-xs rounded-full">Public</span>
                  )}
                </div>
              </div>

              {/* Actions */}
              <div className="flex gap-2 border-t pt-4">
                <button
                  onClick={() => handlePlayPlaylist(playlist.id)}
                  disabled={playlist.items.length === 0}
                  className="flex-1 flex items-center justify-center gap-2 px-3 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  title={playlist.items.length === 0 ? "Add dashboards to play" : "Play playlist"}
                >
                  <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M6.3 2.841A1.5 1.5 0 004 4.11V15.89a1.5 1.5 0 002.3 1.269l9.344-5.89a1.5 1.5 0 000-2.538L6.3 2.84z" />
                  </svg>
                  Play
                </button>
                <button
                  onClick={() => setEditingPlaylist(playlist)}
                  className="flex-1 flex items-center justify-center gap-2 px-3 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                  Edit
                </button>
                <button
                  onClick={() => handleDelete(playlist.id)}
                  className="px-3 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors"
                  title="Delete playlist"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create/Edit Playlist Modal */}
      {(showCreateModal || editingPlaylist) && (
        <PlaylistModal
          playlist={editingPlaylist}
          dashboards={dashboards}
          onClose={() => {
            setShowCreateModal(false);
            setEditingPlaylist(null);
          }}
          onSuccess={() => {
            loadData();
            setShowCreateModal(false);
            setEditingPlaylist(null);
          }}
        />
      )}
    </div>
  );
};

// Playlist Create/Edit Modal Component
interface PlaylistModalProps {
  playlist?: Playlist | null;
  dashboards: Dashboard[];
  onClose: () => void;
  onSuccess: () => void;
}

const PlaylistModal: React.FC<PlaylistModalProps> = ({ playlist, dashboards, onClose, onSuccess }) => {
  const [formData, setFormData] = useState<PlaylistCreateRequest>({
    name: playlist?.name || '',
    description: playlist?.description || '',
    loopEnabled: playlist?.loopEnabled ?? true,
    transitionEffect: playlist?.transitionEffect || 'fade',
    isPublic: playlist?.isPublic || false,
  });
  const [selectedDashboards, setSelectedDashboards] = useState<Array<{ dashboardId: number; duration: number }>>(
    playlist?.items.map(item => ({ dashboardId: item.dashboardId, duration: item.displayDurationSeconds })) || []
  );
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const data = {
        ...formData,
        items: selectedDashboards.map((item, index) => ({
          dashboardId: item.dashboardId,
          position: index,
          displayDurationSeconds: item.duration,
        })),
      };

      if (playlist) {
        await apiService.updatePlaylist(playlist.id, data);
      } else {
        await apiService.createPlaylist(data);
      }

      onSuccess();
    } catch (error) {
      console.error('Failed to save playlist:', error);
      alert('Failed to save playlist');
    } finally {
      setLoading(false);
    }
  };

  const addDashboard = (dashboardId: number) => {
    if (!selectedDashboards.find(d => d.dashboardId === dashboardId)) {
      setSelectedDashboards([...selectedDashboards, { dashboardId, duration: 30 }]);
    }
  };

  const removeDashboard = (dashboardId: number) => {
    setSelectedDashboards(selectedDashboards.filter(d => d.dashboardId !== dashboardId));
  };

  const updateDuration = (dashboardId: number, duration: number) => {
    setSelectedDashboards(selectedDashboards.map(d =>
      d.dashboardId === dashboardId ? { ...d, duration } : d
    ));
  };

  const moveDashboard = (index: number, direction: 'up' | 'down') => {
    const newIndex = direction === 'up' ? index - 1 : index + 1;
    if (newIndex < 0 || newIndex >= selectedDashboards.length) return;

    const newDashboards = [...selectedDashboards];
    [newDashboards[index], newDashboards[newIndex]] = [newDashboards[newIndex], newDashboards[index]];
    setSelectedDashboards(newDashboards);
  };

  const getDashboardName = (dashboardId: number) => {
    return dashboards.find(d => d.id === dashboardId)?.name || 'Unknown Dashboard';
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg w-full max-w-4xl max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b px-6 py-4 flex justify-between items-center">
          <h2 className="text-2xl font-bold text-gray-800">
            {playlist ? 'Edit Playlist' : 'Create Playlist'}
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {/* Basic Info */}
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Playlist Name *
              </label>
              <input
                type="text"
                required
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="e.g., Production Floor Display"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <textarea
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                rows={2}
                placeholder="Optional description"
              />
            </div>
          </div>

          {/* Settings */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Transition Effect
              </label>
              <select
                value={formData.transitionEffect}
                onChange={(e) => setFormData({ ...formData, transitionEffect: e.target.value as TransitionEffect })}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="fade">Fade</option>
                <option value="slide">Slide</option>
                <option value="none">None</option>
              </select>
            </div>

            <div className="flex items-center">
              <label className="flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.loopEnabled}
                  onChange={(e) => setFormData({ ...formData, loopEnabled: e.target.checked })}
                  className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                />
                <span className="ml-2 text-sm font-medium text-gray-700">Loop playlist</span>
              </label>
            </div>

            <div className="flex items-center">
              <label className="flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.isPublic}
                  onChange={(e) => setFormData({ ...formData, isPublic: e.target.checked })}
                  className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
                />
                <span className="ml-2 text-sm font-medium text-gray-700">Public sharing</span>
              </label>
            </div>
          </div>

          {/* Dashboard Selection */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Add Dashboards
            </label>
            <div className="flex gap-2 flex-wrap">
              {dashboards.map((dashboard) => (
                <button
                  key={dashboard.id}
                  type="button"
                  onClick={() => addDashboard(dashboard.id)}
                  disabled={selectedDashboards.some(d => d.dashboardId === dashboard.id)}
                  className="px-3 py-1.5 bg-blue-100 text-blue-700 rounded-md hover:bg-blue-200 transition-colors disabled:opacity-50 disabled:cursor-not-allowed text-sm"
                >
                  + {dashboard.name}
                </button>
              ))}
            </div>
          </div>

          {/* Selected Dashboards */}
          {selectedDashboards.length > 0 && (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Playlist Order ({selectedDashboards.length} dashboard{selectedDashboards.length !== 1 ? 's' : ''})
              </label>
              <div className="space-y-2">
                {selectedDashboards.map((item, index) => (
                  <div key={item.dashboardId} className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
                    <div className="flex flex-col gap-1">
                      <button
                        type="button"
                        onClick={() => moveDashboard(index, 'up')}
                        disabled={index === 0}
                        className="text-gray-400 hover:text-gray-600 disabled:opacity-30"
                      >
                        <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                          <path fillRule="evenodd" d="M14.707 12.707a1 1 0 01-1.414 0L10 9.414l-3.293 3.293a1 1 0 01-1.414-1.414l4-4a1 1 0 011.414 0l4 4a1 1 0 010 1.414z" clipRule="evenodd" />
                        </svg>
                      </button>
                      <button
                        type="button"
                        onClick={() => moveDashboard(index, 'down')}
                        disabled={index === selectedDashboards.length - 1}
                        className="text-gray-400 hover:text-gray-600 disabled:opacity-30"
                      >
                        <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                          <path fillRule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clipRule="evenodd" />
                        </svg>
                      </button>
                    </div>
                    <div className="flex-1">
                      <div className="font-medium text-gray-800">{getDashboardName(item.dashboardId)}</div>
                    </div>
                    <div className="flex items-center gap-2">
                      <input
                        type="number"
                        min="5"
                        max="300"
                        value={item.duration}
                        onChange={(e) => updateDuration(item.dashboardId, parseInt(e.target.value))}
                        className="w-20 px-2 py-1 border border-gray-300 rounded text-sm"
                      />
                      <span className="text-sm text-gray-600">seconds</span>
                    </div>
                    <button
                      type="button"
                      onClick={() => removeDashboard(item.dashboardId)}
                      className="text-red-600 hover:text-red-700"
                    >
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Actions */}
          <div className="flex justify-end gap-3 pt-4 border-t">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-gray-700 bg-gray-200 rounded-md hover:bg-gray-300"
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:opacity-50"
              disabled={loading || selectedDashboards.length === 0}
            >
              {loading ? 'Saving...' : playlist ? 'Update Playlist' : 'Create Playlist'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
