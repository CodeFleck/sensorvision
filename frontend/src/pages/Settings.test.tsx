import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Settings } from './Settings';
import { useAuth } from '../contexts/AuthContext';
import { useTheme } from '../contexts/ThemeContext';
import toast from 'react-hot-toast';

// Mock the contexts
vi.mock('../contexts/AuthContext');
vi.mock('../contexts/ThemeContext');

// Mock react-hot-toast
vi.mock('react-hot-toast', () => ({
  default: {
    error: vi.fn(),
    success: vi.fn(),
  },
}));

// Mock fetch
const mockFetch = vi.fn();
global.fetch = mockFetch;

describe('Settings', () => {
  const mockUser = {
    id: 1,
    username: 'testuser',
    email: 'test@example.com',
    firstName: 'Test',
    lastName: 'User',
    organizationId: 1,
    organizationName: 'Test Org',
    roles: ['ROLE_USER'],
    enabled: true,
    emailNotificationsEnabled: true,
  };

  const mockDeveloperUser = {
    ...mockUser,
    roles: ['ROLE_USER', 'ROLE_DEVELOPER'],
  };

  const mockSetTheme = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();

    // Default mocks
    vi.mocked(useAuth).mockReturnValue({
      user: mockUser,
      hasRole: (role: string) => mockUser.roles.includes(role),
      isAdmin: false,
      isAuthenticated: true,
      login: vi.fn(),
      logout: vi.fn(),
      register: vi.fn(),
      refreshUser: vi.fn(),
    } as any);

    vi.mocked(useTheme).mockReturnValue({
      theme: 'system',
      effectiveTheme: 'light',
      setTheme: mockSetTheme,
      isLoading: false,
    });

    mockFetch.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({}),
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('Rendering', () => {
    it('should render page title and description', () => {
      render(<Settings />);

      expect(screen.getByText('Settings')).toBeInTheDocument();
      expect(screen.getByText('Manage your preferences and account settings')).toBeInTheDocument();
    });

    it('should render all main sections', () => {
      render(<Settings />);

      expect(screen.getByText('Appearance')).toBeInTheDocument();
      expect(screen.getByText('Notifications')).toBeInTheDocument();
      expect(screen.getByText('Account Information')).toBeInTheDocument();
    });

    it('should display user account information', () => {
      render(<Settings />);

      expect(screen.getByText('testuser')).toBeInTheDocument();
      expect(screen.getByText('test@example.com')).toBeInTheDocument();
      expect(screen.getByText('Test Org')).toBeInTheDocument();
    });

    it('should display user roles as badges', () => {
      render(<Settings />);

      expect(screen.getByText('USER')).toBeInTheDocument();
    });
  });

  describe('Theme Selection', () => {
    it('should render all five theme options', () => {
      render(<Settings />);

      expect(screen.getByText('System')).toBeInTheDocument();
      expect(screen.getByText('Light')).toBeInTheDocument();
      expect(screen.getByText('Dark')).toBeInTheDocument();
      expect(screen.getByText('Dark Dimmed')).toBeInTheDocument();
      expect(screen.getByText('High Contrast')).toBeInTheDocument();
    });

    it('should call setTheme when clicking a theme option', async () => {
      render(<Settings />);

      const darkButton = screen.getByText('Dark').closest('button');
      fireEvent.click(darkButton!);

      await waitFor(() => {
        expect(mockSetTheme).toHaveBeenCalledWith('dark');
      });
    });

    it('should show success toast when theme changes', async () => {
      mockSetTheme.mockResolvedValue(undefined);
      render(<Settings />);

      const lightButton = screen.getByText('Light').closest('button');
      fireEvent.click(lightButton!);

      await waitFor(() => {
        expect(toast.success).toHaveBeenCalledWith('Theme changed to Light');
      });
    });

    it('should show error toast when theme change fails', async () => {
      mockSetTheme.mockRejectedValue(new Error('Failed'));
      render(<Settings />);

      const darkButton = screen.getByText('Dark').closest('button');
      fireEvent.click(darkButton!);

      await waitFor(() => {
        expect(toast.error).toHaveBeenCalledWith('Failed to update theme preference');
      });
    });

    it('should disable theme buttons while loading', () => {
      vi.mocked(useTheme).mockReturnValue({
        theme: 'system',
        effectiveTheme: 'light',
        setTheme: mockSetTheme,
        isLoading: true,
      });

      render(<Settings />);

      const systemButton = screen.getByText('System').closest('button');
      expect(systemButton).toBeDisabled();
    });
  });

  describe('Email Notifications Toggle', () => {
    it('should render email notifications toggle', () => {
      render(<Settings />);

      expect(screen.getByText('Email Notifications')).toBeInTheDocument();
      expect(screen.getByText('Receive alerts and updates via email')).toBeInTheDocument();
    });

    it('should toggle email notifications and call API', async () => {
      localStorage.setItem('accessToken', 'test-token');
      render(<Settings />);

      // Find the toggle in the notifications section
      const toggles = screen.getAllByRole('button').filter(
        btn => btn.className.includes('rounded-full') && btn.className.includes('h-6')
      );
      const emailToggle = toggles[0];

      fireEvent.click(emailToggle);

      await waitFor(() => {
        expect(mockFetch).toHaveBeenCalledWith(
          '/api/v1/auth/preferences',
          expect.objectContaining({
            method: 'PUT',
            body: JSON.stringify({ emailNotificationsEnabled: false }),
          })
        );
      });
    });

    it('should show success toast when notifications preference is saved', async () => {
      localStorage.setItem('accessToken', 'test-token');
      render(<Settings />);

      const toggles = screen.getAllByRole('button').filter(
        btn => btn.className.includes('rounded-full') && btn.className.includes('h-6')
      );
      const emailToggle = toggles[0];

      fireEvent.click(emailToggle);

      await waitFor(() => {
        expect(toast.success).toHaveBeenCalledWith('Email notifications disabled');
      });
    });

    it('should show error toast when API call fails', async () => {
      mockFetch.mockResolvedValue({ ok: false });
      localStorage.setItem('accessToken', 'test-token');
      render(<Settings />);

      const toggles = screen.getAllByRole('button').filter(
        btn => btn.className.includes('rounded-full') && btn.className.includes('h-6')
      );
      const emailToggle = toggles[0];

      fireEvent.click(emailToggle);

      await waitFor(() => {
        expect(toast.error).toHaveBeenCalledWith('Failed to update notification preferences');
      });
    });
  });

  describe('Developer Tools Section', () => {
    it('should NOT show Developer Tools section for regular users', () => {
      render(<Settings />);

      // Regular users should NOT see Developer Tools or System Logs toggle
      expect(screen.queryByText('Developer Tools')).not.toBeInTheDocument();
    });

    it('should show Developer Tools section for users with ROLE_DEVELOPER', () => {
      vi.mocked(useAuth).mockReturnValue({
        user: mockDeveloperUser,
        hasRole: (role: string) => mockDeveloperUser.roles.includes(role),
        isAdmin: false,
        isAuthenticated: true,
        login: vi.fn(),
        logout: vi.fn(),
        register: vi.fn(),
        refreshUser: vi.fn(),
      } as any);

      render(<Settings />);

      expect(screen.getByText('Developer Tools')).toBeInTheDocument();
      // "System Logs" appears as a label within Developer Tools
      expect(screen.getByText('System Logs')).toBeInTheDocument();
    });

    it('should toggle logs enabled and save to localStorage for developers', () => {
      vi.mocked(useAuth).mockReturnValue({
        user: mockDeveloperUser,
        hasRole: (role: string) => mockDeveloperUser.roles.includes(role),
        isAdmin: false,
        isAuthenticated: true,
        login: vi.fn(),
        logout: vi.fn(),
        register: vi.fn(),
        refreshUser: vi.fn(),
      } as any);

      render(<Settings />);

      // Find the logs toggle (second toggle button)
      const toggles = screen.getAllByRole('button').filter(
        btn => btn.className.includes('rounded-full') && btn.className.includes('h-6')
      );
      const logsToggle = toggles[1];

      fireEvent.click(logsToggle);

      expect(localStorage.getItem('logsEnabled_1')).toBe('false');
      expect(toast.success).toHaveBeenCalledWith('System logs disabled');
    });

    it('should load logs preference from localStorage for developers', () => {
      localStorage.setItem('logsEnabled_1', 'false');

      vi.mocked(useAuth).mockReturnValue({
        user: mockDeveloperUser,
        hasRole: (role: string) => mockDeveloperUser.roles.includes(role),
        isAdmin: false,
        isAuthenticated: true,
        login: vi.fn(),
        logout: vi.fn(),
        register: vi.fn(),
        refreshUser: vi.fn(),
      } as any);

      render(<Settings />);

      // The toggle should be in the "off" state (bg-gray-200)
      const toggles = screen.getAllByRole('button').filter(
        btn => btn.className.includes('rounded-full') && btn.className.includes('h-6')
      );
      const logsToggle = toggles[1];

      expect(logsToggle.className).toContain('bg-gray-200');
    });
  });

  describe('User Preferences Loading', () => {
    it('should load email notifications preference from user object', () => {
      const userWithNotificationsDisabled = {
        ...mockUser,
        emailNotificationsEnabled: false,
      };

      vi.mocked(useAuth).mockReturnValue({
        user: userWithNotificationsDisabled,
        hasRole: (role: string) => mockUser.roles.includes(role),
        isAdmin: false,
        isAuthenticated: true,
        login: vi.fn(),
        logout: vi.fn(),
        register: vi.fn(),
        refreshUser: vi.fn(),
      } as any);

      render(<Settings />);

      // Find the email toggle
      const toggles = screen.getAllByRole('button').filter(
        btn => btn.className.includes('rounded-full') && btn.className.includes('h-6')
      );
      const emailToggle = toggles[0];

      // Toggle should be in "off" state
      expect(emailToggle.className).toContain('bg-gray-200');
    });

    it('should default email notifications to true if not set', () => {
      const userWithoutPreference = {
        ...mockUser,
        emailNotificationsEnabled: undefined,
      };

      vi.mocked(useAuth).mockReturnValue({
        user: userWithoutPreference,
        hasRole: (role: string) => mockUser.roles.includes(role),
        isAdmin: false,
        isAuthenticated: true,
        login: vi.fn(),
        logout: vi.fn(),
        register: vi.fn(),
        refreshUser: vi.fn(),
      } as any);

      render(<Settings />);

      // Find the email toggle
      const toggles = screen.getAllByRole('button').filter(
        btn => btn.className.includes('rounded-full') && btn.className.includes('h-6')
      );
      const emailToggle = toggles[0];

      // Toggle should be in "on" state (bg-blue-600)
      expect(emailToggle.className).toContain('bg-blue-600');
    });
  });

  describe('Edge Cases', () => {
    it('should handle null user gracefully', () => {
      vi.mocked(useAuth).mockReturnValue({
        user: null,
        hasRole: () => false,
        isAdmin: false,
        isAuthenticated: false,
        login: vi.fn(),
        logout: vi.fn(),
        register: vi.fn(),
        refreshUser: vi.fn(),
      } as any);

      render(<Settings />);

      // Should still render the page without crashing
      expect(screen.getByText('Settings')).toBeInTheDocument();
    });

    it('should handle user with no roles', () => {
      const userWithNoRoles = {
        ...mockUser,
        roles: [],
      };

      vi.mocked(useAuth).mockReturnValue({
        user: userWithNoRoles,
        hasRole: () => false,
        isAdmin: false,
        isAuthenticated: true,
        login: vi.fn(),
        logout: vi.fn(),
        register: vi.fn(),
        refreshUser: vi.fn(),
      } as any);

      render(<Settings />);

      // Should render without crashing
      expect(screen.getByText('Settings')).toBeInTheDocument();
    });

    it('should handle user with no organization', () => {
      const userWithNoOrg = {
        ...mockUser,
        organizationName: null,
      };

      vi.mocked(useAuth).mockReturnValue({
        user: userWithNoOrg,
        hasRole: (role: string) => mockUser.roles.includes(role),
        isAdmin: false,
        isAuthenticated: true,
        login: vi.fn(),
        logout: vi.fn(),
        register: vi.fn(),
        refreshUser: vi.fn(),
      } as any);

      render(<Settings />);

      expect(screen.getByText('None')).toBeInTheDocument();
    });
  });
});
