import React, { useEffect } from 'react';
import { Widget, TelemetryPoint } from '../../types';
import { WidgetRenderer } from './WidgetRenderer';

interface WidgetFullscreenModalProps {
  widget: Widget;
  latestData?: TelemetryPoint;
  isOpen: boolean;
  onClose: () => void;
}

export const WidgetFullscreenModal: React.FC<WidgetFullscreenModalProps> = ({
  widget,
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

  return (
    <div className="fixed inset-0 z-50 bg-gray-900 flex flex-col">
      {/* Header with close button */}
      <div className="bg-gray-800 text-white p-4 flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold">{widget.name}</h1>
          {widget.deviceId && (
            <p className="text-sm text-gray-400 mt-1">Device: {widget.deviceId}</p>
          )}
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

      {/* Widget content - fills remaining space */}
      <div className="flex-1 p-6 overflow-auto">
        <div className="h-full bg-white rounded-lg shadow-xl">
          {/* Render the widget without container decorations */}
          <WidgetRenderer
            widget={widget}
            latestData={latestData}
            // No delete/edit buttons in fullscreen
          />
        </div>
      </div>

      {/* Footer with instructions */}
      <div className="bg-gray-800 text-gray-400 text-sm p-2 text-center">
        Press ESC to exit fullscreen
      </div>
    </div>
  );
};
