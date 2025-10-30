import { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { UserAvatar } from '../components/UserAvatar';
import { AvatarUploadModal } from '../components/AvatarUploadModal';
import {
  User,
  Mail,
  Building2,
  Calendar,
  Shield,
  Activity,
  Cpu,
  AlertTriangle,
  CheckCircle2,
  TrendingUp,
  BarChart3,
  Edit2,
  Camera
} from 'lucide-react';
import { apiService } from '../services/api';

interface UserStats {
  totalDevices: number;
  onlineDevices: number;
  totalAlerts: number;
  unacknowledgedAlerts: number;
  totalDataPoints: number;
  lastActivity: string;
  memberSince: string;
}

export const Profile = () => {
  const { user, refreshUser, isAdmin } = useAuth();
  const [isAvatarModalOpen, setIsAvatarModalOpen] = useState(false);
  const [stats, setStats] = useState<UserStats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchUserStats();
  }, []);

  const fetchUserStats = async () => {
    try {
      setLoading(true);
      const [devicesResponse, alertsResponse] = await Promise.all([
        apiService.getDevices(),
        apiService.getAlerts()
      ]);

      const devices = devicesResponse.data || [];
      const alerts = alertsResponse.data || [];

      const onlineDevices = devices.filter((d: any) => d.status === 'ONLINE').length;
      const unacknowledgedAlerts = alerts.filter((a: any) => !a.acknowledged).length;

      setStats({
        totalDevices: devices.length,
        onlineDevices,
        totalAlerts: alerts.length,
        unacknowledgedAlerts,
        totalDataPoints: 0, // TODO: Add API endpoint for this
        lastActivity: new Date().toISOString(),
        memberSince: user?.createdAt || new Date().toISOString()
      });
    } catch (error) {
      console.error('Error fetching user stats:', error);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  const getTimeAgo = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffInMs = now.getTime() - date.getTime();
    const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24));

    if (diffInDays === 0) return 'Today';
    if (diffInDays === 1) return 'Yesterday';
    if (diffInDays < 30) return `${diffInDays} days ago`;
    if (diffInDays < 365) {
      const months = Math.floor(diffInDays / 30);
      return `${months} ${months === 1 ? 'month' : 'months'} ago`;
    }
    const years = Math.floor(diffInDays / 365);
    return `${years} ${years === 1 ? 'year' : 'years'} ago`;
  };

  const getHealthColor = (online: number, total: number) => {
    if (total === 0) return 'text-gray-400';
    const percentage = (online / total) * 100;
    if (percentage >= 80) return 'text-green-600';
    if (percentage >= 50) return 'text-yellow-600';
    return 'text-red-600';
  };

  if (!user) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-gray-500">Loading...</div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      {/* Header */}
      <div className="bg-gradient-to-r from-cyan-500 to-blue-600 rounded-lg shadow-lg overflow-hidden">
        <div className="px-8 py-12">
          <div className="flex items-start gap-6">
            <div className="relative group">
              <UserAvatar user={user} size="xl" />
              <button
                onClick={() => setIsAvatarModalOpen(true)}
                className="absolute inset-0 flex items-center justify-center bg-black/50 rounded-full opacity-0 group-hover:opacity-100 transition-opacity"
              >
                <Camera className="h-8 w-8 text-white" />
              </button>
            </div>
            <div className="flex-1 text-white">
              <div className="flex items-center gap-3 mb-2">
                <h1 className="text-3xl font-bold">{user.username}</h1>
                {isAdmin && (
                  <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-500 text-white text-sm font-semibold">
                    <Shield className="h-4 w-4" />
                    Admin
                  </span>
                )}
              </div>
              <div className="flex flex-wrap gap-4 text-white/90">
                <div className="flex items-center gap-2">
                  <Mail className="h-4 w-4" />
                  <span className="text-sm">{user.email}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Building2 className="h-4 w-4" />
                  <span className="text-sm">{user.organizationName || 'No Organization'}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Calendar className="h-4 w-4" />
                  <span className="text-sm">Joined {getTimeAgo(stats?.memberSince || user.createdAt || '')}</span>
                </div>
              </div>
            </div>
            <button
              onClick={() => setIsAvatarModalOpen(true)}
              className="px-4 py-2 bg-white/20 hover:bg-white/30 rounded-lg text-white font-medium transition-colors flex items-center gap-2"
            >
              <Edit2 className="h-4 w-4" />
              Edit Avatar
            </button>
          </div>
        </div>
      </div>

      {/* Stats Grid */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 animate-pulse">
              <div className="h-12 w-12 bg-gray-200 dark:bg-gray-700 rounded-lg mb-4" />
              <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-24 mb-2" />
              <div className="h-8 bg-gray-200 dark:bg-gray-700 rounded w-16" />
            </div>
          ))}
        </div>
      ) : stats && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {/* Total Devices */}
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow">
            <div className="flex items-center gap-4">
              <div className="p-3 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
                <Cpu className="h-6 w-6 text-blue-600 dark:text-blue-400" />
              </div>
              <div>
                <p className="text-sm text-gray-600 dark:text-gray-400 font-medium">Total Devices</p>
                <p className="text-2xl font-bold text-gray-900 dark:text-white">{stats.totalDevices}</p>
              </div>
            </div>
          </div>

          {/* Online Devices */}
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow">
            <div className="flex items-center gap-4">
              <div className="p-3 bg-green-100 dark:bg-green-900/30 rounded-lg">
                <Activity className={`h-6 w-6 ${getHealthColor(stats.onlineDevices, stats.totalDevices)}`} />
              </div>
              <div>
                <p className="text-sm text-gray-600 dark:text-gray-400 font-medium">Online Devices</p>
                <p className="text-2xl font-bold text-gray-900 dark:text-white">
                  {stats.onlineDevices}
                  <span className="text-sm text-gray-500 ml-2">
                    / {stats.totalDevices}
                  </span>
                </p>
              </div>
            </div>
          </div>

          {/* Alerts */}
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow">
            <div className="flex items-center gap-4">
              <div className="p-3 bg-yellow-100 dark:bg-yellow-900/30 rounded-lg">
                <AlertTriangle className="h-6 w-6 text-yellow-600 dark:text-yellow-400" />
              </div>
              <div>
                <p className="text-sm text-gray-600 dark:text-gray-400 font-medium">Active Alerts</p>
                <p className="text-2xl font-bold text-gray-900 dark:text-white">
                  {stats.unacknowledgedAlerts}
                  {stats.unacknowledgedAlerts > 0 && (
                    <span className="text-sm text-yellow-600 dark:text-yellow-400 ml-2">
                      needs attention
                    </span>
                  )}
                </p>
              </div>
            </div>
          </div>

          {/* All-Time Stats */}
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow">
            <div className="flex items-center gap-4">
              <div className="p-3 bg-purple-100 dark:bg-purple-900/30 rounded-lg">
                <TrendingUp className="h-6 w-6 text-purple-600 dark:text-purple-400" />
              </div>
              <div>
                <p className="text-sm text-gray-600 dark:text-gray-400 font-medium">Total Alerts</p>
                <p className="text-2xl font-bold text-gray-900 dark:text-white">{stats.totalAlerts}</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Additional Info Cards */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Account Information */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
          <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
            <User className="h-5 w-5 text-cyan-600" />
            Account Information
          </h2>
          <div className="space-y-4">
            <div className="flex justify-between items-center py-3 border-b border-gray-200 dark:border-gray-700">
              <span className="text-sm font-medium text-gray-600 dark:text-gray-400">Username</span>
              <span className="text-sm text-gray-900 dark:text-white font-semibold">{user.username}</span>
            </div>
            <div className="flex justify-between items-center py-3 border-b border-gray-200 dark:border-gray-700">
              <span className="text-sm font-medium text-gray-600 dark:text-gray-400">Email</span>
              <span className="text-sm text-gray-900 dark:text-white">{user.email}</span>
            </div>
            <div className="flex justify-between items-center py-3 border-b border-gray-200 dark:border-gray-700">
              <span className="text-sm font-medium text-gray-600 dark:text-gray-400">Organization</span>
              <span className="text-sm text-gray-900 dark:text-white">{user.organizationName || 'None'}</span>
            </div>
            <div className="flex justify-between items-center py-3 border-b border-gray-200 dark:border-gray-700">
              <span className="text-sm font-medium text-gray-600 dark:text-gray-400">Role</span>
              <span className="text-sm">
                {isAdmin ? (
                  <span className="inline-flex items-center gap-1 px-2 py-1 rounded bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400 font-semibold">
                    <Shield className="h-3 w-3" />
                    Admin
                  </span>
                ) : (
                  <span className="text-gray-900 dark:text-white">User</span>
                )}
              </span>
            </div>
            <div className="flex justify-between items-center py-3">
              <span className="text-sm font-medium text-gray-600 dark:text-gray-400">Member Since</span>
              <span className="text-sm text-gray-900 dark:text-white">{formatDate(stats?.memberSince || user.createdAt || '')}</span>
            </div>
          </div>
        </div>

        {/* Activity Summary */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
          <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
            <BarChart3 className="h-5 w-5 text-cyan-600" />
            Activity Summary
          </h2>
          <div className="space-y-4">
            <div className="p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
              <div className="flex items-center gap-3 mb-2">
                <Cpu className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                <span className="font-semibold text-gray-900 dark:text-white">Devices</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600 dark:text-gray-400">Online</span>
                <span className="font-bold text-green-600">{stats?.onlineDevices || 0}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600 dark:text-gray-400">Offline</span>
                <span className="font-bold text-gray-500">{(stats?.totalDevices || 0) - (stats?.onlineDevices || 0)}</span>
              </div>
            </div>

            <div className="p-4 bg-yellow-50 dark:bg-yellow-900/20 rounded-lg">
              <div className="flex items-center gap-3 mb-2">
                <AlertTriangle className="h-5 w-5 text-yellow-600 dark:text-yellow-400" />
                <span className="font-semibold text-gray-900 dark:text-white">Alerts</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600 dark:text-gray-400">Pending</span>
                <span className="font-bold text-yellow-600">{stats?.unacknowledgedAlerts || 0}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-600 dark:text-gray-400">Acknowledged</span>
                <span className="font-bold text-green-600">{(stats?.totalAlerts || 0) - (stats?.unacknowledgedAlerts || 0)}</span>
              </div>
            </div>

            <div className="p-4 bg-green-50 dark:bg-green-900/20 rounded-lg">
              <div className="flex items-center gap-3">
                <CheckCircle2 className="h-5 w-5 text-green-600 dark:text-green-400" />
                <div className="flex-1">
                  <div className="font-semibold text-gray-900 dark:text-white">System Health</div>
                  <div className="text-xs text-gray-600 dark:text-gray-400">
                    {stats && stats.totalDevices > 0 ? (
                      <>
                        {Math.round((stats.onlineDevices / stats.totalDevices) * 100)}% devices online
                      </>
                    ) : (
                      'No devices yet'
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Avatar Upload Modal */}
      {user && (
        <AvatarUploadModal
          isOpen={isAvatarModalOpen}
          onClose={() => setIsAvatarModalOpen(false)}
          user={user}
          onSuccess={() => {
            refreshUser();
          }}
        />
      )}
    </div>
  );
};

export default Profile;
