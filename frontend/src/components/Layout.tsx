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
  { name: 'Device Management', href: '/admin/devices', icon: Cpu, adminOnly: true },
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
    <div className="min-h-screen bg-[#030712]">
      <div className="flex">
        {/* Premium Glassmorphism Sidebar */}
        <div className="w-72 glass-nav flex flex-col h-screen sticky top-0">
          {/* Logo Section */}
          <div className="flex-shrink-0 p-6 border-b border-white/5">
            <div className="flex items-center gap-3">
              <div className="relative">
                <div className="absolute inset-0 bg-blue-500/30 blur-xl rounded-full" />
                <Activity className="relative h-8 w-8 text-blue-400" />
              </div>
              <span className="text-xl font-bold text-white tracking-tight font-display">
                Industrial Cloud
              </span>
            </div>
          </div>

          {/* Navigation */}
          <nav className="flex-1 overflow-y-auto scrollbar-thin py-4">
            {filteredNavigation.map((item) => {
              const isActive = item.href ? location.pathname === item.href : false;
              const hasActiveChild = isChildActive(item);
              const isExpanded = expandedItems.has(item.name);
              const Icon = item.icon;
              const hasChildren = item.children && item.children.length > 0;

              return (
                <div key={item.name} className="px-3">
                  {/* Parent Item */}
                  {item.href ? (
                    <Link
                      to={item.href}
                      aria-current={isActive ? 'page' : undefined}
                      className={clsx(
                        'group flex items-center px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 mb-1',
                        isActive || hasActiveChild
                          ? 'bg-gradient-to-r from-blue-500/20 to-emerald-500/10 text-white border border-blue-500/30'
                          : 'text-gray-400 hover:text-white hover:bg-white/5'
                      )}
                      onClick={(e) => {
                        if (hasChildren) {
                          e.preventDefault();
                          toggleExpanded(item.name);
                        }
                      }}
                    >
                      <div className={clsx(
                        'p-1.5 rounded-lg mr-3 transition-all duration-200',
                        isActive || hasActiveChild
                          ? 'bg-blue-500/20 text-blue-400'
                          : 'text-gray-500 group-hover:text-gray-300'
                      )}>
                        <Icon className="h-4 w-4" />
                      </div>
                      <span className="flex-1">{item.name}</span>
                      {hasChildren && (
                        <div className={clsx(
                          'transition-transform duration-200',
                          isExpanded && 'rotate-180'
                        )}>
                          <ChevronDown className="h-4 w-4 text-gray-500" />
                        </div>
                      )}
                      {(isActive || hasActiveChild) && (
                        <div className="absolute left-0 w-1 h-6 bg-gradient-to-b from-blue-400 to-emerald-400 rounded-r-full" />
                      )}
                    </Link>
                  ) : (
                    <button
                      onClick={() => toggleExpanded(item.name)}
                      className={clsx(
                        'group w-full flex items-center px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 mb-1',
                        hasActiveChild
                          ? 'bg-gradient-to-r from-blue-500/20 to-emerald-500/10 text-white border border-blue-500/30'
                          : 'text-gray-400 hover:text-white hover:bg-white/5'
                      )}
                    >
                      <div className={clsx(
                        'p-1.5 rounded-lg mr-3 transition-all duration-200',
                        hasActiveChild
                          ? 'bg-blue-500/20 text-blue-400'
                          : 'text-gray-500 group-hover:text-gray-300'
                      )}>
                        <Icon className="h-4 w-4" />
                      </div>
                      <span className="flex-1 text-left">{item.name}</span>
                      {hasChildren && (
                        <div className={clsx(
                          'transition-transform duration-200',
                          isExpanded && 'rotate-180'
                        )}>
                          <ChevronDown className="h-4 w-4 text-gray-500" />
                        </div>
                      )}
                    </button>
                  )}

                  {/* Child Items */}
                  {hasChildren && isExpanded && item.children && (
                    <div className="ml-4 pl-4 border-l border-white/5 mb-2">
                      {item.children.map((child) => {
                        const isChildItemActive = child.href === location.pathname;
                        const ChildIcon = child.icon;
                        return child.href ? (
                          <Link
                            key={child.name}
                            to={child.href}
                            className={clsx(
                              'group flex items-center px-3 py-2 rounded-lg text-sm font-medium transition-all duration-200 mb-1',
                              isChildItemActive
                                ? 'bg-blue-500/15 text-blue-400'
                                : 'text-gray-500 hover:text-gray-300 hover:bg-white/5'
                            )}
                          >
                            <ChildIcon className={clsx(
                              'mr-2.5 h-4 w-4 transition-colors',
                              isChildItemActive ? 'text-blue-400' : 'text-gray-600 group-hover:text-gray-400'
                            )} />
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

          {/* User Section */}
          <div className="flex-shrink-0 border-t border-white/5 p-4">
            {/* User Profile */}
            <div
              className="flex items-center gap-3 p-3 rounded-xl hover:bg-white/5 cursor-pointer transition-all duration-200 mb-3"
              onClick={() => setIsAvatarModalOpen(true)}
            >
              <div className="relative">
                {user && <UserAvatar user={user} size="md" editable={true} />}
                <div className="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-emerald-500 rounded-full border-2 border-[#0d1117]" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                  <p className="text-sm font-medium text-white truncate">
                    {user?.username || 'User'}
                  </p>
                  {isAdmin && (
                    <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-md bg-amber-500/20 border border-amber-500/30">
                      <Shield className="h-2.5 w-2.5 text-amber-400" />
                      <span className="text-[10px] font-semibold text-amber-400 uppercase tracking-wider">
                        Admin
                      </span>
                    </span>
                  )}
                </div>
                <p className="text-xs text-gray-500 truncate" title={user?.organizationName}>
                  {user?.organizationName || 'No Organization'}
                </p>
              </div>
            </div>

            {/* Sign Out Button */}
            <button
              onClick={logout}
              className="w-full flex items-center justify-center gap-2 px-4 py-2.5 text-sm font-medium text-red-400 bg-red-500/10 hover:bg-red-500/20 border border-red-500/20 hover:border-red-500/40 rounded-xl transition-all duration-200"
            >
              <LogOut className="h-4 w-4" />
              Sign Out
            </button>
          </div>
        </div>

        {/* Main content */}
        <div className="flex-1 flex flex-col min-h-screen">
          {/* Premium Header */}
          <header className="sticky top-0 z-10 border-b border-white/5 bg-[#030712]/80 backdrop-blur-xl">
            <div className="flex items-center justify-between px-8 py-4">
              <div className="flex-1">
                {/* Breadcrumb or page title area */}
              </div>
              <div className="flex items-center gap-2">
                <Link
                  to="/how-it-works"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-400 hover:text-white hover:bg-white/5 rounded-xl transition-all duration-200"
                >
                  <BookOpen className="h-4 w-4" />
                  <span>Docs</span>
                </Link>

                {/* Help Menu with Tour Option */}
                {!isAdmin && (
                  <div className="relative">
                    <button
                      onClick={() => setShowHelpMenu(!showHelpMenu)}
                      aria-expanded={showHelpMenu}
                      aria-haspopup="menu"
                      aria-controls="help-menu"
                      className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-400 hover:text-white hover:bg-white/5 rounded-xl transition-all duration-200"
                    >
                      <HelpCircle className="h-4 w-4" />
                      <span>Help</span>
                      <ChevronDown className={clsx(
                        "h-3 w-3 transition-transform duration-200",
                        showHelpMenu && "rotate-180"
                      )} />
                    </button>

                    {showHelpMenu && (
                      <>
                        {/* Backdrop to close menu */}
                        <div
                          className="fixed inset-0 z-10"
                          onClick={() => setShowHelpMenu(false)}
                        />
                        {/* Dropdown menu */}
                        <div
                          id="help-menu"
                          role="menu"
                          className="absolute right-0 mt-2 w-56 glass-card rounded-xl overflow-hidden z-20 animate-fadeIn"
                        >
                          <div className="py-1">
                            <button
                              role="menuitem"
                              onClick={() => {
                                setShowHelpMenu(false);
                                resetTour();
                                setStartTour(true);
                              }}
                              className="w-full flex items-center px-4 py-3 text-sm text-gray-300 hover:bg-white/5 hover:text-white transition-colors"
                            >
                              <Play className="h-4 w-4 mr-3 text-blue-400" />
                              Take the Tour
                            </button>
                            <button
                              role="menuitem"
                              onClick={() => {
                                setShowHelpMenu(false);
                                setIsIssueModalOpen(true);
                              }}
                              className="w-full flex items-center px-4 py-3 text-sm text-gray-300 hover:bg-white/5 hover:text-white transition-colors"
                            >
                              <TicketIcon className="h-4 w-4 mr-3 text-amber-400" />
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
          <main className="flex-1 p-8">
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
