import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Dashboard, Widget, TelemetryPoint, Device } from '../types';
import { apiService } from '../services/api';
import { WidgetRenderer } from '../components/widgets/WidgetRenderer';
import { AddWidgetModal } from '../components/widgets/AddWidgetModal';
import { EditWidgetModal } from '../components/widgets/EditWidgetModal';
import { WidgetFullscreenModal } from '../components/widgets/WidgetFullscreenModal';
import { MultiWidgetFullscreenModal } from '../components/widgets/MultiWidgetFullscreenModal';
import { useWebSocket } from '../hooks/useWebSocket';
import GridLayout from 'react-grid-layout';
import type { Layout } from 'react-grid-layout';

export const Dashboards: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [allDashboards, setAllDashboards] = useState<Dashboard[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [editingWidget, setEditingWidget] = useState<Widget | null>(null);
  const [fullscreenWidget, setFullscreenWidget] = useState<Widget | null>(null);
  const [selectionMode, setSelectionMode] = useState(false);
  const [selectedWidgets, setSelectedWidgets] = useState<Set<number>>(new Set());
  const [showMultiWidgetFullscreen, setShowMultiWidgetFullscreen] = useState(false);
  const [latestData, setLatestData] = useState<Map<string, TelemetryPoint>>(new Map());
  const [showKioskControls, setShowKioskControls] = useState(false);
  const [controlsTimeout, setControlsTimeout] = useState<NodeJS.Timeout | null>(null);

  // Kiosk mode & auto-refresh parameters
  const kioskMode = searchParams.get('kiosk') === 'true';
  const refreshInterval = parseInt(searchParams.get('refresh') || '0', 10) * 1000; // Convert to ms

  // WebSocket connection for real-time updates
  const { lastMessage, connectionStatus } = useWebSocket('ws://localhost:8080/ws/telemetry');

  // Process incoming WebSocket messages
  useEffect(() => {
    if (lastMessage) {
      setLatestData((prev) => {
        const newMap = new Map(prev);
        newMap.set(lastMessage.deviceId, lastMessage);
        return newMap;
      });
    }
  }, [lastMessage]);

  // Load all dashboards on mount
  useEffect(() => {
    const loadAllDashboards = async () => {
      try {
        const dashboards = await apiService.getDashboards();
        setAllDashboards(dashboards);
      } catch (err) {
        console.error('Failed to load dashboards:', err);
      }
    };

    loadAllDashboards();
  }, []);

  useEffect(() => {
    loadDashboard();
  }, []);

  // Auto-refresh logic
  useEffect(() => {
    if (refreshInterval > 0) {
      const interval = setInterval(() => {
        loadDashboard();
      }, refreshInterval);

      return () => clearInterval(interval);
    }
  }, [refreshInterval]);

  const loadDashboard = async () => {
    try {
      setLoading(true);
      const data = await apiService.getDefaultDashboard();
      setDashboard(data);
      setError(null);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to load dashboard';
      setError(errorMessage);
      console.error('Dashboard load error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteWidget = async (widgetId: number) => {
    if (!dashboard) return;

    if (confirm('Are you sure you want to delete this widget?')) {
      try {
        await apiService.deleteWidget(dashboard.id, widgetId);
        await loadDashboard(); // Reload dashboard
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to delete widget';
        console.error('Widget deletion error:', err);
        alert(`Failed to delete widget:\n\n${errorMessage}`);
      }
    }
  };

  // Kiosk mode controls
  const enterKioskMode = () => {
    const newParams = new URLSearchParams(searchParams);
    newParams.set('kiosk', 'true');
    setSearchParams(newParams);
  };

  const exitKioskMode = () => {
    const newParams = new URLSearchParams(searchParams);
    newParams.delete('kiosk');
    newParams.delete('refresh');
    setSearchParams(newParams);
  };

  const handleMouseMove = () => {
    if (!kioskMode) return;

    setShowKioskControls(true);

    if (controlsTimeout) {
      clearTimeout(controlsTimeout);
    }

    const timeout = setTimeout(() => {
      setShowKioskControls(false);
    }, 3000);

    setControlsTimeout(timeout);
  };

  const switchDashboard = async (dashboardId: number) => {
    try {
      const newDashboard = await apiService.getDashboard(dashboardId);
      setDashboard(newDashboard);
      setError(null);
    } catch (err) {
      console.error('Failed to switch dashboard:', err);
    }
  };

  // Selection mode handlers
  const toggleSelectionMode = () => {
    setSelectionMode(!selectionMode);
    if (selectionMode) {
      // Exiting selection mode - clear selections
      setSelectedWidgets(new Set());
    }
  };

  const toggleWidgetSelection = (widgetId: number) => {
    setSelectedWidgets((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(widgetId)) {
        newSet.delete(widgetId);
      } else {
        newSet.add(widgetId);
      }
      return newSet;
    });
  };

  // Handle layout changes from drag/resize
  const handleLayoutChange = async (layout: Layout[]) => {
    if (!dashboard || kioskMode) return;

    // Update widget positions
    const updates = layout.map(async (item) => {
      const widgetId = parseInt(item.i);
      const widget = dashboard.widgets.find(w => w.id === widgetId);

      if (!widget) return;

      // Only update if position or size changed
      if (
        widget.positionX !== item.x ||
        widget.positionY !== item.y ||
        widget.width !== item.w ||
        widget.height !== item.h
      ) {
        try {
          await apiService.updateWidget(dashboard.id, widgetId, {
            ...widget,
            positionX: item.x,
            positionY: item.y,
            width: item.w,
            height: item.h,
          });
        } catch (err) {
          console.error(`Failed to update widget ${widgetId} position:`, err);
        }
      }
    });

    await Promise.all(updates);

    // Update local state without reloading entire dashboard
    // This prevents widgets from remounting and retriggering all API calls
    setDashboard(prev => {
      if (!prev) return prev;
      return {
        ...prev,
        widgets: prev.widgets.map(w => {
          const layoutItem = layout.find(l => parseInt(l.i) === w.id);
          if (layoutItem) {
            return {
              ...w,
              positionX: layoutItem.x,
              positionY: layoutItem.y,
              width: layoutItem.w,
              height: layoutItem.h,
            };
          }
          return w;
        }),
      };
    });
  };

  // ESC key to exit kiosk mode
  useEffect(() => {
    const handleEscKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && kioskMode) {
        exitKioskMode();
      }
    };

    window.addEventListener('keydown', handleEscKey);
    return () => window.removeEventListener('keydown', handleEscKey);
  }, [kioskMode]);

  // Cleanup controls timeout on unmount
  useEffect(() => {
    return () => {
      if (controlsTimeout) {
        clearTimeout(controlsTimeout);
      }
    };
  }, [controlsTimeout]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-center max-w-2xl px-4">
          <svg className="w-16 h-16 text-red-500 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <h2 className="text-2xl font-bold text-gray-800 mb-2">Error Loading Dashboard</h2>
          <div className="text-left bg-gray-50 rounded-lg p-4 mb-4">
            <pre className="text-sm text-gray-700 whitespace-pre-wrap font-mono">{error}</pre>
          </div>
          <button
            onClick={loadDashboard}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (!dashboard) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-gray-800 mb-2">No Dashboard Found</h2>
          <p className="text-gray-600">Create a dashboard to get started</p>
        </div>
      </div>
    );
  }

  // Calculate grid layout
  const cols = dashboard.layoutConfig.cols || 12;
  const rowHeight = dashboard.layoutConfig.rowHeight || 100;

  return (
    <div
      className={kioskMode ? 'p-0 h-screen overflow-hidden' : 'p-6'}
      onMouseMove={handleMouseMove}
    >
      {/* Dashboard Header (hidden in kiosk mode) */}
      {!kioskMode && (
        <div className="mb-6 flex justify-between items-start">
          <div className="flex-1">
            <div className="flex items-center gap-3">
              <h1 className="text-3xl font-bold text-gray-800">{dashboard.name}</h1>
              {/* WebSocket Status Indicator */}
              <div className="flex items-center gap-2 text-sm">
                <div className={`w-2 h-2 rounded-full ${connectionStatus === 'Open' ? 'bg-green-500' : 'bg-gray-400'}`} />
                <span className="text-gray-600">
                  {connectionStatus === 'Open' ? 'Live' : connectionStatus}
                </span>
              </div>
            </div>
            {dashboard.description && (
              <p className="text-gray-600 mt-1">{dashboard.description}</p>
            )}
          </div>

          <div className="flex gap-2">
            {selectionMode && selectedWidgets.size > 0 && (
              <div className="flex items-center gap-2 px-3 py-2 bg-blue-100 text-blue-700 rounded-lg font-medium">
                {selectedWidgets.size} widget{selectedWidgets.size !== 1 ? 's' : ''} selected
              </div>
            )}
            <button
              onClick={toggleSelectionMode}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${
                selectionMode
                  ? 'bg-red-600 text-white hover:bg-red-700'
                  : 'bg-green-600 text-white hover:bg-green-700'
              }`}
              title={selectionMode ? "Exit selection mode" : "Select multiple widgets for fullscreen"}
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                {selectionMode ? (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                ) : (
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
                )}
              </svg>
              {selectionMode ? 'Exit Selection' : 'Select Widgets'}
            </button>
            <button
              onClick={enterKioskMode}
              className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors"
              title="Enter fullscreen mode (ESC to exit)"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
              </svg>
              Fullscreen
            </button>
            <button
              onClick={() => setShowAddModal(true)}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
              </svg>
              Add Widget
            </button>
          </div>
        </div>
      )}

      {/* Floating "View Selected" Button */}
      {selectionMode && selectedWidgets.size > 0 && (
        <div className="fixed bottom-8 right-8 z-40">
          <button
            onClick={() => setShowMultiWidgetFullscreen(true)}
            className="flex items-center gap-3 px-6 py-4 bg-purple-600 text-white rounded-full shadow-2xl hover:bg-purple-700 transition-all hover:scale-105"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
            </svg>
            <span className="font-semibold">
              View {selectedWidgets.size} Widget{selectedWidgets.size !== 1 ? 's' : ''} in Fullscreen
            </span>
          </button>
        </div>
      )}

      {/* Floating Kiosk Controls */}
      {kioskMode && (
        <div
          className={`fixed top-4 right-4 z-50 bg-gray-900 bg-opacity-95 rounded-lg shadow-2xl transition-all duration-300 ${
            showKioskControls ? 'opacity-100 translate-y-0' : 'opacity-0 -translate-y-4 pointer-events-none'
          }`}
        >
          <div className="p-4 flex items-center gap-3">
            {/* Dashboard Selector */}
            {allDashboards.length > 1 && (
              <select
                value={dashboard.id}
                onChange={(e) => switchDashboard(Number(e.target.value))}
                className="px-3 py-2 bg-gray-800 text-white border border-gray-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 text-sm"
              >
                {allDashboards.map((dash) => (
                  <option key={dash.id} value={dash.id}>
                    {dash.name}
                  </option>
                ))}
              </select>
            )}

            {/* Exit Fullscreen Button */}
            <button
              onClick={exitKioskMode}
              className="flex items-center gap-2 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors text-sm"
              title="Exit fullscreen (ESC)"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
              Exit
            </button>
          </div>
        </div>
      )}

      {/* Widgets Grid */}
      {dashboard.widgets.length === 0 ? (
        <div className="text-center py-12 bg-gray-50 rounded-lg">
          <svg className="w-16 h-16 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
          </svg>
          <h3 className="text-xl font-semibold text-gray-700 mb-2">No Widgets Yet</h3>
          <p className="text-gray-500">Add widgets to start monitoring your devices</p>
        </div>
      ) : (
        <GridLayout
          className={kioskMode ? 'h-screen' : 'layout'}
          layout={dashboard.widgets.map((w) => ({
            i: w.id.toString(),
            x: w.positionX,
            y: w.positionY,
            w: w.width,
            h: w.height,
            static: kioskMode, // Disable drag/resize in kiosk mode
          }))}
          cols={cols}
          rowHeight={rowHeight}
          width={typeof window !== 'undefined' ? window.innerWidth - (kioskMode ? 16 : 96) : 1200}
          isDraggable={!kioskMode}
          isResizable={!kioskMode}
          onLayoutChange={handleLayoutChange}
          draggableHandle=".widget-drag-handle"
          margin={kioskMode ? [8, 8] : [16, 16]}
          containerPadding={kioskMode ? [8, 8] : [0, 0]}
        >
          {dashboard.widgets.map((widget) => (
            <div
              key={widget.id}
              className="widget-container"
            >
              <WidgetRenderer
                widget={widget}
                latestData={widget.deviceId ? latestData.get(widget.deviceId) : undefined}
                onDelete={kioskMode || selectionMode ? undefined : () => handleDeleteWidget(widget.id)}
                onEdit={kioskMode || selectionMode ? undefined : () => {
                  setEditingWidget(widget);
                  setShowEditModal(true);
                }}
                onFullscreen={selectionMode ? undefined : () => setFullscreenWidget(widget)}
                selectionMode={selectionMode}
                isSelected={selectedWidgets.has(widget.id)}
                onSelect={() => toggleWidgetSelection(widget.id)}
              />
            </div>
          ))}
        </GridLayout>
      )}

      {/* Add Widget Modal */}
      <AddWidgetModal
        dashboardId={dashboard.id}
        isOpen={showAddModal}
        onClose={() => setShowAddModal(false)}
        onSuccess={loadDashboard}
      />

      {/* Edit Widget Modal */}
      {editingWidget && (
        <EditWidgetModal
          dashboardId={dashboard.id}
          widget={editingWidget}
          isOpen={showEditModal}
          onClose={() => {
            setShowEditModal(false);
            setEditingWidget(null);
          }}
          onSuccess={loadDashboard}
        />
      )}

      {/* Widget Fullscreen Modal */}
      {fullscreenWidget && (
        <WidgetFullscreenModal
          widget={fullscreenWidget}
          latestData={fullscreenWidget.deviceId ? latestData.get(fullscreenWidget.deviceId) : undefined}
          isOpen={!!fullscreenWidget}
          onClose={() => setFullscreenWidget(null)}
        />
      )}

      {/* Multi-Widget Fullscreen Modal */}
      {dashboard && (
        <MultiWidgetFullscreenModal
          widgets={dashboard.widgets.filter((w) => selectedWidgets.has(w.id))}
          latestData={latestData}
          isOpen={showMultiWidgetFullscreen}
          onClose={() => setShowMultiWidgetFullscreen(false)}
        />
      )}
    </div>
  );
};
