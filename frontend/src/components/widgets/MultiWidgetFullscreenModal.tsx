import React, { useEffect } from 'react';
import { Widget, TelemetryPoint } from '../../types';
import { WidgetRenderer } from './WidgetRenderer';

interface MultiWidgetFullscreenModalProps {
  widgets: Widget[];
  latestData: Map<string, TelemetryPoint>;
  isOpen: boolean;
  onClose: () => void;
}

export const MultiWidgetFullscreenModal: React.FC<MultiWidgetFullscreenModalProps> = ({
  widgets,
  latestData,
  isOpen,
  onClose,
}) => {
  // Handle ESC key to exit fullscreen
  useEffect(() => {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    if (isOpen) {
      window.addEventListener('keydown', handleEsc);
      // Prevent body scroll when modal is open
      document.body.style.overflow = 'hidden';
    }

    return () => {
      window.removeEventListener('keydown', handleEsc);
      document.body.style.overflow = 'unset';
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  // Calculate grid columns based on number of widgets
  const getGridColumns = (count: number): string => {
    if (count === 1) return 'grid-cols-1';
    if (count === 2) return 'grid-cols-2';
    if (count === 3) return 'grid-cols-3';
    if (count === 4) return 'grid-cols-2';
    if (count <= 6) return 'grid-cols-3';
    if (count <= 9) return 'grid-cols-3';
    return 'grid-cols-4';
  };

  return (
    <div className="fixed inset-0 z-50 bg-gray-900 flex flex-col">
      {/* Header with close button */}
      <div className="bg-gray-800 text-white p-4 flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold">
            Multi-Widget View ({widgets.length} Widget{widgets.length !== 1 ? 's' : ''})
          </h1>
          <p className="text-sm text-gray-400 mt-1">
            Viewing multiple widgets in fullscreen mode
          </p>
        </div>
        <button
          onClick={onClose}
          className="text-gray-400 hover:text-white transition-colors p-2"
          title="Exit fullscreen (ESC)"
        >
          <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      {/* Widgets Grid - fills remaining space */}
      <div className="flex-1 p-6 overflow-auto bg-gray-100">
        <div className={`grid ${getGridColumns(widgets.length)} gap-6 h-full`}>
          {widgets.map((widget) => (
            <div
              key={widget.id}
              className="bg-white rounded-lg shadow-xl overflow-hidden flex flex-col"
              style={{ minHeight: widgets.length === 1 ? '100%' : '400px' }}
            >
              <WidgetRenderer
                widget={widget}
                latestData={widget.deviceId ? latestData.get(widget.deviceId) : undefined}
                // No delete/edit buttons in fullscreen
              />
            </div>
          ))}
        </div>
      </div>

      {/* Footer with instructions */}
      <div className="bg-gray-800 text-gray-400 text-sm p-2 text-center">
        Press ESC to exit fullscreen
      </div>
    </div>
  );
};
