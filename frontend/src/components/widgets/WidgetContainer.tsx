import React from 'react';
import { Widget } from '../../types';

interface WidgetContainerProps {
  widget: Widget;
  children: React.ReactNode;
  onDelete?: () => void;
  onEdit?: () => void;
  onFullscreen?: () => void;
  isSelected?: boolean;
  onSelect?: () => void;
  selectionMode?: boolean;
}

export const WidgetContainer: React.FC<WidgetContainerProps> = ({
  widget,
  children,
  onDelete,
  onEdit,
  onFullscreen,
  isSelected,
  onSelect,
  selectionMode,
}) => {
  return (
    <div className={`bg-white rounded-lg shadow-md p-4 h-full flex flex-col transition-all ${
      isSelected ? 'ring-4 ring-blue-500' : ''
    }`}>
      {/* Widget Header */}
      <div className="flex justify-between items-center mb-3 border-b pb-2">
        <div className="flex items-center gap-2 flex-1">
          {/* Selection checkbox (shown in selection mode) */}
          {selectionMode && onSelect && (
            <input
              type="checkbox"
              checked={isSelected}
              onChange={onSelect}
              className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500 cursor-pointer"
              title="Select widget"
            />
          )}
          {/* Drag Handle (hidden in selection mode) */}
          {!selectionMode && (
            <div className="widget-drag-handle cursor-move text-gray-400 hover:text-gray-600">
              <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                <path d="M7 2a2 2 0 1 0 .001 4.001A2 2 0 0 0 7 2zm0 6a2 2 0 1 0 .001 4.001A2 2 0 0 0 7 8zm0 6a2 2 0 1 0 .001 4.001A2 2 0 0 0 7 14zm6-8a2 2 0 1 0-.001-4.001A2 2 0 0 0 13 6zm0 2a2 2 0 1 0 .001 4.001A2 2 0 0 0 13 8zm0 6a2 2 0 1 0 .001 4.001A2 2 0 0 0 13 14z" />
              </svg>
            </div>
          )}
          <h3 className="text-sm font-semibold text-gray-700 truncate">
            {widget.name}
          </h3>
        </div>
        <div className="flex gap-2">
          {onFullscreen && (
            <button
              onClick={onFullscreen}
              className="text-gray-400 hover:text-purple-600 transition-colors"
              title="View fullscreen"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
              </svg>
            </button>
          )}
          {onEdit && (
            <button
              onClick={onEdit}
              className="text-gray-400 hover:text-blue-600 transition-colors"
              title="Edit widget"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
            </button>
          )}
          {onDelete && (
            <button
              onClick={onDelete}
              className="text-gray-400 hover:text-red-600 transition-colors"
              title="Delete widget"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          )}
        </div>
      </div>

      {/* Widget Content */}
      <div className="flex-1 flex items-center justify-center overflow-hidden">
        {children}
      </div>

      {/* Widget Footer (optional metadata) */}
      {widget.deviceId && (
        <div className="mt-2 pt-2 border-t text-xs text-gray-500 truncate">
          Device: {widget.deviceId}
        </div>
      )}
    </div>
  );
};
