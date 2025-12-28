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
  Headphones,
  MessageSquare,
  Code,
  Plug2,
  Store,
  Archive,
  Webhook,
  Terminal,
  Network,
  Mail,
  Phone,
  DollarSign,
  User,
  Building2,
  Rocket,
  Trash2,
} from 'lucide-react';
import { clsx } from 'clsx';
import { useAuth } from '../contexts/AuthContext';
import { WelcomeModal } from './WelcomeModal';
import { useUnreadTickets } from '../hooks/useUnreadTickets';
import { SubmitIssueModal } from './SubmitIssueModal';
import { AvatarUploadModal } from './AvatarUploadModal';
import { UserAvatar } from './UserAvatar';
import { Footer } from './Footer';
import ThemeToggle from './ThemeToggle';
import { useState, useEffect } from 'react';

interface LayoutProps {
  children: React.ReactNode;
}

interface NavigationItem {
  name: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  adminOnly: boolean;
  excludeForAdmin?: boolean; // Hide this item from admins
  requiredRole?: string; // Require specific role (e.g., 'ROLE_DEVELOPER')
}

interface NavigationSection {
  name: string;
  icon: React.ComponentType<{ className?: string }>;
  iconColor: string;
  items: NavigationItem[];
  adminOnly: boolean;
  excludeForAdmin?: boolean; // Hide entire section from admins
}

const navigationSections: NavigationSection[] = [
  {
    name: 'GETTING STARTED',
    icon: Rocket,
    iconColor: 'text-purple-600',
    adminOnly: false,
    excludeForAdmin: true, // Hide from admins who don't need onboarding
    items: [
      { name: 'Integration Wizard', href: '/integration-wizard', icon: Zap, adminOnly: false },
      { name: 'How It Works', href: '/how-it-works', icon: BookOpen, adminOnly: false },
    ],
  },
  {
    name: 'CORE',
    icon: Home,
    iconColor: 'text-link',
    adminOnly: false,
    items: [
      { name: 'Dashboard', href: '/', icon: Home, adminOnly: false },
      { name: 'Widget Dashboards', href: '/dashboards', icon: LayoutGrid, adminOnly: false, excludeForAdmin: true },
      { name: 'Analytics', href: '/analytics', icon: BarChart3, adminOnly: false, excludeForAdmin: true },
    ],
  },
  {
    name: 'DEVICES & DATA',
    icon: Cpu,
    iconColor: 'text-green-600',
    adminOnly: false,
    excludeForAdmin: true, // Hide from admin panel - user-only feature
    items: [
      { name: 'Devices', href: '/devices', icon: Cpu, adminOnly: false, excludeForAdmin: true },
      { name: 'Device Groups', href: '/device-groups', icon: FolderTree, adminOnly: false, excludeForAdmin: true },
      { name: 'Device Tags', href: '/device-tags', icon: Tag, adminOnly: false, excludeForAdmin: true },
      { name: 'Serverless Functions', href: '/serverless-functions', icon: Code, adminOnly: false, excludeForAdmin: true },
      { name: 'Data Plugins', href: '/data-plugins', icon: Plug2, adminOnly: false, excludeForAdmin: true },
      { name: 'Plugin Marketplace', href: '/plugin-marketplace', icon: Store, adminOnly: false, excludeForAdmin: true },
    ],
  },
  {
    name: 'MONITORING',
    icon: AlertTriangle,
    iconColor: 'text-orange-600',
    adminOnly: false,
    excludeForAdmin: true, // Hide from admin dashboard - user-only feature
    items: [
      { name: 'Rules', href: '/rules', icon: Settings, adminOnly: false, excludeForAdmin: true },
      { name: 'Global Rules', href: '/global-rules', icon: Network, adminOnly: false, excludeForAdmin: true },
      { name: 'Alerts', href: '/alerts', icon: AlertTriangle, adminOnly: false, excludeForAdmin: true },
      { name: 'Notifications', href: '/notifications', icon: Bell, adminOnly: false, excludeForAdmin: true },
      { name: 'Phone Numbers', href: '/phone-numbers', icon: Phone, adminOnly: false, excludeForAdmin: true },
      { name: 'SMS Settings', href: '/sms-settings', icon: DollarSign, adminOnly: false, excludeForAdmin: true },
      { name: 'Events', href: '/events', icon: Clock, adminOnly: false, excludeForAdmin: true },
    ],
  },
  {
    name: 'DATA MANAGEMENT',
    icon: Database,
    iconColor: 'text-purple-600',
    adminOnly: false,
    excludeForAdmin: true, // Hide from admin dashboard - user-only feature
    items: [
      { name: 'Data Ingestion', href: '/data-ingestion', icon: Upload, adminOnly: false, excludeForAdmin: true },
      { name: 'Data Import', href: '/data-import', icon: FileUp, adminOnly: false, excludeForAdmin: true },
      { name: 'Data Export', href: '/data-export', icon: Download, adminOnly: false, excludeForAdmin: true },
      { name: 'Variables', href: '/variables', icon: Database, adminOnly: false, excludeForAdmin: true },
      { name: 'Data Retention', href: '/data-retention', icon: Archive, adminOnly: false, excludeForAdmin: true },
      { name: 'Webhook Tester', href: '/webhook-tester', icon: Webhook, adminOnly: false, excludeForAdmin: true },
      { name: 'API Playground', href: '/api-playground', icon: Terminal, adminOnly: false, excludeForAdmin: true },
    ],
  },
  {
    name: 'DEVELOPER TOOLS',
    icon: Terminal,
    iconColor: 'text-cyan-600',
    adminOnly: false,
    items: [
      { name: 'System Logs', href: '/logs', icon: Terminal, adminOnly: false, requiredRole: 'ROLE_DEVELOPER' },
    ],
  },
  {
    name: 'ADMINISTRATION',
    icon: Shield,
    iconColor: 'text-indigo-600',
    adminOnly: true,
    items: [
      { name: 'User Management', href: '/admin/users', icon: User, adminOnly: true },
      { name: 'Device Management', href: '/admin/devices', icon: Cpu, adminOnly: true },
      { name: 'Organizations', href: '/admin/organizations', icon: Building2, adminOnly: true },
      { name: 'Trash', href: '/admin/trash', icon: Trash2, adminOnly: true },
    ],
  },
  {
    name: 'HELP & SUPPORT',
    icon: Headphones,
    iconColor: 'text-pink-600',
    adminOnly: false,
    items: [
      { name: 'My Tickets', href: '/my-tickets', icon: MessageSquare, adminOnly: false, excludeForAdmin: true },
      { name: 'Support Tickets', href: '/admin/support-tickets', icon: Headphones, adminOnly: true },
      { name: 'Canned Responses', href: '/admin/canned-responses', icon: MessageSquare, adminOnly: true },
      { name: 'Email Templates', href: '/email-templates', icon: Mail, adminOnly: true },
    ],
  },
];

export const LayoutV1 = ({ children }: LayoutProps) => {
  const location = useLocation();
  const { user, logout, isAdmin, hasRole, refreshUser } = useAuth();
  const { unreadCount } = useUnreadTickets();
  const [isIssueModalOpen, setIsIssueModalOpen] = useState(false);
  const [isAvatarModalOpen, setIsAvatarModalOpen] = useState(false);
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);

  // Initialize collapsed sections from localStorage, default to all expanded
  const [collapsedSections, setCollapsedSections] = useState<Set<string>>(() => {
    const saved = localStorage.getItem('collapsedSections');
    return saved ? new Set(JSON.parse(saved)) : new Set();
  });

  // Track which section is being hovered
  const [hoveredSection, setHoveredSection] = useState<string | null>(null);

  // Save collapsed state to localStorage whenever it changes
  useEffect(() => {
    localStorage.setItem('collapsedSections', JSON.stringify(Array.from(collapsedSections)));
  }, [collapsedSections]);

  // Toggle section collapse state
  const toggleSection = (sectionName: string) => {
    setCollapsedSections(prev => {
      const newSet = new Set(prev);
      if (newSet.has(sectionName)) {
        newSet.delete(sectionName);
      } else {
        newSet.add(sectionName);
      }
      return newSet;
    });
  };

  // Filter sections and items based on user role
  const getVisibleSections = (): NavigationSection[] => {
    return navigationSections
      .map(section => {
        // If section is admin-only and user is not admin, hide entire section
        if (section.adminOnly && !isAdmin) {
          return null;
        }

        // If section is marked excludeForAdmin and user is admin, hide entire section
        if (section.excludeForAdmin && isAdmin) {
          return null;
        }

        // Filter items within section
        const visibleItems = section.items.filter(item => {
          // Hide admin-only items from non-admins
          if (item.adminOnly && !isAdmin) {
            return false;
          }
          // Hide items marked as excludeForAdmin from admins
          if (item.excludeForAdmin && isAdmin) {
            return false;
          }
          // Hide items that require a specific role if user doesn't have it
          if (item.requiredRole && !hasRole(item.requiredRole)) {
            return false;
          }
          return true;
        });

        // If no items are visible, hide section
        if (visibleItems.length === 0) {
          return null;
        }

        return { ...section, items: visibleItems };
      })
      .filter((section): section is NavigationSection => section !== null);
  };

  const visibleSections = getVisibleSections();

  return (
    <div className="min-h-screen bg-canvas">
      <div className="flex">
        {/* Sidebar */}
        <div className="w-64 bg-primary shadow-sm flex flex-col h-screen border-r border-default">
          <div className="flex-shrink-0">
            <div className="p-6">
              <div className="flex items-center space-x-2">
                <Activity className="h-8 w-8 text-link" />
                <h1 className="text-xl font-bold text-primary">Industrial Cloud</h1>
              </div>
            </div>
          </div>

          {/* Navigation */}
          <nav className="mt-4 flex-1 overflow-y-auto px-3">
            {visibleSections.map((section, sectionIndex) => {
              const SectionIcon = section.icon;
              const isCollapsed = collapsedSections.has(section.name);
              const isHovered = hoveredSection === section.name;
              const isExpanded = !isCollapsed || isHovered; // Expand if not collapsed OR if being hovered

              return (
                <div key={section.name} className="mb-4">
                  {/* Section Header */}
                  <button
                    onClick={() => toggleSection(section.name)}
                    onMouseEnter={() => setHoveredSection(section.name)}
                    onMouseLeave={() => setHoveredSection(null)}
                    className="w-full flex items-center px-3 py-2 mb-1 text-xs font-semibold text-secondary hover:text-primary hover:bg-hover rounded-md transition-all duration-200 group"
                  >
                    <SectionIcon className={clsx('h-4 w-4 mr-2 flex-shrink-0', section.iconColor)} />
                    <span className="flex-1 text-left tracking-wide">{section.name}</span>
                    {isCollapsed ? (
                      <ChevronRight className="h-3.5 w-3.5 text-secondary group-hover:text-primary transition-colors" />
                    ) : (
                      <ChevronDown className="h-3.5 w-3.5 text-secondary group-hover:text-primary transition-colors" />
                    )}
                  </button>

                  {/* Section Items */}
                  <div
                    className={clsx(
                      'overflow-hidden transition-all duration-300 ease-in-out',
                      isExpanded ? 'max-h-[500px] opacity-100' : 'max-h-0 opacity-0'
                    )}
                  >
                    <div className="space-y-0.5">
                      {section.items.map((item) => {
                        const ItemIcon = item.icon;
                        const isActive = location.pathname === item.href;

                        return (
                          <Link
                            key={item.name}
                            to={item.href}
                            className={clsx(
                              'flex items-center px-3 py-2.5 text-sm font-medium rounded-md transition-all duration-200 relative',
                              isActive
                                ? 'bg-hover text-link shadow-sm'
                                : 'text-secondary hover:text-primary hover:bg-hover'
                            )}
                          >
                            <ItemIcon
                              className={clsx(
                                'mr-3 h-4.5 w-4.5 flex-shrink-0',
                                isActive ? 'text-link' : 'text-secondary'
                              )}
                            />
                            <span className="flex-1">{item.name}</span>
                            {/* Show unread badge on My Tickets link */}
                            {item.href === '/my-tickets' && unreadCount > 0 && (
                              <span className="ml-2 flex-shrink-0 inline-flex items-center justify-center w-5 h-5 text-xs font-bold text-white bg-red-500 rounded-full animate-pulse">
                                {unreadCount > 9 ? '9+' : unreadCount}
                              </span>
                            )}
                          </Link>
                        );
                      })}
                    </div>
                  </div>

                  {/* Divider after section (except last) */}
                  {sectionIndex < visibleSections.length - 1 && (
                    <div className="mt-4 border-t border-muted" />
                  )}
                </div>
              );
            })}
          </nav>
        </div>

        {/* Main content */}
        <div className="flex-1 flex flex-col">
          {/* Header */}
          <header className="bg-primary border-b border-default shadow-sm">
            <div className="flex items-center justify-between px-8 py-4">
              <div className="flex-1">
                {/* Future: Add breadcrumbs or page title here */}
              </div>
              <div className="flex items-center space-x-4">
                <Link
                  to="/how-it-works"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center space-x-2 px-4 py-2 text-sm font-medium text-secondary hover:text-link hover:bg-hover rounded-md transition-colors"
                >
                  <BookOpen className="h-5 w-5" />
                  <span>Documentation</span>
                </Link>

                {/* User Menu Dropdown */}
                <div className="relative">
                  <button
                    onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                    className="flex items-center space-x-3 px-3 py-2 rounded-md hover:bg-hover transition-colors"
                  >
                    {user && <UserAvatar user={user} size="sm" />}
                    <div className="flex flex-col items-start">
                      <div className="flex items-center gap-1.5">
                        <span className="text-sm font-medium text-primary">
                          {user?.username || 'User'}
                        </span>
                        {isAdmin && (
                          <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded bg-[var(--status-warning-bg)] border border-[var(--accent-warning)]/30">
                            <Shield className="h-3 w-3 text-[var(--status-warning-text)]" />
                            <span className="text-[10px] font-semibold text-[var(--status-warning-text)] uppercase tracking-wide">
                              Admin
                            </span>
                          </span>
                        )}
                      </div>
                    </div>
                    <ChevronDown className={`h-4 w-4 text-secondary transition-transform ${isUserMenuOpen ? 'rotate-180' : ''}`} />
                  </button>

                  {isUserMenuOpen && (
                    <>
                      <div
                        className="fixed inset-0 z-10"
                        onClick={() => setIsUserMenuOpen(false)}
                      />
                      <div className="absolute right-0 mt-2 w-64 bg-dropdown rounded-md shadow-lg border border-default z-20 animate-fadeIn">
                        <div className="p-3 border-b border-muted">
                          <p className="text-sm font-medium text-primary">
                            {user?.username || 'User'}
                          </p>
                          <p className="text-xs text-secondary mt-0.5">
                            {user?.organizationName || 'No Organization'}
                          </p>
                        </div>

                        <div className="p-3 border-b border-muted">
                          <div className="text-xs font-semibold text-secondary mb-2">
                            Theme
                          </div>
                          <ThemeToggle />
                        </div>

                        <div className="p-2">
                          <Link
                            to="/profile"
                            onClick={() => setIsUserMenuOpen(false)}
                            className="w-full flex items-center px-3 py-2 text-sm text-secondary hover:bg-hover rounded-md transition-colors"
                          >
                            Edit Profile
                          </Link>
                          <Link
                            to="/settings"
                            onClick={() => setIsUserMenuOpen(false)}
                            className="w-full flex items-center px-3 py-2 text-sm text-secondary hover:bg-hover rounded-md transition-colors"
                          >
                            <Settings className="mr-2 h-4 w-4" />
                            Settings
                          </Link>
                          <button
                            onClick={() => {
                              setIsUserMenuOpen(false);
                              logout();
                            }}
                            className="w-full flex items-center px-3 py-2 text-sm text-danger hover:bg-[var(--status-error-bg)] rounded-md transition-colors"
                          >
                            <LogOut className="mr-2 h-4 w-4" />
                            Sign Out
                          </button>
                        </div>
                      </div>
                    </>
                  )}
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

      {/* Welcome Modal for first-time users */}
      <WelcomeModal />
    </div>
  );
};