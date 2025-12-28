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
  Zap,
  BookOpen,
  Menu,
  X,
  Search,
} from 'lucide-react';
import { clsx } from 'clsx';
import { useAuth } from '../contexts/AuthContext';
import { SubmitIssueModal } from './SubmitIssueModal';
import { AvatarUploadModal } from './AvatarUploadModal';
import { UserAvatar } from './UserAvatar';
import { Footer } from './Footer';
import { useState, useEffect } from 'react';

interface LayoutProps {
  children: React.ReactNode;
}

interface NavigationItem {
  name: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  adminOnly: boolean;
  badge?: number;
  gradient?: string;
  category?: string;
}

const navigation: NavigationItem[] = [
  {
    name: 'Dashboard',
    href: '/',
    icon: Home,
    adminOnly: false,
    gradient: 'from-blue-500 to-cyan-500',
    category: 'main'
  },
  {
    name: 'Integration Wizard',
    href: '/integration-wizard',
    icon: Zap,
    adminOnly: false,
    gradient: 'from-purple-500 to-pink-500',
    category: 'main'
  },
  {
    name: 'Widget Dashboards',
    href: '/dashboards',
    icon: LayoutGrid,
    adminOnly: false,
    gradient: 'from-indigo-500 to-blue-500',
    category: 'monitoring'
  },
  {
    name: 'Devices',
    href: '/devices',
    icon: Cpu,
    adminOnly: false,
    gradient: 'from-green-500 to-emerald-500',
    category: 'monitoring'
  },
  {
    name: 'Analytics',
    href: '/analytics',
    icon: BarChart3,
    adminOnly: false,
    gradient: 'from-orange-500 to-amber-500',
    category: 'monitoring'
  },
  {
    name: 'Rules',
    href: '/rules',
    icon: Settings,
    adminOnly: false,
    gradient: 'from-cyan-500 to-teal-500',
    category: 'alerting'
  },
  {
    name: 'Alerts',
    href: '/alerts',
    icon: AlertTriangle,
    adminOnly: false,
    badge: 0,
    gradient: 'from-red-500 to-orange-500',
    category: 'alerting'
  },
  {
    name: 'Notifications',
    href: '/notifications',
    icon: Bell,
    adminOnly: false,
    badge: 0,
    gradient: 'from-yellow-500 to-orange-500',
    category: 'alerting'
  },
  {
    name: 'Device Groups',
    href: '/device-groups',
    icon: FolderTree,
    adminOnly: true,
    gradient: 'from-emerald-500 to-green-500',
    category: 'admin'
  },
  {
    name: 'Device Tags',
    href: '/device-tags',
    icon: Tag,
    adminOnly: true,
    gradient: 'from-pink-500 to-rose-500',
    category: 'admin'
  },
  {
    name: 'Events',
    href: '/events',
    icon: Clock,
    adminOnly: true,
    gradient: 'from-violet-500 to-purple-500',
    category: 'admin'
  },
  {
    name: 'Data Ingestion',
    href: '/data-ingestion',
    icon: Upload,
    adminOnly: true,
    gradient: 'from-blue-500 to-indigo-500',
    category: 'admin'
  },
  {
    name: 'Data Import',
    href: '/data-import',
    icon: FileUp,
    adminOnly: true,
    gradient: 'from-teal-500 to-cyan-500',
    category: 'admin'
  },
  {
    name: 'Data Export',
    href: '/data-export',
    icon: Download,
    adminOnly: true,
    gradient: 'from-lime-500 to-green-500',
    category: 'admin'
  },
  {
    name: 'Variables',
    href: '/variables',
    icon: Database,
    adminOnly: true,
    gradient: 'from-sky-500 to-blue-500',
    category: 'admin'
  },
];

const SIDEBAR_COLLAPSED_KEY = 'indcloud_sidebar_collapsed';

export const LayoutV3 = ({ children }: LayoutProps) => {
  const location = useLocation();
  const { user, logout, isAdmin, refreshUser } = useAuth();
  const [isIssueModalOpen, setIsIssueModalOpen] = useState(false);
  const [isAvatarModalOpen, setIsAvatarModalOpen] = useState(false);
  const [isCollapsed, setIsCollapsed] = useState(() => {
    const saved = localStorage.getItem(SIDEBAR_COLLAPSED_KEY);
    return saved ? JSON.parse(saved) : false;
  });
  const [searchQuery, setSearchQuery] = useState('');
  const [hoveredItem, setHoveredItem] = useState<string | null>(null);

  // Save collapse state to localStorage
  useEffect(() => {
    localStorage.setItem(SIDEBAR_COLLAPSED_KEY, JSON.stringify(isCollapsed));
  }, [isCollapsed]);

  // Keyboard shortcut: Ctrl+B to toggle
  useEffect(() => {
    const handleKeyPress = (e: KeyboardEvent) => {
      if (e.ctrlKey && e.key === 'b') {
        e.preventDefault();
        setIsCollapsed((prev: boolean) => !prev);
      }
    };
    window.addEventListener('keydown', handleKeyPress);
    return () => window.removeEventListener('keydown', handleKeyPress);
  }, []);

  // Responsive: Auto-collapse on smaller screens
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth < 768) {
        setIsCollapsed(true);
      }
    };
    handleResize();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  // Filter navigation based on user role
  const filteredNavigation = navigation.filter(item => !item.adminOnly || isAdmin);

  // Filter by search query
  const searchedNavigation = searchQuery
    ? filteredNavigation.filter(item =>
        item.name.toLowerCase().includes(searchQuery.toLowerCase())
      )
    : filteredNavigation;

  // Group navigation by category
  const groupedNavigation = searchedNavigation.reduce((acc, item) => {
    const category = item.category || 'other';
    if (!acc[category]) {
      acc[category] = [];
    }
    acc[category].push(item);
    return acc;
  }, {} as Record<string, NavigationItem[]>);

  const categoryOrder = ['main', 'monitoring', 'alerting', 'admin', 'other'];
  const categoryLabels: Record<string, string> = {
    main: 'Main',
    monitoring: 'Monitoring',
    alerting: 'Alerts & Events',
    admin: 'Administration',
    other: 'Other'
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="flex">
        {/* Collapsible Sidebar */}
        <div
          className={clsx(
            'bg-gradient-to-b from-slate-900 via-slate-800 to-slate-900 shadow-2xl flex flex-col h-screen transition-all duration-300 ease-in-out fixed left-0 top-0 z-50',
            isCollapsed ? 'w-[60px]' : 'w-[240px]'
          )}
          style={{
            boxShadow: '4px 0 24px rgba(0, 0, 0, 0.12), 0 0 48px rgba(59, 130, 246, 0.1)'
          }}
        >
          {/* Header with toggle */}
          <div className="flex-shrink-0 border-b border-slate-700/50 backdrop-blur-sm">
            <div className={clsx('p-4 flex items-center', isCollapsed ? 'justify-center' : 'justify-between')}>
              {!isCollapsed && (
                <div className="flex items-center space-x-2">
                  <Activity className="h-7 w-7 text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-cyan-400" style={{ filter: 'drop-shadow(0 0 8px rgba(59, 130, 246, 0.5))' }} />
                  <h1 className="text-lg font-bold text-white tracking-tight">Industrial Cloud</h1>
                </div>
              )}
              <button
                onClick={() => setIsCollapsed(!isCollapsed)}
                className={clsx(
                  'p-2 rounded-lg transition-all duration-200 hover:scale-110',
                  'bg-slate-800/50 hover:bg-slate-700/80 text-slate-300 hover:text-white',
                  'backdrop-blur-sm border border-slate-600/30 hover:border-slate-500/50',
                  'shadow-lg hover:shadow-xl'
                )}
                title={isCollapsed ? 'Expand sidebar (Ctrl+B)' : 'Collapse sidebar (Ctrl+B)'}
              >
                {isCollapsed ? <Menu className="h-5 w-5" /> : <X className="h-5 w-5" />}
              </button>
            </div>
          </div>

          {/* Search bar (when expanded) */}
          {!isCollapsed && (
            <div className="px-3 py-3 border-b border-slate-700/50">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
                <input
                  type="text"
                  placeholder="Search..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-9 pr-3 py-2 bg-slate-800/50 border border-slate-600/30 rounded-lg text-sm text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500/50 transition-all backdrop-blur-sm"
                />
              </div>
            </div>
          )}

          {/* Navigation */}
          <nav className="mt-2 flex-1 overflow-y-auto overflow-x-hidden custom-scrollbar">
            {categoryOrder.map((category) => {
              const items = groupedNavigation[category];
              if (!items || items.length === 0) return null;

              return (
                <div key={category} className="mb-1">
                  {/* Category label (only when expanded and not searching) */}
                  {!isCollapsed && !searchQuery && (
                    <div className="px-4 py-2 text-[10px] font-semibold text-slate-400 uppercase tracking-wider">
                      {categoryLabels[category]}
                    </div>
                  )}

                  {/* Navigation items */}
                  {items.map((item) => {
                    const isActive = location.pathname === item.href;
                    const Icon = item.icon;

                    return (
                      <div key={item.name} className="relative px-2 mb-1">
                        <Link
                          to={item.href}
                          className={clsx(
                            'group relative flex items-center rounded-lg transition-all duration-200',
                            isCollapsed ? 'justify-center p-3' : 'px-3 py-2.5',
                            isActive
                              ? 'bg-gradient-to-r from-blue-600 to-cyan-600 text-white shadow-lg shadow-blue-500/50'
                              : 'text-slate-300 hover:text-white hover:bg-slate-800/60 backdrop-blur-sm'
                          )}
                          onMouseEnter={() => setHoveredItem(item.name)}
                          onMouseLeave={() => setHoveredItem(null)}
                          style={
                            isActive
                              ? {
                                  borderLeft: '3px solid transparent',
                                  borderImage: 'linear-gradient(to bottom, #60a5fa, #22d3ee) 1',
                                }
                              : hoveredItem === item.name && !isCollapsed
                              ? {
                                  backdropFilter: 'blur(8px)',
                                  backgroundColor: 'rgba(51, 65, 85, 0.4)',
                                  boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1), inset 0 0 20px rgba(59, 130, 246, 0.1)'
                                }
                              : {}
                          }
                        >
                          {/* Icon with gradient */}
                          <div
                            className={clsx('flex-shrink-0', isCollapsed ? '' : 'mr-3')}
                            style={
                              !isActive
                                ? {
                                    filter: 'drop-shadow(0 0 6px rgba(59, 130, 246, 0.3))',
                                    WebkitTextStroke: isActive ? '0' : '1.5px currentColor'
                                  }
                                : {}
                            }
                          >
                            <Icon
                              className={clsx(
                                'h-5 w-5 transition-all duration-200',
                                isActive
                                  ? 'text-white drop-shadow-[0_0_8px_rgba(255,255,255,0.5)]'
                                  : `text-transparent bg-clip-text bg-gradient-to-br ${item.gradient}`
                              )}
                            />
                          </div>

                          {/* Text (when expanded) */}
                          {!isCollapsed && (
                            <>
                              <span className="flex-1 text-sm font-medium">{item.name}</span>
                              {/* Badge */}
                              {item.badge !== undefined && item.badge > 0 && (
                                <span className="flex-shrink-0 ml-2 px-2 py-0.5 text-xs font-bold text-white bg-gradient-to-r from-red-500 to-orange-500 rounded-full shadow-lg">
                                  {item.badge}
                                </span>
                              )}
                              {/* Admin indicator */}
                              {item.adminOnly && (
                                <Shield className="flex-shrink-0 ml-2 h-3.5 w-3.5 text-amber-400 opacity-70" />
                              )}
                            </>
                          )}

                          {/* Tooltip (when collapsed) */}
                          {isCollapsed && (
                            <div
                              className={clsx(
                                'absolute left-full ml-4 px-3 py-2 bg-slate-800 text-white text-sm font-medium rounded-lg shadow-xl pointer-events-none transition-opacity duration-200 whitespace-nowrap z-50 border border-slate-600',
                                hoveredItem === item.name ? 'opacity-100' : 'opacity-0'
                              )}
                              style={{
                                boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.3), 0 4px 6px -2px rgba(0, 0, 0, 0.2)'
                              }}
                            >
                              {item.name}
                              {item.badge !== undefined && item.badge > 0 && (
                                <span className="ml-2 px-1.5 py-0.5 text-xs font-bold text-white bg-red-500 rounded-full">
                                  {item.badge}
                                </span>
                              )}
                              {/* Tooltip arrow */}
                              <div className="absolute right-full top-1/2 transform -translate-y-1/2 border-8 border-transparent border-r-slate-800"></div>
                            </div>
                          )}

                          {/* Active indicator bar */}
                          {isActive && (
                            <div className="absolute left-0 top-0 bottom-0 w-1 bg-gradient-to-b from-blue-400 via-cyan-400 to-blue-400 rounded-r-full shadow-lg shadow-blue-500/50"></div>
                          )}
                        </Link>
                      </div>
                    );
                  })}
                </div>
              );
            })}
          </nav>

          {/* User Profile Section */}
          <div className="flex-shrink-0 border-t border-slate-700/50 backdrop-blur-sm">
            <div className={clsx('p-3', isCollapsed ? '' : 'px-4')}>
              {/* Profile card */}
              <div
                className={clsx(
                  'group cursor-pointer rounded-lg transition-all duration-200 mb-3',
                  isCollapsed
                    ? 'p-2 hover:bg-slate-800/60 flex justify-center'
                    : 'p-3 hover:bg-slate-800/60 backdrop-blur-sm border border-slate-700/30 hover:border-slate-600/50'
                )}
                onClick={() => setIsAvatarModalOpen(true)}
                style={
                  !isCollapsed
                    ? { boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.2), inset 0 0 20px rgba(59, 130, 246, 0.05)' }
                    : {}
                }
              >
                {isCollapsed ? (
                  // Collapsed: Just avatar
                  <div className="relative">
                    {user && <UserAvatar user={user} size="md" editable={false} />}
                    <div className="absolute -bottom-1 -right-1 w-3 h-3 bg-green-500 rounded-full border-2 border-slate-900"></div>
                  </div>
                ) : (
                  // Expanded: Full profile card
                  <div className="flex items-center space-x-3">
                    <div className="flex-shrink-0 relative">
                      {user && <UserAvatar user={user} size="lg" editable={false} />}
                      <div className="absolute -bottom-1 -right-1 w-3 h-3 bg-green-500 rounded-full border-2 border-slate-900"></div>
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-1.5 mb-1">
                        <p className="text-sm font-semibold text-white truncate">
                          {user?.username || 'User'}
                        </p>
                        {isAdmin && (
                          <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded bg-gradient-to-r from-amber-500 to-orange-500 flex-shrink-0 shadow-lg">
                            <Shield className="h-3 w-3 text-white" />
                            <span className="text-[9px] font-bold text-white uppercase tracking-wide">
                              Admin
                            </span>
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-slate-400 truncate" title={user?.organizationName}>
                        {user?.organizationName || 'No Organization'}
                      </p>
                    </div>
                  </div>
                )}
              </div>

              {/* Sign Out Button */}
              <button
                onClick={logout}
                className={clsx(
                  'w-full flex items-center justify-center rounded-lg text-sm font-medium text-white transition-all duration-200',
                  'bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-700 hover:to-rose-700',
                  'shadow-lg hover:shadow-xl hover:shadow-red-500/50',
                  'border border-red-500/30 hover:border-red-400/50',
                  isCollapsed ? 'p-2.5' : 'px-4 py-2.5'
                )}
              >
                <LogOut className={clsx('h-4 w-4', isCollapsed ? '' : 'mr-2')} />
                {!isCollapsed && 'Sign Out'}
              </button>
            </div>
          </div>
        </div>

        {/* Main content area (with dynamic left padding) */}
        <div
          className={clsx('flex-1 flex flex-col transition-all duration-300 ease-in-out')}
          style={{ marginLeft: isCollapsed ? '60px' : '240px' }}
        >
          {/* Header */}
          <header className="bg-white border-b border-gray-200 shadow-sm sticky top-0 z-40">
            <div className="flex items-center justify-between px-8 py-4">
              <div className="flex-1">
                {/* Breadcrumb placeholder */}
              </div>
              <div className="flex items-center space-x-4">
                <Link
                  to="/how-it-works"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center space-x-2 px-4 py-2 text-sm font-medium text-gray-700 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-all duration-200"
                >
                  <BookOpen className="h-5 w-5" />
                  <span>Documentation</span>
                </Link>
              </div>
            </div>
          </header>

          {/* Main content area */}
          <main className="flex-1 p-8 overflow-y-auto bg-gray-50">
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

      {/* Custom scrollbar styles */}
      <style>{`
        .custom-scrollbar::-webkit-scrollbar {
          width: 6px;
        }
        .custom-scrollbar::-webkit-scrollbar-track {
          background: rgba(51, 65, 85, 0.3);
          border-radius: 3px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb {
          background: rgba(148, 163, 184, 0.5);
          border-radius: 3px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover {
          background: rgba(148, 163, 184, 0.7);
        }
      `}</style>
    </div>
  );
};
