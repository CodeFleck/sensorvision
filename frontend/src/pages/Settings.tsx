import { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import {
  Settings as SettingsIcon,
  Palette,
  Bell,
  Monitor,
  Moon,
  Sun,
  Check,
  Loader2,
  Mail,
  ScrollText,
} from 'lucide-react';
import toast from 'react-hot-toast';

type ThemeOption = 'light' | 'dark' | 'dark-dimmed' | 'dark-high-contrast' | 'system';

interface ThemeChoice {
  value: ThemeOption;
  label: string;
  description: string;
  icon: React.ReactNode;
}

const themeOptions: ThemeChoice[] = [
  {
    value: 'system',
    label: 'System',
    description: 'Follows your device settings',
    icon: <Monitor className="h-5 w-5" />,
  },
  {
    value: 'light',
    label: 'Light',
    description: 'Light background with dark text',
    icon: <Sun className="h-5 w-5" />,
  },
  {
    value: 'dark',
    label: 'Dark',
    description: 'Dark background with light text',
    icon: <Moon className="h-5 w-5" />,
  },
  {
    value: 'dark-dimmed',
    label: 'Dark Dimmed',
    description: 'Softer dark theme for less contrast',
    icon: <Moon className="h-5 w-5 opacity-70" />,
  },
  {
    value: 'dark-high-contrast',
    label: 'High Contrast',
    description: 'Maximum contrast for accessibility',
    icon: <Moon className="h-5 w-5 text-white" />,
  },
];

export const Settings = () => {
  const { user, hasRole } = useAuth();
  const { theme, setTheme, isLoading: themeLoading } = useTheme();
  const [emailNotifications, setEmailNotifications] = useState(true);
  const [logsEnabled, setLogsEnabled] = useState(true);
  const [savingNotifications, setSavingNotifications] = useState(false);

  const isDeveloper = hasRole('ROLE_DEVELOPER');

  // Load user preferences
  useEffect(() => {
    if (user?.id) {
      // Email notifications preference from user object if available
      setEmailNotifications(user.emailNotificationsEnabled ?? true);
      // Logs preference from localStorage (per-user setting)
      const storedLogsEnabled = localStorage.getItem(`logsEnabled_${user.id}`);
      if (storedLogsEnabled !== null) {
        setLogsEnabled(storedLogsEnabled === 'true');
      }
    }
  }, [user?.id, user?.emailNotificationsEnabled]);

  const handleThemeChange = async (newTheme: ThemeOption) => {
    try {
      await setTheme(newTheme);
      toast.success(`Theme changed to ${themeOptions.find(t => t.value === newTheme)?.label}`);
    } catch (error) {
      toast.error('Failed to update theme preference');
    }
  };

  const handleEmailNotificationsChange = async (enabled: boolean) => {
    setSavingNotifications(true);
    try {
      const response = await fetch('/api/v1/auth/preferences', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
        },
        body: JSON.stringify({ emailNotificationsEnabled: enabled }),
      });

      if (!response.ok) {
        throw new Error('Failed to update notification preferences');
      }

      setEmailNotifications(enabled);
      toast.success(`Email notifications ${enabled ? 'enabled' : 'disabled'}`);
    } catch (error) {
      toast.error('Failed to update notification preferences');
    } finally {
      setSavingNotifications(false);
    }
  };

  const handleLogsEnabledChange = (enabled: boolean) => {
    // Store in localStorage per user (synchronous operation)
    if (user) {
      localStorage.setItem(`logsEnabled_${user.id}`, String(enabled));
    }
    setLogsEnabled(enabled);
    toast.success(`System logs ${enabled ? 'enabled' : 'disabled'}`);
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3 mb-8">
        <div className="p-3 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
          <SettingsIcon className="h-6 w-6 text-blue-600 dark:text-blue-400" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Settings</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Manage your preferences and account settings
          </p>
        </div>
      </div>

      {/* Theme Preferences */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
        <div className="flex items-center gap-3 mb-6">
          <Palette className="h-5 w-5 text-purple-600 dark:text-purple-400" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Appearance</h2>
        </div>

        <div className="space-y-4">
          <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
            Choose how SensorVision looks to you. Select a theme that works best for your environment.
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
            {themeOptions.map((option) => {
              const isSelected = theme === option.value;
              return (
                <button
                  key={option.value}
                  onClick={() => handleThemeChange(option.value)}
                  disabled={themeLoading}
                  className={`relative flex flex-col items-start p-4 rounded-lg border-2 transition-all ${
                    isSelected
                      ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                      : 'border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700/50'
                  }`}
                >
                  {isSelected && (
                    <div className="absolute top-2 right-2">
                      <Check className="h-4 w-4 text-blue-500" />
                    </div>
                  )}
                  <div className={`p-2 rounded-md mb-2 ${
                    isSelected
                      ? 'bg-blue-100 dark:bg-blue-800/50 text-blue-600 dark:text-blue-400'
                      : 'bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400'
                  }`}>
                    {option.icon}
                  </div>
                  <span className={`text-sm font-medium ${
                    isSelected
                      ? 'text-blue-700 dark:text-blue-300'
                      : 'text-gray-900 dark:text-white'
                  }`}>
                    {option.label}
                  </span>
                  <span className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                    {option.description}
                  </span>
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Notification Preferences */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
        <div className="flex items-center gap-3 mb-6">
          <Bell className="h-5 w-5 text-orange-600 dark:text-orange-400" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Notifications</h2>
        </div>

        <div className="space-y-4">
          <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
            Configure how you want to receive notifications from SensorVision.
          </p>

          <div className="flex items-center justify-between p-4 rounded-lg border border-gray-200 dark:border-gray-700">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-gray-100 dark:bg-gray-700 rounded-md">
                <Mail className="h-5 w-5 text-gray-600 dark:text-gray-400" />
              </div>
              <div>
                <p className="text-sm font-medium text-gray-900 dark:text-white">
                  Email Notifications
                </p>
                <p className="text-xs text-gray-500 dark:text-gray-400">
                  Receive alerts and updates via email
                </p>
              </div>
            </div>
            <button
              onClick={() => handleEmailNotificationsChange(!emailNotifications)}
              disabled={savingNotifications}
              className={`relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 ${
                emailNotifications ? 'bg-blue-600' : 'bg-gray-200 dark:bg-gray-600'
              }`}
            >
              <span
                className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                  emailNotifications ? 'translate-x-5' : 'translate-x-0'
                }`}
              >
                {savingNotifications && (
                  <Loader2 className="h-5 w-5 animate-spin text-blue-600" />
                )}
              </span>
            </button>
          </div>
        </div>
      </div>

      {/* Developer Tools Settings - Only visible to users with ROLE_DEVELOPER */}
      {isDeveloper && (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
          <div className="flex items-center gap-3 mb-6">
            <ScrollText className="h-5 w-5 text-cyan-600 dark:text-cyan-400" />
            <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Developer Tools</h2>
          </div>

          <div className="space-y-4">
            <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
              Configure developer-specific features and tools.
            </p>

            <div className="flex items-center justify-between p-4 rounded-lg border border-gray-200 dark:border-gray-700">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-cyan-100 dark:bg-cyan-900/30 rounded-md">
                  <ScrollText className="h-5 w-5 text-cyan-600 dark:text-cyan-400" />
                </div>
                <div>
                  <p className="text-sm font-medium text-gray-900 dark:text-white">
                    System Logs
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    Enable real-time system log streaming
                  </p>
                </div>
              </div>
              <button
                onClick={() => handleLogsEnabledChange(!logsEnabled)}
                className={`relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-cyan-500 focus:ring-offset-2 ${
                  logsEnabled ? 'bg-cyan-600' : 'bg-gray-200 dark:bg-gray-600'
                }`}
              >
                <span
                  className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                    logsEnabled ? 'translate-x-5' : 'translate-x-0'
                  }`}
                />
              </button>
            </div>

            <p className="text-xs text-gray-500 dark:text-gray-400 mt-2">
              When enabled, you can view real-time logs from the System Logs page.
              Disabling this will stop the WebSocket connection to the log stream.
            </p>
          </div>
        </div>
      )}

      {/* Account Info */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-6">
        <div className="flex items-center gap-3 mb-6">
          <SettingsIcon className="h-5 w-5 text-gray-600 dark:text-gray-400" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Account Information</h2>
        </div>

        <div className="space-y-3">
          <div className="flex justify-between items-center py-2 border-b border-gray-200 dark:border-gray-700">
            <span className="text-sm text-gray-600 dark:text-gray-400">Username</span>
            <span className="text-sm font-medium text-gray-900 dark:text-white">{user?.username}</span>
          </div>
          <div className="flex justify-between items-center py-2 border-b border-gray-200 dark:border-gray-700">
            <span className="text-sm text-gray-600 dark:text-gray-400">Email</span>
            <span className="text-sm font-medium text-gray-900 dark:text-white">{user?.email}</span>
          </div>
          <div className="flex justify-between items-center py-2 border-b border-gray-200 dark:border-gray-700">
            <span className="text-sm text-gray-600 dark:text-gray-400">Organization</span>
            <span className="text-sm font-medium text-gray-900 dark:text-white">
              {user?.organizationName || 'None'}
            </span>
          </div>
          <div className="flex justify-between items-center py-2">
            <span className="text-sm text-gray-600 dark:text-gray-400">Roles</span>
            <div className="flex gap-2">
              {user?.roles?.map((role) => (
                <span
                  key={role}
                  className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-400"
                >
                  {role.replace('ROLE_', '')}
                </span>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Settings;
