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
  Archive,
  TestTube,
  Terminal,
  Mail,
} from 'lucide-react';
import { clsx } from 'clsx';
import { useAuth } from '../contexts/AuthContext';
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
}

interface NavigationSection {
  name: string;
  icon: React.ComponentType<{ className?: string }>;
  iconColor: string;
  items: NavigationItem[];
  adminOnly: boolean;
}

const navigationSections: NavigationSection[] = [
  {
    name: 'CORE',
    icon: Home,
    iconColor: 'text-blue-600',
    adminOnly: false,
    items: [
      { name: 'Dashboard', href: '/', icon: Home, adminOnly: false },
      { name: 'Widget Dashboards', href: '/dashboards', icon: LayoutGrid, adminOnly: false },
      { name: 'Analytics', href: '/analytics', icon: BarChart3, adminOnly: false },
    ],
  },
  {
    name: 'DEVICES & DATA',
    icon: Cpu,
    iconColor: 'text-green-600',
    adminOnly: false,
    items: [
      { name: 'Devices', href: '/devices', icon: Cpu, adminOnly: false },
      { name: 'Device Groups', href: '/device-groups', icon: FolderTree, adminOnly: true },
      { name: 'Device Tags', href: '/device-tags', icon: Tag, adminOnly: true },
      { name: 'Integration Wizard', href: '/integration-wizard', icon: Zap, adminOnly: false },
      { name: 'Serverless Functions', href: '/serverless-functions', icon: Code, adminOnly: false },
      { name: 'Data Plugins', href: '/data-plugins', icon: Plug2, adminOnly: false },
    ],
  },
  {
    name: 'MONITORING',
    icon: AlertTriangle,
    iconColor: 'text-orange-600',
    adminOnly: false,
    items: [
      { name: 'Rules', href: '/rules', icon: Settings, adminOnly: false },
      { name: 'Alerts', href: '/alerts', icon: AlertTriangle, adminOnly: false },
      { name: 'Notifications', href: '/notifications', icon: Bell, adminOnly: false },
      { name: 'Events', href: '/events', icon: Clock, adminOnly: true },
    ],
  },
  {
    name: 'DATA MANAGEMENT',
    icon: Database,
    iconColor: 'text-purple-600',
    adminOnly: true,
    items: [
      { name: 'Data Ingestion', href: '/data-ingestion', icon: Upload, adminOnly: true },
      { name: 'Data Import', href: '/data-import', icon: FileUp, adminOnly: true },
      { name: 'Data Export', href: '/data-export', icon: Download, adminOnly: true },
      { name: 'Variables', href: '/variables', icon: Database, adminOnly: true },
      { name: 'Data Retention', href: '/data-retention', icon: Archive, adminOnly: true },
      { name: 'Webhook Tester', href: '/webhook-tester', icon: TestTube, adminOnly: true },
      { name: 'API Playground', href: '/api-playground', icon: Terminal, adminOnly: true },
      { name: 'Email Templates', href: '/email-templates', icon: Mail, adminOnly: true },
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
  const { user, logout, isAdmin, refreshUser } = useAuth();
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
          return true;
        });

        // If section is admin-only and user is not admin, hide entire section
        if (section.adminOnly && !isAdmin) {
          return null;
        }

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
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <div className="flex">
        {/* Sidebar */}
        <div className="w-64 bg-white dark:bg-gray-800 shadow-sm flex flex-col h-screen border-r border-gray-200 dark:border-gray-700">
          <div className="flex-shrink-0">
            <div className="p-6">
              <div className="flex items-center space-x-2">
                <Activity className="h-8 w-8 text-blue-600 dark:text-blue-400" />
                <h1 className="text-xl font-bold text-gray-900 dark:text-white">SensorVision</h1>
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
                    className="w-full flex items-center px-3 py-2 mb-1 text-xs font-semibold text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-700 rounded-md transition-all duration-200 group"
                  >
                    <SectionIcon className={clsx('h-4 w-4 mr-2 flex-shrink-0', section.iconColor)} />
                    <span className="flex-1 text-left tracking-wide">{section.name}</span>
                    {isCollapsed ? (
                      <ChevronRight className="h-3.5 w-3.5 text-gray-400 group-hover:text-gray-600 transition-colors" />
                    ) : (
                      <ChevronDown className="h-3.5 w-3.5 text-gray-400 group-hover:text-gray-600 transition-colors" />
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
                                ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400 shadow-sm'
                                : 'text-gray-700 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white hover:bg-gray-50 dark:hover:bg-gray-700'
                            )}
                          >
                            <ItemIcon
                              className={clsx(
                                'mr-3 h-4.5 w-4.5 flex-shrink-0',
                                isActive ? 'text-blue-600' : 'text-gray-400'
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
                    <div className="mt-4 border-t border-gray-200" />
                  )}
                </div>
              );
            })}
          </nav>
        </div>

        {/* Main content */}
        <div className="flex-1 flex flex-col">
          {/* Header */}
          <header className="bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 shadow-sm">
            <div className="flex items-center justify-between px-8 py-4">
              <div className="flex-1">
                {/* Future: Add breadcrumbs or page title here */}
              </div>
              <div className="flex items-center space-x-4">
                <Link
                  to="/how-it-works"
                  className="flex items-center space-x-2 px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded-md transition-colors"
                >
                  <BookOpen className="h-5 w-5" />
                  <span>Documentation</span>
                </Link>

                {/* User Menu Dropdown */}
                <div className="relative">
                  <button
                    onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                    className="flex items-center space-x-3 px-3 py-2 rounded-md hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                  >
                    {user && <UserAvatar user={user} size="sm" />}
                    <div className="flex flex-col items-start">
                      <div className="flex items-center gap-1.5">
                        <span className="text-sm font-medium text-gray-900 dark:text-white">
                          {user?.username || 'User'}
                        </span>
                        {isAdmin && (
                          <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700">
                            <Shield className="h-3 w-3 text-amber-600 dark:text-amber-500" />
                            <span className="text-[10px] font-semibold text-amber-700 dark:text-amber-400 uppercase tracking-wide">
                              Admin
                            </span>
                          </span>
                        )}
                      </div>
                    </div>
                    <ChevronDown className={`h-4 w-4 text-gray-500 dark:text-gray-400 transition-transform ${isUserMenuOpen ? 'rotate-180' : ''}`} />
                  </button>

                  {isUserMenuOpen && (
                    <>
                      <div
                        className="fixed inset-0 z-10"
                        onClick={() => setIsUserMenuOpen(false)}
                      />
                      <div className="absolute right-0 mt-2 w-64 bg-white dark:bg-gray-800 rounded-md shadow-lg border border-gray-200 dark:border-gray-700 z-20 animate-fadeIn">
                        <div className="p-3 border-b border-gray-200 dark:border-gray-700">
                          <p className="text-sm font-medium text-gray-900 dark:text-white">
                            {user?.username || 'User'}
                          </p>
                          <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                            {user?.organizationName || 'No Organization'}
                          </p>
                        </div>

                        <div className="p-3 border-b border-gray-200 dark:border-gray-700">
                          <div className="text-xs font-semibold text-gray-500 dark:text-gray-400 mb-2">
                            Theme
                          </div>
                          <ThemeToggle />
                        </div>

                        <div className="p-2">
                          <Link
                            to="/profile"
                            onClick={() => setIsUserMenuOpen(false)}
                            className="w-full flex items-center px-3 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-md transition-colors"
                          >
                            Edit Profile
                          </Link>
                          <button
                            onClick={() => {
                              setIsUserMenuOpen(false);
                              logout();
                            }}
                            className="w-full flex items-center px-3 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-md transition-colors"
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
    </div>
  );
};