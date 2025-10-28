import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Dashboard, Widget, TelemetryPoint, Device } from '../types';
import { apiService } from '../services/api';
import { WidgetRenderer } from '../components/widgets/WidgetRenderer';
import { AddWidgetModal } from '../components/widgets/AddWidgetModal';
import { EditWidgetModal } from '../components/widgets/EditWidgetModal';
import { useWebSocket } from '../hooks/useWebSocket';

export const Dashboards: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [devices, setDevices] = useState<Device[]>([]);
  const [selectedDeviceId, setSelectedDeviceId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [editingWidget, setEditingWidget] = useState<Widget | null>(null);
  const [latestData, setLatestData] = useState<Map<string, TelemetryPoint>>(new Map());

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

  // Load devices on mount
  useEffect(() => {
    const loadDevices = async () => {
      try {
        const devicesData = await apiService.getDevices();
        setDevices(devicesData);
      } catch (err) {
        console.error('Failed to load devices:', err);
      }
    };
    loadDevices();
  }, []);

  // Initialize selected device from URL parameter or dashboard default
  useEffect(() => {
    if (dashboard) {
      const deviceFromUrl = searchParams.get('device');
      if (deviceFromUrl) {
        setSelectedDeviceId(deviceFromUrl);
      } else if (dashboard.defaultDeviceId) {
        setSelectedDeviceId(dashboard.defaultDeviceId);
      } else if (devices.length > 0) {
        setSelectedDeviceId(devices[0].externalId);
      }
    }
  }, [dashboard, devices, searchParams]);

  useEffect(() => {
    loadDashboard();
  }, []);

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

  const handleDeviceChange = (deviceId: string) => {
    setSelectedDeviceId(deviceId);
    // Update URL parameter
    const newParams = new URLSearchParams(searchParams);
    newParams.set('device', deviceId);
    setSearchParams(newParams);
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
    <div className="p-6">
      {/* Dashboard Header */}
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

          {/* Device Selector */}
          {devices.length > 1 && (
            <div className="mt-3 flex items-center gap-2">
              <label htmlFor="device-selector" className="text-sm font-medium text-gray-700">
                Device:
              </label>
              <select
                id="device-selector"
                value={selectedDeviceId || ''}
                onChange={(e) => handleDeviceChange(e.target.value)}
                className="px-3 py-1.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
              >
                {devices.map((device) => (
                  <option key={device.externalId} value={device.externalId}>
                    {device.name} ({device.externalId})
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>

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
        <div
          className="grid gap-4"
          style={{
            gridTemplateColumns: `repeat(${cols}, minmax(0, 1fr))`,
          }}
        >
          {dashboard.widgets.map((widget) => (
            <div
              key={widget.id}
              style={{
                gridColumn: `span ${widget.width} / span ${widget.width}`,
                minHeight: `${widget.height * rowHeight}px`,
              }}
            >
              <WidgetRenderer
                widget={widget}
                contextDeviceId={selectedDeviceId || undefined}
                latestData={widget.deviceId ? latestData.get(widget.deviceId) : undefined}
                onDelete={() => handleDeleteWidget(widget.id)}
                onEdit={() => {
                  setEditingWidget(widget);
                  setShowEditModal(true);
                }}
              />
            </div>
          ))}
        </div>
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
    </div>
  );
};
