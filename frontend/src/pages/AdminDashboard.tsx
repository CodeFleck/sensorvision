import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Users,
  Cpu,
  Building2,
  AlertTriangle,
  TicketIcon,
  TrendingUp,
  RefreshCw,
  Activity,
  Database,
  Wifi,
  CheckCircle,
  AlertCircle,
} from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import axios from 'axios';
import toast from 'react-hot-toast';

interface SystemOverview {
  totalUsers: number;
  totalDevices: number;
  totalOrganizations: number;
  activeAlerts: number;
  pendingSupportTickets: number;
}

interface RecentActivity {
  type: string;
  description: string;
  timestamp: string;
  username: string;
  entityId: number;
}

interface SystemHealth {
  dataIngestionRate: number;
  storageUsedMb: number;
  activeConnections: number;
  status: string;
}

interface DataPoint {
  label: string;
  value: number;
}

interface ChartData {
  userGrowth: DataPoint[];
  deviceActivity: DataPoint[];
  ticketVolume: DataPoint[];
}

interface AdminDashboardStats {
  systemOverview: SystemOverview;
  recentActivities: RecentActivity[];
  systemHealth: SystemHealth;
  chartData: ChartData;
}

const AdminDashboard: React.FC = () => {
  const [stats, setStats] = useState<AdminDashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchStats = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await axios.get('/api/v1/admin/dashboard/stats');
      setStats(response.data);
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || 'Failed to load dashboard statistics';
      setError(errorMsg);
      toast.error(errorMsg);
      console.error('Error fetching admin dashboard stats:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStats();
    // Refresh stats every 30 seconds
    const interval = setInterval(fetchStats, 30000);
    return () => clearInterval(interval);
  }, []);

  if (loading && !stats) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading dashboard...</p>
        </div>
      </div>
    );
  }

  if (error && !stats) {
    return (
      <div className="p-6">
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded relative">
          <strong className="font-bold">Error: </strong>
          <span className="block sm:inline">{error}</span>
          <button
            onClick={fetchStats}
            className="mt-4 bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (!stats) {
    return null;
  }

  const { systemOverview, recentActivities, systemHealth, chartData } = stats;

  const getActivityIcon = (type: string) => {
    switch (type) {
      case 'USER_REGISTERED':
        return Users;
      case 'DEVICE_ADDED':
        return Cpu;
      case 'TICKET_CREATED':
        return TicketIcon;
      case 'ALERT_TRIGGERED':
        return AlertTriangle;
      default:
        return Activity;
    }
  };

  const getActivityColor = (type: string) => {
    switch (type) {
      case 'USER_REGISTERED':
        return 'text-blue-600 bg-blue-100';
      case 'DEVICE_ADDED':
        return 'text-indigo-600 bg-indigo-100';
      case 'TICKET_CREATED':
        return 'text-amber-600 bg-amber-100';
      case 'ALERT_TRIGGERED':
        return 'text-red-600 bg-red-100';
      default:
        return 'text-gray-600 bg-gray-100';
    }
  };

  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleString();
  };

  const getHealthStatusColor = (status: string) => {
    switch (status) {
      case 'HEALTHY':
        return 'text-green-700 bg-green-100 border-green-200';
      case 'DEGRADED':
        return 'text-amber-700 bg-amber-100 border-amber-200';
      case 'ERROR':
        return 'text-red-700 bg-red-100 border-red-200';
      default:
        return 'text-gray-700 bg-gray-100 border-gray-200';
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Admin Dashboard</h1>
          <p className="text-gray-600 mt-1">System overview and recent activity</p>
        </div>
        <button
          onClick={fetchStats}
          disabled={loading}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
        >
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* System Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6">
        <Link to="/admin/users" className="bg-white p-6 rounded-lg shadow hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Total Users</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">{systemOverview.totalUsers}</p>
              <p className="text-sm text-blue-600 mt-2">Manage →</p>
            </div>
            <Users className="h-10 w-10 text-blue-600" />
          </div>
        </Link>

        <Link to="/devices" className="bg-white p-6 rounded-lg shadow hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Total Devices</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">{systemOverview.totalDevices}</p>
              <p className="text-sm text-indigo-600 mt-2">View →</p>
            </div>
            <Cpu className="h-10 w-10 text-indigo-600" />
          </div>
        </Link>

        <Link to="/admin/organizations" className="bg-white p-6 rounded-lg shadow hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Organizations</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">{systemOverview.totalOrganizations}</p>
              <p className="text-sm text-green-600 mt-2">Manage →</p>
            </div>
            <Building2 className="h-10 w-10 text-green-600" />
          </div>
        </Link>

        <Link to="/alerts" className="bg-white p-6 rounded-lg shadow hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Active Alerts</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">{systemOverview.activeAlerts}</p>
              <p className="text-sm text-amber-600 mt-2">View →</p>
            </div>
            <AlertTriangle className="h-10 w-10 text-amber-600" />
          </div>
        </Link>

        <Link to="/admin/support-tickets" className="bg-white p-6 rounded-lg shadow hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Pending Tickets</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">{systemOverview.pendingSupportTickets}</p>
              <p className="text-sm text-red-600 mt-2">Review →</p>
            </div>
            <TicketIcon className="h-10 w-10 text-red-600" />
          </div>
        </Link>
      </div>

      {/* System Health */}
      <div className="bg-white p-6 rounded-lg shadow">
        <h2 className="text-xl font-bold text-gray-900 mb-4">System Health</h2>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          <div>
            <p className="text-sm text-gray-600 mb-2">Status</p>
            <span className={`inline-flex items-center gap-2 px-3 py-1 rounded-full border ${getHealthStatusColor(systemHealth.status)}`}>
              {systemHealth.status === 'HEALTHY' ? <CheckCircle className="h-4 w-4" /> : <AlertCircle className="h-4 w-4" />}
              {systemHealth.status}
            </span>
          </div>
          <div>
            <p className="text-sm text-gray-600 mb-2">Data Ingestion Rate</p>
            <div className="flex items-center gap-2">
              <Activity className="h-5 w-5 text-blue-600" />
              <p className="text-xl font-semibold">{systemHealth.dataIngestionRate.toFixed(2)} msg/s</p>
            </div>
          </div>
          <div>
            <p className="text-sm text-gray-600 mb-2">Active Connections</p>
            <div className="flex items-center gap-2">
              <Wifi className="h-5 w-5 text-green-600" />
              <p className="text-xl font-semibold">{systemHealth.activeConnections}</p>
            </div>
          </div>
          <div>
            <p className="text-sm text-gray-600 mb-2">Storage Used</p>
            <div className="flex items-center gap-2">
              <Database className="h-5 w-5 text-purple-600" />
              <p className="text-xl font-semibold">{systemHealth.storageUsedMb} MB</p>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Charts */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white p-6 rounded-lg shadow">
            <h2 className="text-lg font-bold text-gray-900 mb-4">User Growth (Last 30 Days)</h2>
            <ResponsiveContainer width="100%" height={200}>
              <LineChart data={chartData.userGrowth}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                <YAxis />
                <Tooltip />
                <Line type="monotone" dataKey="value" stroke="#3B82F6" name="New Users" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          </div>

          <div className="bg-white p-6 rounded-lg shadow">
            <h2 className="text-lg font-bold text-gray-900 mb-4">Device Activity (Last 30 Days)</h2>
            <ResponsiveContainer width="100%" height={200}>
              <LineChart data={chartData.deviceActivity}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                <YAxis />
                <Tooltip />
                <Line type="monotone" dataKey="value" stroke="#10B981" name="Active Devices" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          </div>

          <div className="bg-white p-6 rounded-lg shadow">
            <h2 className="text-lg font-bold text-gray-900 mb-4">Support Ticket Volume (Last 30 Days)</h2>
            <ResponsiveContainer width="100%" height={200}>
              <LineChart data={chartData.ticketVolume}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                <YAxis />
                <Tooltip />
                <Line type="monotone" dataKey="value" stroke="#F59E0B" name="Tickets Created" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Recent Activity */}
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <div className="p-6 border-b border-gray-200">
            <h2 className="text-lg font-bold text-gray-900">Recent Activity</h2>
          </div>
          <div className="overflow-y-auto max-h-[680px]">
            <div className="divide-y divide-gray-200">
              {recentActivities.map((activity, index) => {
                const Icon = getActivityIcon(activity.type);
                const colorClass = getActivityColor(activity.type);
                return (
                  <div key={index} className="p-4 hover:bg-gray-50">
                    <div className="flex gap-3">
                      <div className={`flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center ${colorClass}`}>
                        <Icon className="h-4 w-4" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-gray-900">{activity.description}</p>
                        <p className="text-xs text-gray-500 mt-1">{formatTimestamp(activity.timestamp)}</p>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminDashboard;
