import { Link, useLocation } from 'react-router-dom';
import {
  Home,
  Cpu,
  BarChart3,
  AlertTriangle,
  Settings,
  Activity,
  LayoutGrid,
  Clock,
  Bell,
  Upload,
  FolderTree,
  Tag,
  Download,
  Database,
  FileUp,
  LogOut,
  Shield,
  User,
} from 'lucide-react';
import { clsx } from 'clsx';
import { useAuth } from '../contexts/AuthContext';

interface LayoutProps {
  children: React.ReactNode;
}

const navigation = [
  { name: 'Dashboard', href: '/', icon: Home, adminOnly: false },
  { name: 'Widget Dashboards', href: '/dashboards', icon: LayoutGrid, adminOnly: false },
  { name: 'Devices', href: '/devices', icon: Cpu, adminOnly: false },
  { name: 'Device Groups', href: '/device-groups', icon: FolderTree, adminOnly: true },
  { name: 'Device Tags', href: '/device-tags', icon: Tag, adminOnly: true },
  { name: 'Analytics', href: '/analytics', icon: BarChart3, adminOnly: false },
  { name: 'Rules', href: '/rules', icon: Settings, adminOnly: false },
  { name: 'Alerts', href: '/alerts', icon: AlertTriangle, adminOnly: false },
  { name: 'Events', href: '/events', icon: Clock, adminOnly: true },
  { name: 'Notifications', href: '/notifications', icon: Bell, adminOnly: false },
  { name: 'Data Ingestion', href: '/data-ingestion', icon: Upload, adminOnly: true },
  { name: 'Data Import', href: '/data-import', icon: FileUp, adminOnly: true },
  { name: 'Data Export', href: '/data-export', icon: Download, adminOnly: true },
  { name: 'Variables', href: '/variables', icon: Database, adminOnly: true },
];

export const Layout = ({ children }: LayoutProps) => {
  const location = useLocation();
  const { user, logout, isAdmin } = useAuth();

  // Filter navigation based on user role
  const filteredNavigation = navigation.filter(item => {
    if (item.adminOnly && !isAdmin) {
      return false;
    }
    return true;
  });

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="flex">
        {/* Sidebar */}
        <div className="w-64 bg-white shadow-sm flex flex-col h-screen">
          <div className="flex-shrink-0">
            <div className="p-6">
              <div className="flex items-center space-x-2">
                <Activity className="h-8 w-8 text-blue-600" />
                <h1 className="text-xl font-bold text-gray-900">SensorVision</h1>
              </div>
            </div>
          </div>

          {/* Navigation */}
          <nav className="mt-6 flex-1 overflow-y-auto">
            {filteredNavigation.map((item) => {
              const isActive = location.pathname === item.href;
              const Icon = item.icon;
              return (
                <Link
                  key={item.name}
                  to={item.href}
                  className={clsx(
                    'flex items-center px-6 py-3 text-sm font-medium transition-colors',
                    isActive
                      ? 'bg-blue-50 text-blue-600 border-r-2 border-blue-600'
                      : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                  )}
                >
                  <Icon className="mr-3 h-5 w-5" />
                  {item.name}
                </Link>
              );
            })}
          </nav>

          {/* User info and logout */}
          <div className="flex-shrink-0 border-t border-gray-200">
            <div className="p-4">
              <div className="flex items-center mb-3">
                <div className="flex-shrink-0">
                  <div className="h-10 w-10 rounded-full bg-blue-100 flex items-center justify-center">
                    <User className="h-6 w-6 text-blue-600" />
                  </div>
                </div>
                <div className="ml-3 flex-1">
                  <p className="text-sm font-medium text-gray-900">{user?.username || 'User'}</p>
                  {isAdmin && (
                    <div className="flex items-center mt-1">
                      <Shield className="h-3 w-3 text-amber-500 mr-1" />
                      <span className="text-xs text-amber-600 font-medium">Admin</span>
                    </div>
                  )}
                  {!isAdmin && (
                    <p className="text-xs text-gray-500">Standard User</p>
                  )}
                </div>
              </div>
              <button
                onClick={logout}
                className="w-full flex items-center justify-center px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-md transition-colors"
              >
                <LogOut className="mr-2 h-4 w-4" />
                Sign Out
              </button>
            </div>
          </div>
        </div>

        {/* Main content */}
        <div className="flex-1">
          <main className="p-8">
            {children}
          </main>
        </div>
      </div>
    </div>
  );
};