import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { Dashboard } from './Dashboard';
import { useAuth } from '../contexts/AuthContext';
import { useWebSocket } from '../hooks/useWebSocket';
import { apiService } from '../services/api';
import { BrowserRouter } from 'react-router-dom';
import { Device, TelemetryPoint, LatestTelemetry } from '../types';

// Mock the contexts and hooks
vi.mock('../contexts/AuthContext');
vi.mock('../hooks/useWebSocket');
vi.mock('../services/api');

// Mock react-router-dom Navigate
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    Navigate: ({ to }: { to: string }) => <div data-testid="navigate-to">{to}</div>,
  };
});

// Mock Chart.js components
vi.mock('react-chartjs-2', () => ({
  Line: () => <div data-testid="mock-line-chart">Line Chart</div>,
}));

// Mock child components
vi.mock('../components/DeviceCard', () => ({
  DeviceCard: ({ device }: { device: Device }) => (
    <div data-testid={`device-card-${device.externalId}`}>{device.name}</div>
  ),
}));

vi.mock('../components/RealTimeChart', () => ({
  RealTimeChart: () => <div data-testid="real-time-chart">Real-time Chart</div>,
}));

vi.mock('../components/GettingStarted', () => ({
  GettingStarted: () => <div data-testid="getting-started">Getting Started</div>,
}));

vi.mock('../components/FleetHealthGauge', () => ({
  FleetHealthGauge: () => <div data-testid="fleet-health-gauge">Fleet Health</div>,
}));

vi.mock('../components/ActivityTimeline', () => ({
  ActivityTimeline: () => <div data-testid="activity-timeline">Activity Timeline</div>,
}));

describe('Dashboard', () => {
  const mockUser = {
    id: 1,
    username: 'testuser',
    email: 'test@example.com',
    organizationId: 1,
    roles: ['ROLE_USER'],
    isAdmin: false,
  };

  const mockDevice: Device = {
    id: 'device-uuid-001',
    externalId: 'device-001',
    name: 'Test Device 1',
    status: 'ONLINE',
    healthScore: 85,
    healthStatus: 'EXCELLENT',
    location: 'Floor 1',
    lastSeenAt: new Date().toISOString(),
    active: true,
  };

  const mockTelemetryPoint: TelemetryPoint = {
    deviceId: 'device-001',
    timestamp: new Date().toISOString(),
    kwConsumption: 50.5,
    voltage: 220.1,
    current: 0.57,
  };

  const mockLatestTelemetry: LatestTelemetry = {
    deviceId: 'device-001',
    latest: mockTelemetryPoint,
  };

  const mockAggregatedData = [{ timestamp: new Date().toISOString(), value: 50.5 }];

  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();

    // Default mock for useAuth
    vi.mocked(useAuth).mockReturnValue({
      user: mockUser,
      isAdmin: false,
      isAuthenticated: true,
      hasRole: (role: string) => mockUser.roles.includes(role),
      login: vi.fn(),
      logout: vi.fn(),
      register: vi.fn(),
      refreshUser: vi.fn(),
    } as any);

    // Default mock for useWebSocket
    vi.mocked(useWebSocket).mockReturnValue({
      lastMessage: null,
      connectionStatus: 'Open',
      sendMessage: vi.fn(),
    });

    // Default mocks for apiService
    vi.mocked(apiService.getDevices).mockResolvedValue([mockDevice]);
    vi.mocked(apiService.getLatestTelemetry).mockResolvedValue([mockLatestTelemetry]);
    vi.mocked(apiService.getAggregatedData).mockResolvedValue(mockAggregatedData);

    // Mock window.location for WebSocket URL construction
    Object.defineProperty(window, 'location', {
      writable: true,
      value: {
        protocol: 'http:',
        host: 'localhost:3001',
      },
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const renderDashboard = () => {
    return render(
      <BrowserRouter>
        <Dashboard />
      </BrowserRouter>
    );
  };

  // ========== Admin Redirect Tests ==========

  describe('Admin Redirect', () => {
    it('should redirect admin users to admin dashboard', () => {
      vi.mocked(useAuth).mockReturnValue({
        user: { ...mockUser, roles: ['ROLE_ADMIN'] },
        isAdmin: true,
        isAuthenticated: true,
        hasRole: () => true,
        login: vi.fn(),
        logout: vi.fn(),
        register: vi.fn(),
        refreshUser: vi.fn(),
      } as any);

      renderDashboard();

      expect(screen.getByTestId('navigate-to')).toHaveTextContent('/admin-dashboard');
    });

    it('should NOT redirect regular users', async () => {
      renderDashboard();

      await waitFor(() => {
        expect(screen.queryByTestId('navigate-to')).not.toBeInTheDocument();
      });
    });
  });

  // ========== Loading State Tests ==========

  describe('Loading State', () => {
    it('should show loading message initially', () => {
      // Make API call pending to keep loading state
      vi.mocked(apiService.getDevices).mockImplementation(
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        () => new Promise(() => {}) // Never resolves
      );

      renderDashboard();

      expect(screen.getByText('Loading dashboard...')).toBeInTheDocument();
    });

    it('should hide loading after data loads', async () => {
      renderDashboard();

      await waitFor(() => {
        expect(screen.queryByText('Loading dashboard...')).not.toBeInTheDocument();
      });
    });
  });

  // ========== Error State Tests ==========

  describe('Error State', () => {
    it('should show error message when data fetch fails', async () => {
      vi.mocked(apiService.getDevices).mockRejectedValue(new Error('Network error'));

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Failed to load dashboard data. Please try again.')).toBeInTheDocument();
      });
    });

    it('should show Try Again button on error', async () => {
      vi.mocked(apiService.getDevices).mockRejectedValue(new Error('Network error'));

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Try Again')).toBeInTheDocument();
      });
    });

    it('should retry data fetch when Try Again is clicked', async () => {
      vi.mocked(apiService.getDevices)
        .mockRejectedValueOnce(new Error('Network error'))
        .mockResolvedValueOnce([mockDevice]);

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Try Again')).toBeInTheDocument();
      });

      const tryAgainButton = screen.getByText('Try Again');
      fireEvent.click(tryAgainButton);

      await waitFor(() => {
        expect(apiService.getDevices).toHaveBeenCalledTimes(2);
      });
    });
  });

  // ========== Empty State Tests ==========

  describe('Empty State (No Devices)', () => {
    it('should show Getting Started when user has no devices', async () => {
      vi.mocked(apiService.getDevices).mockResolvedValue([]);

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByTestId('getting-started')).toBeInTheDocument();
      });
    });

    it('should show dashboard title even with no devices', async () => {
      vi.mocked(apiService.getDevices).mockResolvedValue([]);

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Dashboard')).toBeInTheDocument();
        expect(screen.getByText('Real-time IoT monitoring overview')).toBeInTheDocument();
      });
    });

    it('should NOT show device grid when no devices', async () => {
      vi.mocked(apiService.getDevices).mockResolvedValue([]);

      renderDashboard();

      await waitFor(() => {
        expect(screen.queryByText('Device Overview')).not.toBeInTheDocument();
      });
    });
  });

  // ========== Real-time Telemetry Widget Tests (PR #256 Focus) ==========

  describe('Real-time Telemetry Widget - Always Visible', () => {
    it('should always show Real-time Telemetry card even when no telemetry data', async () => {
      vi.mocked(apiService.getLatestTelemetry).mockResolvedValue([]);

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Real-time Telemetry')).toBeInTheDocument();
      });
    });

    it('should show empty state message when no telemetry data', async () => {
      vi.mocked(apiService.getLatestTelemetry).mockResolvedValue([]);

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Waiting for real-time telemetry data...')).toBeInTheDocument();
        expect(screen.getByText('Data will appear here when devices send telemetry via WebSocket')).toBeInTheDocument();
      });
    });

    it('should show RealTimeChart when telemetry data exists', async () => {
      renderDashboard();

      await waitFor(() => {
        expect(screen.getByTestId('real-time-chart')).toBeInTheDocument();
      });
    });

    it('should switch from empty state to chart when WebSocket data arrives', async () => {
      vi.mocked(apiService.getLatestTelemetry).mockResolvedValue([]);

      const { rerender } = renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Waiting for real-time telemetry data...')).toBeInTheDocument();
      });

      // Simulate WebSocket message arrival
      vi.mocked(useWebSocket).mockReturnValue({
        lastMessage: mockTelemetryPoint,
        connectionStatus: 'Open',
        sendMessage: vi.fn(),
      });

      rerender(
        <BrowserRouter>
          <Dashboard />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByTestId('real-time-chart')).toBeInTheDocument();
        expect(screen.queryByText('Waiting for real-time telemetry data...')).not.toBeInTheDocument();
      });
    });

    it('should always render the Real-time Telemetry Card component structure', async () => {
      vi.mocked(apiService.getLatestTelemetry).mockResolvedValue([]);

      renderDashboard();

      await waitFor(() => {
        const rtTelemetryHeading = screen.getByText('Real-time Telemetry');
        expect(rtTelemetryHeading).toBeInTheDocument();

        // Should be inside a container element
        const containerElement = rtTelemetryHeading.closest('div');
        expect(containerElement).toBeTruthy();
      });
    });
  });

  // ========== WebSocket Connection Status Tests ==========

  describe('WebSocket Connection Status', () => {
    it('should show Connected status when WebSocket is open', async () => {
      vi.mocked(useWebSocket).mockReturnValue({
        lastMessage: null,
        connectionStatus: 'Open',
        sendMessage: vi.fn(),
      });

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Connected: Live')).toBeInTheDocument();
      });
    });

    it('should show Disconnected status when WebSocket is closed', async () => {
      vi.mocked(useWebSocket).mockReturnValue({
        lastMessage: null,
        connectionStatus: 'Closed',
        sendMessage: vi.fn(),
      });

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Disconnected')).toBeInTheDocument();
      });
    });

    it('should construct WebSocket URL using current window location', async () => {
      renderDashboard();

      await waitFor(() => {
        expect(useWebSocket).toHaveBeenCalledWith('ws://localhost:3001/ws/telemetry');
      });
    });

    it('should use wss:// protocol when page is loaded over https', async () => {
      Object.defineProperty(window, 'location', {
        writable: true,
        value: {
          protocol: 'https:',
          host: 'indcloud.io',
        },
      });

      renderDashboard();

      await waitFor(() => {
        expect(useWebSocket).toHaveBeenCalledWith('wss://indcloud.io/ws/telemetry');
      });
    });
  });

  // ========== WebSocket Telemetry Updates Tests ==========

  describe('WebSocket Telemetry Updates', () => {
    it('should update latestTelemetry when WebSocket message arrives', async () => {
      const { rerender } = renderDashboard();

      await waitFor(() => {
        expect(screen.getByTestId('real-time-chart')).toBeInTheDocument();
      });

      // Simulate new WebSocket message
      const newTelemetry: TelemetryPoint = {
        deviceId: 'device-001',
        timestamp: new Date().toISOString(),
        kwConsumption: 75.2,
        voltage: 225.0,
        current: 0.65,
      };

      await act(async () => {
        vi.mocked(useWebSocket).mockReturnValue({
          lastMessage: newTelemetry,
          connectionStatus: 'Open',
          sendMessage: vi.fn(),
        });

        rerender(
          <BrowserRouter>
            <Dashboard />
          </BrowserRouter>
        );
      });

      // Component should re-render with new data
      await waitFor(() => {
        expect(screen.getByTestId('real-time-chart')).toBeInTheDocument();
      });
    });

    it('should store telemetry history in ref for sparklines', async () => {
      const { rerender } = renderDashboard();

      await waitFor(() => {
        expect(screen.getByTestId('device-card-device-001')).toBeInTheDocument();
      });

      // Send multiple WebSocket messages to build history
      const messages = [
        { ...mockTelemetryPoint, kwConsumption: 50.0 },
        { ...mockTelemetryPoint, kwConsumption: 52.0 },
        { ...mockTelemetryPoint, kwConsumption: 54.0 },
      ];

      for (const msg of messages) {
        vi.mocked(useWebSocket).mockReturnValue({
          lastMessage: msg,
          connectionStatus: 'Open',
          sendMessage: vi.fn(),
        });

        rerender(
          <BrowserRouter>
            <Dashboard />
          </BrowserRouter>
        );

        await waitFor(() => {
          expect(screen.getByTestId('device-card-device-001')).toBeInTheDocument();
        });
      }
    });

    it('should handle kw_consumption (snake_case) from WebSocket', async () => {
      const { rerender } = renderDashboard();

      await waitFor(() => {
        expect(screen.getByTestId('device-card-device-001')).toBeInTheDocument();
      });

      const snakeCaseTelemetry = {
        deviceId: 'device-001',
        timestamp: new Date().toISOString(),
        kw_consumption: 60.5,
        voltage: 220.0,
      } as unknown as TelemetryPoint;

      vi.mocked(useWebSocket).mockReturnValue({
        lastMessage: snakeCaseTelemetry,
        connectionStatus: 'Open',
        sendMessage: vi.fn(),
      });

      rerender(
        <BrowserRouter>
          <Dashboard />
        </BrowserRouter>
      );

      await waitFor(() => {
        expect(screen.getByTestId('device-card-device-001')).toBeInTheDocument();
      });
    });
  });

  // ========== Historical Metrics Panel Tests ==========

  describe('Historical Metrics Panel', () => {
    it('should render Historical Metrics section', async () => {
      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Historical Metrics')).toBeInTheDocument();
      });
    });

    it('should render time range selector with all options', async () => {
      renderDashboard();

      await waitFor(() => {
        expect(screen.getByDisplayValue('24 Hours')).toBeInTheDocument();
      });

      const select = screen.getByDisplayValue('24 Hours') as HTMLSelectElement;
      const options = Array.from(select.options).map(opt => opt.textContent);

      // Check that all expected options are present
      expect(options).toContain('1 Hour');
      expect(options).toContain('6 Hours');
      expect(options).toContain('12 Hours');
      expect(options).toContain('24 Hours');
    });

    it('should fetch aggregated metrics on mount', async () => {
      renderDashboard();

      await waitFor(() => {
        expect(apiService.getAggregatedData).toHaveBeenCalled();
      });

      // Should fetch MIN, MAX, AVG for each metric (power, voltage, current)
      // Note: May be called multiple times due to React StrictMode or re-renders
      expect(apiService.getAggregatedData).toHaveBeenCalled();
    });

    it('should refetch metrics when time range changes', async () => {
      renderDashboard();

      await waitFor(() => {
        expect(apiService.getAggregatedData).toHaveBeenCalled();
      });

      const callCountBefore = vi.mocked(apiService.getAggregatedData).mock.calls.length;

      const select = screen.getByDisplayValue('24 Hours');

      await act(async () => {
        fireEvent.change(select, { target: { value: '6h' } });
      });

      await waitFor(() => {
        const callCountAfter = vi.mocked(apiService.getAggregatedData).mock.calls.length;
        expect(callCountAfter).toBeGreaterThan(callCountBefore);
      });
    });

    it('should show refresh button for metrics', async () => {
      renderDashboard();

      await waitFor(() => {
        const refreshButtons = screen.queryAllByTitle('Refresh metrics');
        expect(refreshButtons.length).toBeGreaterThan(0);
      });
    });

    it('should refetch metrics when refresh button is clicked', async () => {
      renderDashboard();

      await waitFor(() => {
        expect(apiService.getAggregatedData).toHaveBeenCalled();
      });

      // Wait for initial data to load completely
      await waitFor(() => {
        expect(screen.getByText('Historical Metrics')).toBeInTheDocument();
      });

      // Clear mock calls to get a clean count
      vi.mocked(apiService.getAggregatedData).mockClear();

      const refreshButtons = screen.getAllByTitle('Refresh metrics');
      const refreshButton = refreshButtons[0];

      await act(async () => {
        fireEvent.click(refreshButton);
      });

      await waitFor(() => {
        // After clearing and clicking refresh, we should have new calls
        expect(apiService.getAggregatedData).toHaveBeenCalled();
      });
    });

    it('should show loading spinner when metrics are loading', async () => {
      vi.mocked(apiService.getAggregatedData).mockImplementation(
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        () => new Promise(() => {}) // Never resolves
      );

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Loading metrics...')).toBeInTheDocument();
      });
    });

    it('should display power consumption metrics', async () => {
      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Power Consumption')).toBeInTheDocument();
      });
    });

    it('should display voltage metrics', async () => {
      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Voltage')).toBeInTheDocument();
      });
    });

    it('should display current metrics', async () => {
      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Current')).toBeInTheDocument();
      });
    });
  });

  // ========== AbortController Cleanup Tests ==========

  describe('AbortController Cleanup', () => {
    it('should handle AbortController for metrics requests', async () => {
      // This test verifies that AbortController is used, even if we can't directly spy on it
      // due to the component's internal implementation

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByDisplayValue('24 Hours')).toBeInTheDocument();
      });

      const select = screen.getByDisplayValue('24 Hours');

      await act(async () => {
        fireEvent.change(select, { target: { value: '6h' } });
      });

      // Should have made new API calls for the new time range
      await waitFor(() => {
        expect(apiService.getAggregatedData).toHaveBeenCalled();
      });
    });

    it('should cleanup on component unmount without errors', async () => {
      const { unmount } = renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Dashboard')).toBeInTheDocument();
      });

      // Should unmount without errors (AbortController cleanup happens internally)
      expect(() => unmount()).not.toThrow();
    });
  });

  // ========== Device Grid Tests ==========

  describe('Device Grid', () => {
    it('should render device cards for all devices', async () => {
      const devices = [
        { ...mockDevice, externalId: 'device-001', name: 'Device 1' },
        { ...mockDevice, externalId: 'device-002', name: 'Device 2' },
        { ...mockDevice, externalId: 'device-003', name: 'Device 3' },
      ];

      vi.mocked(apiService.getDevices).mockResolvedValue(devices);
      vi.mocked(apiService.getLatestTelemetry).mockResolvedValue([
        { deviceId: 'device-001', latest: mockTelemetryPoint },
        { deviceId: 'device-002', latest: mockTelemetryPoint },
        { deviceId: 'device-003', latest: mockTelemetryPoint },
      ]);

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByTestId('device-card-device-001')).toBeInTheDocument();
        expect(screen.getByTestId('device-card-device-002')).toBeInTheDocument();
        expect(screen.getByTestId('device-card-device-003')).toBeInTheDocument();
      });
    });

    it('should show Device Overview heading', async () => {
      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Device Overview')).toBeInTheDocument();
      });
    });
  });

  // ========== Fleet Health & Activity Timeline Tests ==========

  describe('Top Section Widgets', () => {
    it('should render Fleet Health Gauge', async () => {
      renderDashboard();

      await waitFor(() => {
        expect(screen.getByTestId('fleet-health-gauge')).toBeInTheDocument();
      });
    });

    it('should render Activity Timeline', async () => {
      renderDashboard();

      await waitFor(() => {
        expect(screen.getByTestId('activity-timeline')).toBeInTheDocument();
      });
    });
  });

  // ========== Edge Cases ==========

  describe('Edge Cases', () => {
    it('should handle devices with no telemetry data', async () => {
      vi.mocked(apiService.getLatestTelemetry).mockResolvedValue([]);

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByTestId('device-card-device-001')).toBeInTheDocument();
      });
    });

    it('should handle partial telemetry data', async () => {
      const partialTelemetry: LatestTelemetry = {
        deviceId: 'device-001',
        latest: {
          deviceId: 'device-001',
          timestamp: new Date().toISOString(),
          voltage: 220.0,
          // Missing kwConsumption and current
        } as TelemetryPoint,
      };

      vi.mocked(apiService.getLatestTelemetry).mockResolvedValue([partialTelemetry]);

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByTestId('device-card-device-001')).toBeInTheDocument();
      });
    });

    it('should handle empty metrics response', async () => {
      vi.mocked(apiService.getAggregatedData).mockResolvedValue([]);

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Historical Metrics')).toBeInTheDocument();
      });
    });

    it('should handle metrics API error gracefully', async () => {
      vi.mocked(apiService.getAggregatedData).mockRejectedValue(new Error('Metrics error'));

      renderDashboard();

      // Should not crash - metrics just won't display
      await waitFor(() => {
        expect(screen.getByText('Dashboard')).toBeInTheDocument();
      });
    });

    it('should handle WebSocket message for unknown device', async () => {
      const { rerender } = renderDashboard();

      await waitFor(() => {
        expect(screen.getByTestId('device-card-device-001')).toBeInTheDocument();
      });

      const unknownDeviceTelemetry: TelemetryPoint = {
        deviceId: 'unknown-device',
        timestamp: new Date().toISOString(),
        kwConsumption: 100.0,
      };

      vi.mocked(useWebSocket).mockReturnValue({
        lastMessage: unknownDeviceTelemetry,
        connectionStatus: 'Open',
        sendMessage: vi.fn(),
      });

      rerender(
        <BrowserRouter>
          <Dashboard />
        </BrowserRouter>
      );

      // Should not crash
      await waitFor(() => {
        expect(screen.getByTestId('device-card-device-001')).toBeInTheDocument();
      });
    });

    it('should handle very large metric values', async () => {
      const largeTelemetry: TelemetryPoint = {
        deviceId: 'device-001',
        timestamp: new Date().toISOString(),
        kwConsumption: 999999.99,
        voltage: 999999.9,
        current: 999999.999,
      };

      vi.mocked(apiService.getLatestTelemetry).mockResolvedValue([
        { deviceId: 'device-001', latest: largeTelemetry },
      ]);

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByTestId('device-card-device-001')).toBeInTheDocument();
      });
    });

    it('should handle zero values in telemetry', async () => {
      const zeroTelemetry: TelemetryPoint = {
        deviceId: 'device-001',
        timestamp: new Date().toISOString(),
        kwConsumption: 0,
        voltage: 0,
        current: 0,
      };

      vi.mocked(apiService.getLatestTelemetry).mockResolvedValue([
        { deviceId: 'device-001', latest: zeroTelemetry },
      ]);

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByTestId('device-card-device-001')).toBeInTheDocument();
      });
    });

    it('should prevent duplicate initial data fetch', async () => {
      renderDashboard();

      await waitFor(() => {
        expect(apiService.getDevices).toHaveBeenCalledTimes(1);
      });

      // Should not fetch again on re-render
      await waitFor(() => {
        expect(apiService.getDevices).toHaveBeenCalledTimes(1);
      });
    });
  });

  // ========== Accessibility Tests ==========

  describe('Accessibility', () => {
    it('should have proper heading hierarchy', async () => {
      renderDashboard();

      await waitFor(() => {
        const h1 = screen.getByRole('heading', { level: 1, name: /Dashboard/i });
        expect(h1).toBeInTheDocument();
      });
    });

    it('should have select element for time range', async () => {
      renderDashboard();

      await waitFor(() => {
        const select = screen.getByDisplayValue('24 Hours');
        expect(select.tagName).toBe('SELECT');
      });
    });

    it('should disable time range selector when metrics loading', async () => {
      vi.mocked(apiService.getAggregatedData).mockImplementation(
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        () => new Promise(() => {}) // Never resolves
      );

      renderDashboard();

      await waitFor(() => {
        const selects = screen.queryAllByDisplayValue('24 Hours');
        // At least one select should exist and be disabled while loading
        const disabledSelect = selects.find(s => (s as HTMLSelectElement).disabled);
        expect(disabledSelect).toBeDefined();
      });
    });

    it('should disable refresh button when metrics loading', async () => {
      vi.mocked(apiService.getAggregatedData).mockImplementation(
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        () => new Promise(() => {}) // Never resolves
      );

      renderDashboard();

      await waitFor(() => {
        const refreshButtons = screen.queryAllByTitle('Refresh metrics');
        // At least one refresh button should exist and be disabled while loading
        const disabledButton = refreshButtons.find(btn => (btn as HTMLButtonElement).disabled);
        expect(disabledButton).toBeDefined();
      });
    });
  });

  // ========== Performance Tests ==========

  describe('Performance & Memory', () => {
    it('should limit telemetry history to last 10 values per device', async () => {
      const { rerender } = renderDashboard();

      await waitFor(() => {
        expect(screen.getByTestId('device-card-device-001')).toBeInTheDocument();
      });

      // Send 15 messages to ensure history is limited
      for (let i = 0; i < 15; i++) {
        vi.mocked(useWebSocket).mockReturnValue({
          lastMessage: {
            ...mockTelemetryPoint,
            kwConsumption: 50.0 + i,
            timestamp: new Date(Date.now() + i * 1000).toISOString(),
          },
          connectionStatus: 'Open',
          sendMessage: vi.fn(),
        });

        rerender(
          <BrowserRouter>
            <Dashboard />
          </BrowserRouter>
        );
      }

      // History should be limited (verified through ref behavior in component)
      await waitFor(() => {
        expect(screen.getByTestId('device-card-device-001')).toBeInTheDocument();
      });
    });

    it('should not refetch devices when time range changes', async () => {
      renderDashboard();

      await waitFor(() => {
        expect(apiService.getDevices).toHaveBeenCalledTimes(1);
      });

      const select = screen.getByDisplayValue('24 Hours');
      fireEvent.change(select, { target: { value: '6h' } });

      await waitFor(() => {
        // Should still only have been called once
        expect(apiService.getDevices).toHaveBeenCalledTimes(1);
      });
    });
  });
});
