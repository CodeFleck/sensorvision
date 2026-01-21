import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import { Dashboards } from './Dashboards';
import { apiService } from '../services/api';
import { useWebSocket } from '../hooks/useWebSocket';
import { BrowserRouter } from 'react-router-dom';
import { Dashboard, Widget } from '../types';

// Mock dependencies
vi.mock('../services/api');
vi.mock('../hooks/useWebSocket');

// Mock react-grid-layout with a way to trigger layout changes
let mockOnLayoutChange: ((layout: any[]) => void) | null = null;

vi.mock('react-grid-layout', () => ({
  default: ({ children, onLayoutChange }: {
    children: React.ReactNode;
    onLayoutChange: (layout: any[]) => void;
  }) => {
    mockOnLayoutChange = onLayoutChange;
    return (
      <div data-testid="mock-grid-layout">
        {children}
      </div>
    );
  },
}));

// Mock child components
vi.mock('../components/widgets/WidgetRenderer', () => ({
  WidgetRenderer: ({ widget }: { widget: Widget }) => (
    <div data-testid={`widget-${widget.id}`}>{widget.title}</div>
  ),
}));

vi.mock('../components/widgets/AddWidgetModal', () => ({
  AddWidgetModal: () => null,
}));

vi.mock('../components/widgets/EditWidgetModal', () => ({
  EditWidgetModal: () => null,
}));

vi.mock('../components/widgets/WidgetFullscreenModal', () => ({
  WidgetFullscreenModal: () => null,
}));

vi.mock('../components/widgets/MultiWidgetFullscreenModal', () => ({
  MultiWidgetFullscreenModal: () => null,
}));

vi.mock('../components/widgets/WidgetAssistantChat', () => ({
  WidgetAssistantChat: () => null,
}));

describe('Dashboards - Widget Position Update Debouncing', () => {
  const mockWidget: Widget = {
    id: 1,
    dashboardId: 1,
    type: 'metric',
    title: 'Test Widget',
    config: {},
    positionX: 0,
    positionY: 0,
    width: 4,
    height: 2,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  const mockDashboard: Dashboard = {
    id: 1,
    name: 'Test Dashboard',
    description: 'A test dashboard',
    isDefault: true,
    widgets: [mockWidget],
    layoutConfig: { cols: 12, rowHeight: 100 },
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    mockOnLayoutChange = null;

    vi.mocked(useWebSocket).mockReturnValue({
      lastMessage: null,
      connectionStatus: 'Open',
      sendMessage: vi.fn(),
    });

    vi.mocked(apiService.getDashboards).mockResolvedValue([mockDashboard]);
    vi.mocked(apiService.getDefaultDashboard).mockResolvedValue(mockDashboard);
    vi.mocked(apiService.updateWidget).mockResolvedValue(mockWidget);

    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      value: 1200,
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  const renderDashboards = () => {
    return render(
      <BrowserRouter>
        <Dashboards />
      </BrowserRouter>
    );
  };

  it('should debounce API calls when layout changes rapidly', async () => {
    renderDashboards();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    vi.mocked(apiService.updateWidget).mockClear();

    // Switch to fake timers AFTER initial render
    vi.useFakeTimers();

    // Simulate rapid layout changes (like dragging a widget)
    act(() => {
      if (mockOnLayoutChange) {
        mockOnLayoutChange([{ i: '1', x: 1, y: 0, w: 4, h: 2 }]);
        mockOnLayoutChange([{ i: '1', x: 2, y: 0, w: 4, h: 2 }]);
        mockOnLayoutChange([{ i: '1', x: 3, y: 0, w: 4, h: 2 }]);
      }
    });

    // Before debounce timeout, no API calls should be made
    expect(apiService.updateWidget).not.toHaveBeenCalled();

    // Advance timers to trigger debounced save (500ms)
    await act(async () => {
      await vi.advanceTimersByTimeAsync(600);
    });

    // After debounce, only ONE API call should be made (not 3)
    expect(apiService.updateWidget).toHaveBeenCalledTimes(1);

    // The final position (x: 3) should be sent
    expect(apiService.updateWidget).toHaveBeenCalledWith(
      mockDashboard.id,
      mockWidget.id,
      expect.objectContaining({
        positionX: 3,
      })
    );
  });

  it('should reset debounce timer when new layout change occurs', async () => {
    renderDashboards();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    vi.mocked(apiService.updateWidget).mockClear();
    vi.useFakeTimers();

    // First layout change
    act(() => {
      if (mockOnLayoutChange) {
        mockOnLayoutChange([{ i: '1', x: 1, y: 0, w: 4, h: 2 }]);
      }
    });

    // Wait 400ms (less than 500ms debounce)
    await act(async () => {
      await vi.advanceTimersByTimeAsync(400);
    });

    // Second layout change resets the timer
    act(() => {
      if (mockOnLayoutChange) {
        mockOnLayoutChange([{ i: '1', x: 2, y: 0, w: 4, h: 2 }]);
      }
    });

    // Wait 400ms more
    await act(async () => {
      await vi.advanceTimersByTimeAsync(400);
    });

    // Still no API call because timer was reset
    expect(apiService.updateWidget).not.toHaveBeenCalled();

    // Wait remaining 100ms + buffer
    await act(async () => {
      await vi.advanceTimersByTimeAsync(200);
    });

    // Now the API should be called with final position
    expect(apiService.updateWidget).toHaveBeenCalledTimes(1);
    expect(apiService.updateWidget).toHaveBeenCalledWith(
      mockDashboard.id,
      mockWidget.id,
      expect.objectContaining({
        positionX: 2,
      })
    );
  });

  it('should send API call even when position matches (server handles no-op)', async () => {
    // With debouncing, we always send the final position to the server
    // The server will handle the case where position hasn't changed
    // This simplifies the client logic and avoids stale closure issues
    renderDashboards();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    vi.mocked(apiService.updateWidget).mockClear();
    vi.useFakeTimers();

    // Layout change with same position as initial
    act(() => {
      if (mockOnLayoutChange) {
        mockOnLayoutChange([{ i: '1', x: 0, y: 0, w: 4, h: 2 }]);
      }
    });

    await act(async () => {
      await vi.advanceTimersByTimeAsync(600);
    });

    // API call is made - server will handle no-op case
    expect(apiService.updateWidget).toHaveBeenCalledTimes(1);
  });

  it('should render dashboard with widgets', async () => {
    renderDashboards();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    expect(screen.getByTestId('widget-1')).toBeInTheDocument();
    expect(screen.getByText('Test Widget')).toBeInTheDocument();
  });

  it('should use ref to avoid stale closure when saving layout', async () => {
    // This test verifies the fix for the stale closure bug
    // The saveLayoutToServer function now uses a ref to get current dashboard state
    // rather than capturing dashboard in the closure

    renderDashboards();

    await waitFor(() => {
      expect(screen.getByText('Test Dashboard')).toBeInTheDocument();
    });

    vi.mocked(apiService.updateWidget).mockClear();
    vi.useFakeTimers();

    // Simulate multiple rapid layout changes (like continuous dragging)
    act(() => {
      if (mockOnLayoutChange) {
        // First position change
        mockOnLayoutChange([{ i: '1', x: 1, y: 0, w: 4, h: 2 }]);
      }
    });

    // Small delay between changes
    await act(async () => {
      await vi.advanceTimersByTimeAsync(100);
    });

    act(() => {
      if (mockOnLayoutChange) {
        // Second position change (local state now has x=1)
        mockOnLayoutChange([{ i: '1', x: 2, y: 0, w: 4, h: 2 }]);
      }
    });

    await act(async () => {
      await vi.advanceTimersByTimeAsync(100);
    });

    act(() => {
      if (mockOnLayoutChange) {
        // Third position change (local state now has x=2)
        mockOnLayoutChange([{ i: '1', x: 3, y: 0, w: 4, h: 2 }]);
      }
    });

    // Wait for debounce
    await act(async () => {
      await vi.advanceTimersByTimeAsync(600);
    });

    // Should make exactly one API call with the final position
    expect(apiService.updateWidget).toHaveBeenCalledTimes(1);
    expect(apiService.updateWidget).toHaveBeenCalledWith(
      mockDashboard.id,
      mockWidget.id,
      expect.objectContaining({
        positionX: 3,
      })
    );
  });
});
