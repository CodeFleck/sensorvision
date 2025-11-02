import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Playlist, Dashboard, TelemetryPoint } from '../types';
import { apiService } from '../services/api';
import { useWebSocket } from '../hooks/useWebSocket';
import GridLayout from 'react-grid-layout';
import { WidgetRenderer } from '../components/widgets/WidgetRenderer';

export const PlaylistPlayer: React.FC = () => {
  const { playlistId } = useParams<{ playlistId: string }>();
  const navigate = useNavigate();
  const [playlist, setPlaylist] = useState<Playlist | null>(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [currentDashboard, setCurrentDashboard] = useState<Dashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isPaused, setIsPaused] = useState(false);
  const [showControls, setShowControls] = useState(false);
  const [controlsTimeout, setControlsTimeout] = useState<number | null>(null);
  const [latestData, setLatestData] = useState<Map<string, TelemetryPoint>>(new Map());
  const [timeRemaining, setTimeRemaining] = useState(0);

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

  // Load playlist
  useEffect(() => {
    const loadPlaylist = async () => {
      if (!playlistId) {
        setError('No playlist ID provided');
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        const data = await apiService.getPlaylist(parseInt(playlistId));
        setPlaylist(data);

        if (data.items.length === 0) {
          setError('Playlist has no dashboards');
          return;
        }

        // Load first dashboard
        const firstDashboard = await apiService.getDashboard(data.items[0].dashboardId);
        setCurrentDashboard(firstDashboard);
        setTimeRemaining(data.items[0].displayDurationSeconds);
        setError(null);
      } catch (err) {
        console.error('Failed to load playlist:', err);
        setError('Failed to load playlist');
      } finally {
        setLoading(false);
      }
    };

    loadPlaylist();
  }, [playlistId]);

  // Auto-advance to next dashboard
  const advanceToDashboard = useCallback(async (index: number) => {
    if (!playlist) return;

    const item = playlist.items[index];
    if (!item) return;

    try {
      const dashboard = await apiService.getDashboard(item.dashboardId);
      setCurrentDashboard(dashboard);
      setCurrentIndex(index);
      setTimeRemaining(item.displayDurationSeconds);
    } catch (err) {
      console.error('Failed to load dashboard:', err);
    }
  }, [playlist]);

  // Timer effect for auto-advance
  useEffect(() => {
    if (!playlist || isPaused || playlist.items.length === 0) return;

    const interval = setInterval(() => {
      setTimeRemaining((prev) => {
        if (prev <= 1) {
          // Time to advance to next dashboard
          const nextIndex = currentIndex + 1;
          if (nextIndex >= playlist.items.length) {
            // End of playlist
            if (playlist.loopEnabled) {
              // Loop back to start
              advanceToDashboard(0);
            } else {
              // Stop at the end
              setIsPaused(true);
            }
          } else {
            // Advance to next
            advanceToDashboard(nextIndex);
          }
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [playlist, currentIndex, isPaused, advanceToDashboard]);

  // Mouse move handler for showing/hiding controls
  const handleMouseMove = () => {
    setShowControls(true);

    if (controlsTimeout) {
      clearTimeout(controlsTimeout);
    }

    const timeout = setTimeout(() => {
      setShowControls(false);
    }, 3000);

    setControlsTimeout(timeout);
  };

  // ESC key to exit player
  useEffect(() => {
    const handleEscKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        navigate('/playlists');
      }
    };

    window.addEventListener('keydown', handleEscKey);
    return () => window.removeEventListener('keydown', handleEscKey);
  }, [navigate]);

  // Cleanup controls timeout
  useEffect(() => {
    return () => {
      if (controlsTimeout) {
        clearTimeout(controlsTimeout);
      }
    };
  }, [controlsTimeout]);

  const handlePrevious = () => {
    if (!playlist) return;
    const prevIndex = currentIndex - 1;
    if (prevIndex >= 0) {
      advanceToDashboard(prevIndex);
    }
  };

  const handleNext = () => {
    if (!playlist) return;
    const nextIndex = currentIndex + 1;
    if (nextIndex < playlist.items.length) {
      advanceToDashboard(nextIndex);
    } else if (playlist.loopEnabled) {
      advanceToDashboard(0);
    }
  };

  const handleExit = () => {
    navigate('/playlists');
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen bg-gray-900">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-white mx-auto mb-4"></div>
          <p className="text-white">Loading playlist...</p>
        </div>
      </div>
    );
  }

  if (error || !playlist || !currentDashboard) {
    return (
      <div className="flex items-center justify-center h-screen bg-gray-900">
        <div className="text-center max-w-2xl px-4">
          <svg className="w-16 h-16 text-red-500 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <h2 className="text-2xl font-bold text-white mb-2">Error</h2>
          <p className="text-gray-300 mb-4">{error || 'Failed to load playlist'}</p>
          <button
            onClick={handleExit}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            Back to Playlists
          </button>
        </div>
      </div>
    );
  }

  const cols = currentDashboard.layoutConfig.cols || 12;
  const rowHeight = currentDashboard.layoutConfig.rowHeight || 100;

  return (
    <div
      className="h-screen overflow-hidden bg-gray-100"
      onMouseMove={handleMouseMove}
    >
      {/* Floating Controls */}
      <div
        className={`fixed top-4 right-4 z-50 bg-gray-900 bg-opacity-95 rounded-lg shadow-2xl transition-all duration-300 ${
          showControls ? 'opacity-100 translate-y-0' : 'opacity-0 -translate-y-4 pointer-events-none'
        }`}
      >
        <div className="p-4">
          {/* Playlist Info */}
          <div className="mb-3 pb-3 border-b border-gray-700">
            <h3 className="text-white font-semibold">{playlist.name}</h3>
            <p className="text-gray-400 text-sm">
              Dashboard {currentIndex + 1} of {playlist.items.length}: {currentDashboard.name}
            </p>
            <div className="flex items-center gap-2 mt-2">
              <div className={`w-2 h-2 rounded-full ${connectionStatus === 'Open' ? 'bg-green-500' : 'bg-gray-400'}`} />
              <span className="text-gray-400 text-xs">
                {connectionStatus === 'Open' ? 'Live' : connectionStatus}
              </span>
            </div>
          </div>

          {/* Progress Bar */}
          <div className="mb-3">
            <div className="flex justify-between text-xs text-gray-400 mb-1">
              <span>Time remaining</span>
              <span>{timeRemaining}s</span>
            </div>
            <div className="w-full bg-gray-700 rounded-full h-2">
              <div
                className="bg-purple-600 h-2 rounded-full transition-all duration-1000"
                style={{
                  width: `${(timeRemaining / playlist.items[currentIndex].displayDurationSeconds) * 100}%`
                }}
              />
            </div>
          </div>

          {/* Controls */}
          <div className="flex items-center gap-2">
            <button
              onClick={handlePrevious}
              disabled={currentIndex === 0 && !playlist.loopEnabled}
              className="p-2 bg-gray-800 text-white rounded hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
              title="Previous dashboard"
            >
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                <path d="M8.445 14.832A1 1 0 0010 14v-2.798l5.445 3.63A1 1 0 0017 14V6a1 1 0 00-1.555-.832L10 8.798V6a1 1 0 00-1.555-.832l-6 4a1 1 0 000 1.664l6 4z" />
              </svg>
            </button>

            <button
              onClick={() => setIsPaused(!isPaused)}
              className="p-2 bg-gray-800 text-white rounded hover:bg-gray-700"
              title={isPaused ? "Resume" : "Pause"}
            >
              {isPaused ? (
                <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M6.3 2.841A1.5 1.5 0 004 4.11V15.89a1.5 1.5 0 002.3 1.269l9.344-5.89a1.5 1.5 0 000-2.538L6.3 2.84z" />
                </svg>
              ) : (
                <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                  <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zM7 8a1 1 0 012 0v4a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v4a1 1 0 102 0V8a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
              )}
            </button>

            <button
              onClick={handleNext}
              disabled={currentIndex === playlist.items.length - 1 && !playlist.loopEnabled}
              className="p-2 bg-gray-800 text-white rounded hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
              title="Next dashboard"
            >
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                <path d="M4.555 5.168A1 1 0 003 6v8a1 1 0 001.555.832L10 11.202V14a1 1 0 001.555.832l6-4a1 1 0 000-1.664l-6-4A1 1 0 0010 6v2.798l-5.445-3.63z" />
              </svg>
            </button>

            <div className="border-l border-gray-700 h-8 mx-1" />

            <button
              onClick={handleExit}
              className="px-3 py-2 bg-red-600 text-white rounded hover:bg-red-700 text-sm"
              title="Exit playlist (ESC)"
            >
              Exit
            </button>
          </div>
        </div>
      </div>

      {/* Dashboard Display */}
      <div className="h-full p-4">
        {currentDashboard.widgets.length === 0 ? (
          <div className="flex items-center justify-center h-full">
            <div className="text-center">
              <svg className="w-16 h-16 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
              </svg>
              <h3 className="text-xl font-semibold text-gray-700">No Widgets</h3>
              <p className="text-gray-500">This dashboard has no widgets</p>
            </div>
          </div>
        ) : (
          <GridLayout
            className="layout"
            layout={currentDashboard.widgets.map((w) => ({
              i: w.id.toString(),
              x: w.positionX,
              y: w.positionY,
              w: w.width,
              h: w.height,
              static: true, // Disable drag/resize in player
            }))}
            cols={cols}
            rowHeight={rowHeight}
            width={typeof window !== 'undefined' ? window.innerWidth - 32 : 1200}
            isDraggable={false}
            isResizable={false}
            margin={[16, 16]}
            containerPadding={[0, 0]}
          >
            {currentDashboard.widgets.map((widget) => (
              <div
                key={widget.id}
                className="widget-container"
              >
                <WidgetRenderer
                  widget={widget}
                  latestData={widget.deviceId ? latestData.get(widget.deviceId) : undefined}
                  // No edit/delete buttons in player
                />
              </div>
            ))}
          </GridLayout>
        )}
      </div>

      {/* Footer with ESC hint */}
      <div
        className={`fixed bottom-0 left-0 right-0 bg-gray-900 bg-opacity-90 text-gray-400 text-xs text-center py-1 transition-opacity duration-300 ${
          showControls ? 'opacity-100' : 'opacity-0'
        }`}
      >
        Press ESC to exit playlist player
      </div>
    </div>
  );
};
