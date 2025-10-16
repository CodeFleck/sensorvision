import React from 'react';
import { Widget } from '../../types';

interface WidgetContainerProps {
  widget: Widget;
  children: React.ReactNode;
  onDelete?: () => void;
  onEdit?: () => void;
}

export const WidgetContainer: React.FC<WidgetContainerProps> = ({
  widget,
  children,
  onDelete,
  onEdit,
}) => {
  return (
    <div className="bg-white rounded-lg shadow-md p-4 h-full flex flex-col">
      {/* Widget Header */}
      <div className="flex justify-between items-center mb-3 border-b pb-2">
        <h3 className="text-sm font-semibold text-gray-700 truncate">
          {widget.name}
        </h3>
        <div className="flex gap-2">
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
