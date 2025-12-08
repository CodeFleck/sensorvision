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
  ChevronRight,
  Zap,
  BookOpen,
  Plug,
  Users,
  Building,
  TicketIcon,
  MessageSquare,
  Gauge,
  Server,
  Mail,
  Smartphone,
  Archive,
  Code,
  Webhook,
  FlaskConical,
  HelpCircle,
  Play,
} from 'lucide-react';
import { clsx } from 'clsx';
import { useAuth } from '../contexts/AuthContext';
import { SubmitIssueModal } from './SubmitIssueModal';
import { AvatarUploadModal } from './AvatarUploadModal';
import { UserAvatar } from './UserAvatar';
import { Footer } from './Footer';
import { WelcomeTour, hasTourBeenCompleted, resetTour } from './WelcomeTour';
import { useState, useEffect } from 'react';

interface LayoutProps {
  children: React.ReactNode;
}

interface NavigationItem {
  name: string;
  href?: string;
  icon: React.ComponentType<{ className?: string }>;
  adminOnly: boolean;
  excludeForAdmin?: boolean;  // Hide from admins (show only to regular users)
  children?: NavigationItem[];
}

const navigation: NavigationItem[] = [
  // User-accessible features (hidden from admins - they should use admin dashboard)
  { name: 'Dashboard', href: '/', icon: Home, adminOnly: false, excludeForAdmin: true },
  { name: 'Integration Wizard', href: '/integration-wizard', icon: Zap, adminOnly: false, excludeForAdmin: true },
  { name: 'Widget Dashboards', href: '/dashboards', icon: LayoutGrid, adminOnly: false, excludeForAdmin: true },
  {
    name: 'Devices',
    href: '/devices',
    icon: Cpu,
    adminOnly: false,
    excludeForAdmin: true,
    children: [
      { name: 'Device Groups', href: '/device-groups', icon: FolderTree, adminOnly: false },
      { name: 'Device Tags', href: '/device-tags', icon: Tag, adminOnly: false },
    ]
  },
  { name: 'Analytics', href: '/analytics', icon: BarChart3, adminOnly: false, excludeForAdmin: true },
  { name: 'Rules', href: '/rules', icon: Settings, adminOnly: false, excludeForAdmin: true },
  { name: 'Alerts', href: '/alerts', icon: AlertTriangle, adminOnly: false, excludeForAdmin: true },
  { name: 'Notifications', href: '/notifications', icon: Bell, adminOnly: false, excludeForAdmin: true },
  { name: 'Variables', href: '/variables', icon: Database, adminOnly: false, excludeForAdmin: true },
  { name: 'Data Export', href: '/data-export', icon: Download, adminOnly: false, excludeForAdmin: true },
  { name: 'Webhook Tester', href: '/webhook-tester', icon: Webhook, adminOnly: false, excludeForAdmin: true },
  { name: 'API Playground', href: '/api-playground', icon: FlaskConical, adminOnly: false, excludeForAdmin: true },
  { name: 'Plugin Marketplace', href: '/plugin-marketplace', icon: Plug, adminOnly: false },

  // Admin-only features
  { name: 'Admin Dashboard', href: '/admin-dashboard', icon: Gauge, adminOnly: true },
  { name: 'User Management', href: '/admin/users', icon: Users, adminOnly: true },
  { name: 'Organizations', href: '/admin/organizations', icon: Building, adminOnly: true },
  { name: 'Support Tickets', href: '/admin/support-tickets', icon: TicketIcon, adminOnly: true },
  { name: 'Canned Responses', href: '/admin/canned-responses', icon: MessageSquare, adminOnly: true },
  { name: 'Events', href: '/events', icon: Clock, adminOnly: true },
  { name: 'Data Ingestion', href: '/data-ingestion', icon: Upload, adminOnly: true },
  { name: 'Data Import', href: '/data-import', icon: FileUp, adminOnly: true },
  { name: 'Data Plugins', href: '/data-plugins', icon: Server, adminOnly: true },
  { name: 'Serverless Functions', href: '/serverless-functions', icon: Code, adminOnly: true },
  { name: 'Email Templates', href: '/email-templates', icon: Mail, adminOnly: true },
  { name: 'SMS Settings', href: '/sms-settings', icon: Smartphone, adminOnly: true },
  { name: 'Data Retention', href: '/data-retention', icon: Archive, adminOnly: true },
];

export const Layout = ({ children }: LayoutProps) => {
  const location = useLocation();
  const { user, logout, isAdmin, refreshUser } = useAuth();
  const [isIssueModalOpen, setIsIssueModalOpen] = useState(false);
  const [isAvatarModalOpen, setIsAvatarModalOpen] = useState(false);
  const [expandedItems, setExpandedItems] = useState<Set<string>>(new Set());
  const [showHelpMenu, setShowHelpMenu] = useState(false);
  const [startTour, setStartTour] = useState(false);
  const [isNewUser, setIsNewUser] = useState(false);

  // Check if user is new (hasn't completed the tour)
  useEffect(() => {
    if (user && !isAdmin) {
      setIsNewUser(!hasTourBeenCompleted());
    }
  }, [user, isAdmin]);

  // Toggle expansion state for parent items
  const toggleExpanded = (itemName: string) => {
    setExpandedItems(prev => {
      const newSet = new Set(prev);
      if (newSet.has(itemName)) {
        newSet.delete(itemName);
      } else {
        newSet.add(itemName);
      }
      return newSet;
    });
  };

  // Check if any child route is active
  const isChildActive = (item: NavigationItem): boolean => {
    if (!item.children) return false;
    return item.children.some(child => child.href === location.pathname);
  };

  // Filter navigation based on user role
  const filterNavigation = (items: NavigationItem[]): NavigationItem[] => {
    return items
      .map(item => {
        // Hide items marked as excludeForAdmin when user is admin
        if (item.excludeForAdmin && isAdmin) {
          return null;
        }

        // Filter children if they exist
        const filteredChildren = item.children
          ? item.children.filter(child => {
              // Exclude if marked excludeForAdmin and user is admin
              if (child.excludeForAdmin && isAdmin) return false;
              // Exclude if adminOnly and user is not admin
              if (child.adminOnly && !isAdmin) return false;
              return true;
            })
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
              const isActive = item.href ? location.pathname === item.href : false;
              const hasActiveChild = isChildActive(item);
              const isExpanded = expandedItems.has(item.name);
              const Icon = item.icon;
              const hasChildren = item.children && item.children.length > 0;

              return (
                <div key={item.name}>
                  {/* Parent Item */}
                  {item.href ? (
                    <Link
                      to={item.href}
                      className={clsx(
                        'flex items-center px-6 py-3 text-sm font-medium transition-colors',
                        isActive || hasActiveChild
                          ? 'bg-blue-50 text-blue-600 border-r-2 border-blue-600'
                          : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                      )}
                      onClick={(e) => {
                        if (hasChildren) {
                          e.preventDefault();
                          toggleExpanded(item.name);
                        }
                      }}
                    >
                      <Icon className="mr-3 h-5 w-5" />
                      <span className="flex-1">{item.name}</span>
                      {hasChildren && (
                        isExpanded ? (
                          <ChevronDown className="h-4 w-4" />
                        ) : (
                          <ChevronRight className="h-4 w-4" />
                        )
                      )}
                    </Link>
                  ) : (
                    <button
                      onClick={() => toggleExpanded(item.name)}
                      className={clsx(
                        'w-full flex items-center px-6 py-3 text-sm font-medium transition-colors',
                        hasActiveChild
                          ? 'bg-blue-50 text-blue-600 border-r-2 border-blue-600'
                          : 'text-gray-600 hover:text-gray-900 hover:bg-gray-50'
                      )}
                    >
                      <Icon className="mr-3 h-5 w-5" />
                      <span className="flex-1 text-left">{item.name}</span>
                      {hasChildren && (
                        isExpanded ? (
                          <ChevronDown className="h-4 w-4" />
                        ) : (
                          <ChevronRight className="h-4 w-4" />
                        )
                      )}
                    </button>
                  )}

                  {/* Child Items */}
                  {hasChildren && isExpanded && item.children && (
                    <div className="bg-gray-50">
                      {item.children.map((child) => {
                        const isChildItemActive = child.href === location.pathname;
                        const ChildIcon = child.icon;
                        return child.href ? (
                          <Link
                            key={child.name}
                            to={child.href}
                            className={clsx(
                              'flex items-center pl-12 pr-6 py-2.5 text-sm font-medium transition-colors',
                              isChildItemActive
                                ? 'bg-blue-100 text-blue-700 border-r-2 border-blue-600'
                                : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
                            )}
                          >
                            <ChildIcon className="mr-3 h-4 w-4" />
                            {child.name}
                          </Link>
                        ) : null;
                      })}
                    </div>
                  )}
                </div>
              );
            })}
          </nav>

          {/* User info and actions */}
          <div className="flex-shrink-0 border-t border-gray-200 bg-gray-50">
            <div className="p-4">
              <div className="flex items-start mb-3 group cursor-pointer hover:bg-white rounded-lg p-2 -m-2 transition-colors"
                   onClick={() => setIsAvatarModalOpen(true)}>
                <div className="flex-shrink-0">
                  {user && <UserAvatar user={user} size="md" editable={true} />}
                </div>
                <div className="ml-3 flex-1 min-w-0">
                  <div className="flex items-center gap-1.5">
                    <p className="text-sm font-medium text-gray-900 truncate">
                      {user?.username || 'User'}
                    </p>
                    {isAdmin && (
                      <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded bg-amber-50 border border-amber-200 flex-shrink-0">
                        <Shield className="h-3 w-3 text-amber-600" />
                        <span className="text-[10px] font-semibold text-amber-700 uppercase tracking-wide">
                          Admin
                        </span>
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-gray-500 mt-0.5 truncate" title={user?.organizationName}>
                    {user?.organizationName || 'No Organization'}
                  </p>
                </div>
              </div>

              {/* Sign Out Button */}
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
        <div className="flex-1 flex flex-col">
          {/* Header */}
          <header className="bg-white border-b border-gray-200 shadow-sm">
            <div className="flex items-center justify-between px-8 py-4">
              <div className="flex-1">
                {/* Future: Add breadcrumbs or page title here */}
              </div>
              <div className="flex items-center space-x-4">
                <Link
                  to="/how-it-works"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center space-x-2 px-4 py-2 text-sm font-medium text-gray-700 hover:text-blue-600 hover:bg-blue-50 rounded-md transition-colors"
                >
                  <BookOpen className="h-5 w-5" />
                  <span>Documentation</span>
                </Link>

                {/* Help Menu with Tour Option */}
                {!isAdmin && (
                  <div className="relative">
                    <button
                      onClick={() => setShowHelpMenu(!showHelpMenu)}
                      className="flex items-center space-x-2 px-4 py-2 text-sm font-medium text-gray-700 hover:text-blue-600 hover:bg-blue-50 rounded-md transition-colors"
                    >
                      <HelpCircle className="h-5 w-5" />
                      <span>Help</span>
                      <ChevronDown className="h-4 w-4" />
                    </button>

                    {showHelpMenu && (
                      <>
                        {/* Backdrop to close menu */}
                        <div
                          className="fixed inset-0 z-10"
                          onClick={() => setShowHelpMenu(false)}
                        />
                        {/* Dropdown menu */}
                        <div className="absolute right-0 mt-2 w-56 bg-white rounded-md shadow-lg ring-1 ring-black ring-opacity-5 z-20">
                          <div className="py-1">
                            <button
                              onClick={() => {
                                setShowHelpMenu(false);
                                resetTour();
                                setStartTour(true);
                              }}
                              className="w-full flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-blue-50 hover:text-blue-600"
                            >
                              <Play className="h-4 w-4 mr-3" />
                              Take the Tour
                            </button>
                            <button
                              onClick={() => {
                                setShowHelpMenu(false);
                                setIsIssueModalOpen(true);
                              }}
                              className="w-full flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-blue-50 hover:text-blue-600"
                            >
                              <TicketIcon className="h-4 w-4 mr-3" />
                              Report an Issue
                            </button>
                          </div>
                        </div>
                      </>
                    )}
                  </div>
                )}
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

      {/* Welcome Tour for New Users */}
      {!isAdmin && (
        <WelcomeTour
          isNewUser={isNewUser}
          forceStart={startTour}
          onComplete={() => {
            setStartTour(false);
            setIsNewUser(false);
          }}
        />
      )}
    </div>
  );
};