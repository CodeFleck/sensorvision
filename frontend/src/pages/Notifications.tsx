import React, { useState, useEffect } from 'react';
import { apiService } from '../services/api';
import {
  NotificationPreference,
  NotificationPreferenceRequest,
  NotificationLog,
  NotificationStats,
  NotificationChannel,
  AlertSeverity
} from '../types';
import {
  BellIcon,
  EnvelopeIcon,
  DevicePhoneMobileIcon,
  GlobeAltIcon,
  CheckCircleIcon,
  XCircleIcon,
  ClockIcon
} from '@heroicons/react/24/outline';

export const Notifications: React.FC = () => {
  const [preferences, setPreferences] = useState<NotificationPreference[]>([]);
  const [logs, setLogs] = useState<NotificationLog[]>([]);
  const [stats, setStats] = useState<NotificationStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Form state
  const [formChannel, setFormChannel] = useState<NotificationChannel>('EMAIL');
  const [formEnabled, setFormEnabled] = useState(true);
  const [formDestination, setFormDestination] = useState('');
  const [formMinSeverity, setFormMinSeverity] = useState<AlertSeverity>('LOW');
  const [formImmediate, setFormImmediate] = useState(true);
  const [formDigestInterval, setFormDigestInterval] = useState<number | ''>('');

  useEffect(() => {
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [prefsData, logsData, statsData] = await Promise.all([
        apiService.getNotificationPreferences(),
        apiService.getNotificationLogs(page, 20),
        apiService.getNotificationStats()
      ]);
      setPreferences(prefsData);
      setLogs(logsData.content);
      setTotalPages(logsData.totalPages);
      setStats(statsData);
      setError('');
    } catch (err) {
      setError('Failed to fetch notification data');
      console.error('Error fetching notifications:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSavePreference = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const request: NotificationPreferenceRequest = {
        channel: formChannel,
        enabled: formEnabled,
        destination: formDestination || undefined,
        minSeverity: formMinSeverity,
        immediate: formImmediate,
        digestIntervalMinutes: formDigestInterval ? Number(formDigestInterval) : undefined
      };

      await apiService.saveNotificationPreference(request);
      setShowAddModal(false);
      resetForm();
      fetchData();
    } catch (err) {
      setError('Failed to save notification preference');
      console.error('Error saving preference:', err);
    }
  };

  const handleDeletePreference = async (channel: NotificationChannel) => {
    if (!confirm(`Are you sure you want to delete the ${channel} notification preference?`)) {
      return;
    }

    try {
      await apiService.deleteNotificationPreference(channel);
      fetchData();
    } catch (err) {
      setError('Failed to delete notification preference');
      console.error('Error deleting preference:', err);
    }
  };

  const resetForm = () => {
    setFormChannel('EMAIL');
    setFormEnabled(true);
    setFormDestination('');
    setFormMinSeverity('LOW');
    setFormImmediate(true);
    setFormDigestInterval('');
  };

  const getChannelIcon = (channel: NotificationChannel) => {
    switch (channel) {
      case 'EMAIL':
        return <EnvelopeIcon className="h-5 w-5 text-blue-500" />;
      case 'SMS':
        return <DevicePhoneMobileIcon className="h-5 w-5 text-green-500" />;
      case 'WEBHOOK':
        return <GlobeAltIcon className="h-5 w-5 text-purple-500" />;
      case 'IN_APP':
        return <BellIcon className="h-5 w-5 text-orange-500" />;
    }
  };

  const getSeverityBadgeClass = (severity: AlertSeverity) => {
    switch (severity) {
      case 'LOW':
        return 'bg-blue-100 text-blue-800';
      case 'MEDIUM':
        return 'bg-yellow-100 text-yellow-800';
      case 'HIGH':
        return 'bg-orange-100 text-orange-800';
      case 'CRITICAL':
        return 'bg-red-100 text-red-800';
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'SENT':
        return <CheckCircleIcon className="h-5 w-5 text-green-500" />;
      case 'FAILED':
        return <XCircleIcon className="h-5 w-5 text-red-500" />;
      default:
        return <ClockIcon className="h-5 w-5 text-gray-500" />;
    }
  };

  const formatTimestamp = (timestamp: string) => {
    const date = new Date(timestamp);
    return date.toLocaleString();
  };

  const channels: NotificationChannel[] = ['EMAIL', 'SMS', 'WEBHOOK', 'IN_APP'];
  const severities: AlertSeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  return (
    <div className="space-y-6">
      <div className="sm:flex sm:items-center sm:justify-between">
        <h1 className="text-2xl font-semibold text-gray-900">Notification Settings</h1>
        <button
          onClick={() => setShowAddModal(true)}
          className="mt-4 sm:mt-0 inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700"
        >
          Add Notification Channel
        </button>
      </div>

      {error && (
        <div className="rounded-md bg-red-50 p-4">
          <p className="text-sm text-red-800">{error}</p>
        </div>
      )}

      {/* Statistics */}
      {stats && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <BellIcon className="h-6 w-6 text-gray-400" />
                </div>
                <div className="ml-5 w-0 flex-1">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">Total Notifications</dt>
                    <dd className="text-lg font-semibold text-gray-900">{stats.total}</dd>
                  </dl>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <CheckCircleIcon className="h-6 w-6 text-green-400" />
                </div>
                <div className="ml-5 w-0 flex-1">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">Sent Successfully</dt>
                    <dd className="text-lg font-semibold text-gray-900">{stats.sent}</dd>
                  </dl>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-white overflow-hidden shadow rounded-lg">
            <div className="p-5">
              <div className="flex items-center">
                <div className="flex-shrink-0">
                  <XCircleIcon className="h-6 w-6 text-red-400" />
                </div>
                <div className="ml-5 w-0 flex-1">
                  <dl>
                    <dt className="text-sm font-medium text-gray-500 truncate">Failed</dt>
                    <dd className="text-lg font-semibold text-gray-900">{stats.failed}</dd>
                  </dl>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Notification Preferences */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-4 py-5 sm:p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">Active Channels</h2>
          {loading ? (
            <div className="flex justify-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
            </div>
          ) : preferences.length === 0 ? (
            <p className="text-sm text-gray-500">No notification channels configured</p>
          ) : (
            <ul className="divide-y divide-gray-200">
              {preferences.map((pref) => (
                <li key={pref.id} className="py-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      {getChannelIcon(pref.channel)}
                      <div>
                        <p className="text-sm font-medium text-gray-900">{pref.channel}</p>
                        <div className="flex items-center space-x-2 mt-1">
                          {pref.destination && (
                            <span className="text-xs text-gray-500">{pref.destination}</span>
                          )}
                          <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${getSeverityBadgeClass(pref.minSeverity)}`}>
                            Min: {pref.minSeverity}
                          </span>
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-800">
                            {pref.immediate ? 'Immediate' : `Digest (${pref.digestIntervalMinutes}m)`}
                          </span>
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center space-x-2">
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${pref.enabled ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}`}>
                        {pref.enabled ? 'Enabled' : 'Disabled'}
                      </span>
                      <button
                        onClick={() => handleDeletePreference(pref.channel)}
                        className="text-red-600 hover:text-red-900 text-sm font-medium"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {/* Notification History */}
      <div className="bg-white shadow rounded-lg">
        <div className="px-4 py-5 sm:p-6">
          <h2 className="text-lg font-medium text-gray-900 mb-4">Notification History</h2>
          {logs.length === 0 ? (
            <p className="text-sm text-gray-500">No notification history</p>
          ) : (
            <>
              <ul className="divide-y divide-gray-200">
                {logs.map((log) => (
                  <li key={log.id} className="py-3">
                    <div className="flex items-start space-x-3">
                      {getStatusIcon(log.status)}
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-2">
                            {getChannelIcon(log.channel)}
                            <p className="text-sm font-medium text-gray-900">{log.subject || log.channel}</p>
                          </div>
                          <p className="text-xs text-gray-500">{formatTimestamp(log.createdAt)}</p>
                        </div>
                        <p className="mt-1 text-sm text-gray-600">{log.message}</p>
                        <p className="mt-1 text-xs text-gray-500">To: {log.destination}</p>
                        {log.errorMessage && (
                          <p className="mt-1 text-xs text-red-600">Error: {log.errorMessage}</p>
                        )}
                      </div>
                    </div>
                  </li>
                ))}
              </ul>

              {/* Pagination */}
              {totalPages > 1 && (
                <div className="mt-4 flex items-center justify-between border-t border-gray-200 pt-3">
                  <button
                    onClick={() => setPage(Math.max(0, page - 1))}
                    disabled={page === 0}
                    className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
                  >
                    Previous
                  </button>
                  <p className="text-sm text-gray-700">
                    Page {page + 1} of {totalPages}
                  </p>
                  <button
                    onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                    disabled={page >= totalPages - 1}
                    className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
                  >
                    Next
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* Add/Edit Modal */}
      {showAddModal && (
        <div className="fixed z-10 inset-0 overflow-y-auto">
          <div className="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
            <div className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" onClick={() => setShowAddModal(false)}></div>

            <div className="inline-block align-bottom bg-white rounded-lg px-4 pt-5 pb-4 text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full sm:p-6">
              <form onSubmit={handleSavePreference}>
                <h3 className="text-lg font-medium text-gray-900 mb-4">Add Notification Channel</h3>

                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700">Channel</label>
                    <select
                      value={formChannel}
                      onChange={(e) => setFormChannel(e.target.value as NotificationChannel)}
                      className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm rounded-md"
                      required
                    >
                      {channels.map((channel) => (
                        <option key={channel} value={channel}>{channel}</option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700">Destination {(formChannel === 'EMAIL' || formChannel === 'SMS') && '*'}</label>
                    <input
                      type={formChannel === 'EMAIL' ? 'email' : 'text'}
                      value={formDestination}
                      onChange={(e) => setFormDestination(e.target.value)}
                      placeholder={formChannel === 'EMAIL' ? 'email@example.com' : formChannel === 'SMS' ? '+1234567890' : 'https://webhook.url'}
                      className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                      required={formChannel === 'SMS'}
                    />
                    <p className="mt-1 text-xs text-gray-500">Leave empty for EMAIL to use your account email</p>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700">Minimum Severity</label>
                    <select
                      value={formMinSeverity}
                      onChange={(e) => setFormMinSeverity(e.target.value as AlertSeverity)}
                      className="mt-1 block w-full pl-3 pr-10 py-2 text-base border-gray-300 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm rounded-md"
                    >
                      {severities.map((severity) => (
                        <option key={severity} value={severity}>{severity}</option>
                      ))}
                    </select>
                  </div>

                  <div className="flex items-center">
                    <input
                      type="checkbox"
                      checked={formEnabled}
                      onChange={(e) => setFormEnabled(e.target.checked)}
                      className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                    />
                    <label className="ml-2 block text-sm text-gray-900">Enabled</label>
                  </div>

                  <div className="flex items-center">
                    <input
                      type="checkbox"
                      checked={formImmediate}
                      onChange={(e) => setFormImmediate(e.target.checked)}
                      className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                    />
                    <label className="ml-2 block text-sm text-gray-900">Send Immediately</label>
                  </div>

                  {!formImmediate && (
                    <div>
                      <label className="block text-sm font-medium text-gray-700">Digest Interval (minutes)</label>
                      <input
                        type="number"
                        value={formDigestInterval}
                        onChange={(e) => setFormDigestInterval(e.target.value ? Number(e.target.value) : '')}
                        min="15"
                        step="15"
                        className="mt-1 block w-full border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                        required={!formImmediate}
                      />
                    </div>
                  )}
                </div>

                <div className="mt-5 sm:mt-6 sm:grid sm:grid-cols-2 sm:gap-3 sm:grid-flow-row-dense">
                  <button
                    type="submit"
                    className="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-blue-600 text-base font-medium text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 sm:col-start-2 sm:text-sm"
                  >
                    Save
                  </button>
                  <button
                    type="button"
                    onClick={() => { setShowAddModal(false); resetForm(); }}
                    className="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 sm:mt-0 sm:col-start-1 sm:text-sm"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
