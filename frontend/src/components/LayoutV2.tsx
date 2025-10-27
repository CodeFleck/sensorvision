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
  ChevronDown,
  Zap,
  BookOpen,
} from 'lucide-react';
import { clsx } from 'clsx';
import { useAuth } from '../contexts/AuthContext';
import { SubmitIssueModal } from './SubmitIssueModal';
import { AvatarUploadModal } from './AvatarUploadModal';
import { UserAvatar } from './UserAvatar';
import { Footer } from './Footer';
import { useState } from 'react';

interface LayoutProps {
  children: React.ReactNode;
}

interface NavigationItem {
  name: string;
  href?: string;
  icon: React.ComponentType<{ className?: string }>;
  adminOnly: boolean;
  description?: string;
  children?: NavigationItem[];
}

const navigation: NavigationItem[] = [
  // Top-level items (frequently used, always visible)
  {
    name: 'Dashboard',
    href: '/',
    icon: Home,
    adminOnly: false,
    description: 'Overview and real-time monitoring'
  },
  {
    name: 'Devices',
    href: '/devices',
    icon: Cpu,
    adminOnly: false,
    description: 'Manage connected devices'
  },
  {
    name: 'Analytics',
    href: '/analytics',
    icon: BarChart3,
    adminOnly: false,
    description: 'Historical data and insights'
  },
  {
    name: 'Integration Wizard',
    href: '/integration-wizard',
    icon: Zap,
    adminOnly: false,
    description: 'Quick device integration setup'
  },

  // Grouped dropdown: Monitoring & Alerts
  {
    name: 'Monitoring & Alerts',
    icon: Bell,
    adminOnly: false,
    children: [
      {
        name: 'Rules',
        href: '/rules',
        icon: Settings,
        adminOnly: false,
        description: 'Configure monitoring rules'
      },
      {
        name: 'Alerts',
        href: '/alerts',
        icon: AlertTriangle,
        adminOnly: false,
        description: 'View triggered alerts'
      },
      {
        name: 'Notifications',
        href: '/notifications',
        icon: Bell,
        adminOnly: false,
        description: 'Manage alert notifications'
      },
      {
        name: 'Events',
        href: '/events',
        icon: Clock,
        adminOnly: true,
        description: 'System event log'
      },
    ]
  },

  // Grouped dropdown: Dashboards & Views
  {
    name: 'Dashboards & Views',
    icon: LayoutGrid,
    adminOnly: false,
    children: [
      {
        name: 'Widget Dashboards',
        href: '/dashboards',
        icon: LayoutGrid,
        adminOnly: false,
        description: 'Custom dashboard builder'
      },
    ]
  },

  // Admin Tools dropdown (admin-only)
  {
    name: 'Admin Tools',
    icon: Shield,
    adminOnly: true,
    children: [
      {
        name: 'Data Ingestion',
        href: '/data-ingestion',
        icon: Upload,
        adminOnly: true,
        description: 'Bulk data ingestion'
      },
      {
        name: 'Data Import',
        href: '/data-import',
        icon: FileUp,
        adminOnly: true,
        description: 'Import from files'
      },
      {
        name: 'Data Export',
        href: '/data-export',
        icon: Download,
        adminOnly: true,
        description: 'Export data archives'
      },
      {
        name: 'Variables',
        href: '/variables',
        icon: Database,
        adminOnly: true,
        description: 'Manage variable definitions'
      },
      {
        name: 'Device Groups',
        href: '/device-groups',
        icon: FolderTree,
        adminOnly: true,
        description: 'Organize devices into groups'
      },
      {
        name: 'Device Tags',
        href: '/device-tags',
        icon: Tag,
        adminOnly: true,
        description: 'Tag management system'
      },
    ]
  },
];

export const LayoutV2 = ({ children }: LayoutProps) => {
  const location = useLocation();
  const { user, logout, isAdmin, refreshUser } = useAuth();
  const [isIssueModalOpen, setIsIssueModalOpen] = useState(false);
  const [isAvatarModalOpen, setIsAvatarModalOpen] = useState(false);
  const [hoveredItem, setHoveredItem] = useState<string | null>(null);
  const [clickedItem, setClickedItem] = useState<string | null>(null);

  // Toggle clicked state for dropdowns
  const toggleDropdown = (itemName: string) => {
    setClickedItem(prev => prev === itemName ? null : itemName);
  };

  // Check if any child route is active
  const isChildActive = (item: NavigationItem): boolean => {
    if (!item.children) return false;
    return item.children.some(child => child.href === location.pathname);
  };

  // Determine if dropdown should be shown (hover OR click)
  const isDropdownOpen = (itemName: string): boolean => {
    return hoveredItem === itemName || clickedItem === itemName;
  };

  // Filter navigation based on user role
  const filterNavigation = (items: NavigationItem[]): NavigationItem[] => {
    return items
      .map(item => {
        // Filter children if they exist
        const filteredChildren = item.children
          ? item.children.filter(child => !child.adminOnly || isAdmin)
          : undefined;

        // If item has children, keep it if at least one child is visible
        if (item.children) {
          if (filteredChildren && filteredChildren.length > 0) {
            return { ...item, children: filteredChildren };
          }
          // If no children are visible but parent is not admin-only, keep parent
          if (!item.adminOnly) {
            return { ...item, children: undefined };
          }
          return null;
        }

        // For items without children, apply standard filter
        if (item.adminOnly && !isAdmin) {
          return null;
        }
        return item;
      })
      .filter((item): item is NavigationItem => item !== null);
  };

  const filteredNavigation = filterNavigation(navigation);

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="flex flex-col h-screen">
        {/* Top Navigation Bar */}
        <header className="bg-white border-b border-gray-200 shadow-sm flex-shrink-0">
          <div className="px-6 py-3">
            <div className="flex items-center justify-between">
              {/* Logo */}
              <div className="flex items-center space-x-2">
                <Activity className="h-7 w-7 text-blue-600" />
                <h1 className="text-lg font-bold text-gray-900">SensorVision</h1>
              </div>

              {/* Navigation Menu */}
              <nav className="flex items-center space-x-1 flex-1 px-8">
                {filteredNavigation.map((item) => {
                  const isActive = item.href ? location.pathname === item.href : false;
                  const hasActiveChild = isChildActive(item);
                  const Icon = item.icon;
                  const hasChildren = item.children && item.children.length > 0;
                  const showDropdown = hasChildren && isDropdownOpen(item.name);

                  return (
                    <div
                      key={item.name}
                      className="relative"
                      onMouseEnter={() => hasChildren && setHoveredItem(item.name)}
                      onMouseLeave={() => hasChildren && setHoveredItem(null)}
                    >
                      {/* Top-level nav item */}
                      {item.href ? (
                        <Link
                          to={item.href}
                          className={clsx(
                            'flex items-center space-x-2 px-3 py-2 text-sm font-medium rounded-md transition-all',
                            isActive
                              ? 'bg-blue-600 text-white shadow-sm'
                              : 'text-gray-700 hover:text-blue-600 hover:bg-blue-50'
                          )}
                        >
                          <Icon className="h-4 w-4" />
                          <span>{item.name}</span>
                        </Link>
                      ) : (
                        <button
                          onClick={() => toggleDropdown(item.name)}
                          className={clsx(
                            'flex items-center space-x-2 px-3 py-2 text-sm font-medium rounded-md transition-all',
                            hasActiveChild
                              ? 'bg-blue-600 text-white shadow-sm'
                              : showDropdown
                              ? 'text-blue-600 bg-blue-50'
                              : 'text-gray-700 hover:text-blue-600 hover:bg-blue-50'
                          )}
                        >
                          <Icon className="h-4 w-4" />
                          <span>{item.name}</span>
                          {hasChildren && (
                            <ChevronDown
                              className={clsx(
                                'h-3.5 w-3.5 transition-transform',
                                showDropdown && 'rotate-180'
                              )}
                            />
                          )}
                        </button>
                      )}

                      {/* Mega Menu Dropdown */}
                      {hasChildren && showDropdown && item.children && (
                        <div
                          className="absolute left-0 top-full mt-1 w-72 bg-white rounded-lg shadow-lg border border-gray-200 py-2 z-50 animate-fadeIn"
                          style={{
                            animation: 'fadeIn 0.15s ease-in-out'
                          }}
                        >
                          {item.children.map((child) => {
                            const isChildItemActive = child.href === location.pathname;
                            const ChildIcon = child.icon;
                            return child.href ? (
                              <Link
                                key={child.name}
                                to={child.href}
                                onClick={() => setClickedItem(null)}
                                className={clsx(
                                  'flex items-start px-4 py-3 transition-colors group',
                                  isChildItemActive
                                    ? 'bg-blue-50 text-blue-700'
                                    : 'text-gray-700 hover:bg-gray-50'
                                )}
                              >
                                <ChildIcon className={clsx(
                                  'h-5 w-5 mr-3 mt-0.5 flex-shrink-0',
                                  isChildItemActive ? 'text-blue-600' : 'text-gray-400 group-hover:text-blue-600'
                                )} />
                                <div className="flex-1 min-w-0">
                                  <div className={clsx(
                                    'text-sm font-medium',
                                    isChildItemActive && 'text-blue-700'
                                  )}>
                                    {child.name}
                                  </div>
                                  {child.description && (
                                    <div className="text-xs text-gray-500 mt-0.5">
                                      {child.description}
                                    </div>
                                  )}
                                </div>
                              </Link>
                            ) : null;
                          })}
                        </div>
                      )}
                    </div>
                  );
                })}
              </nav>

              {/* Right side actions */}
              <div className="flex items-center space-x-3">
                <Link
                  to="/how-it-works"
                  className="flex items-center space-x-2 px-3 py-2 text-sm font-medium text-gray-700 hover:text-blue-600 hover:bg-blue-50 rounded-md transition-colors"
                >
                  <BookOpen className="h-4 w-4" />
                  <span>Docs</span>
                </Link>

                {/* User Profile */}
                <div
                  className="flex items-center space-x-2 px-3 py-2 cursor-pointer hover:bg-gray-50 rounded-md transition-colors"
                  onClick={() => setIsAvatarModalOpen(true)}
                >
                  {user && <UserAvatar user={user} size="sm" editable={false} />}
                  <div className="flex items-center gap-1.5">
                    <span className="text-sm font-medium text-gray-700">{user?.username}</span>
                    {isAdmin && (
                      <Shield className="h-3.5 w-3.5 text-amber-600" />
                    )}
                  </div>
                </div>

                <button
                  onClick={logout}
                  className="flex items-center space-x-2 px-3 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-md transition-colors"
                >
                  <LogOut className="h-4 w-4" />
                  <span>Sign Out</span>
                </button>
              </div>
            </div>
          </div>
        </header>

        {/* Main content area */}
        <main className="flex-1 p-8 overflow-y-auto">
          {children}
        </main>

        {/* Footer */}
        <Footer onReportIssue={() => setIsIssueModalOpen(true)} />
      </div>

      {/* Issue Submission Modal */}
      <SubmitIssueModal
        isOpen={isIssueModalOpen}
        onClose={() => setIsIssueModalOpen(false)}
        onSuccess={() => {
          // Optional: You could show a success toast here
        }}
      />

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