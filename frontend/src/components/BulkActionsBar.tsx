import React from 'react';
import { Trash2, Power, PowerOff } from 'lucide-react';

interface BulkActionsBarProps {
  selectedCount: number;
  onDelete: () => void;
  onEnable: () => void;
  onDisable: () => void;
  onClearSelection: () => void;
}

export const BulkActionsBar: React.FC<BulkActionsBarProps> = ({
  selectedCount,
  onDelete,
  onEnable,
  onDisable,
  onClearSelection,
}) => {
  if (selectedCount === 0) return null;

  return (
    <div className="fixed bottom-8 left-1/2 transform -translate-x-1/2 z-50">
      <div className="bg-white border border-gray-200 rounded-lg shadow-lg px-6 py-4 flex items-center space-x-4">
        <span className="text-sm font-medium text-gray-700">
          {selectedCount} device{selectedCount > 1 ? 's' : ''} selected
        </span>

        <div className="h-6 w-px bg-gray-300" />

        <button
          onClick={onEnable}
          className="flex items-center space-x-2 px-3 py-1.5 text-sm font-medium text-green-700 hover:bg-green-50 rounded-md transition-colors"
          title="Enable selected devices"
        >
          <Power className="h-4 w-4" />
          <span>Enable</span>
        </button>

        <button
          onClick={onDisable}
          className="flex items-center space-x-2 px-3 py-1.5 text-sm font-medium text-orange-700 hover:bg-orange-50 rounded-md transition-colors"
          title="Disable selected devices"
        >
          <PowerOff className="h-4 w-4" />
          <span>Disable</span>
        </button>

        <button
          onClick={onDelete}
          className="flex items-center space-x-2 px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-50 rounded-md transition-colors"
          title="Delete selected devices"
        >
          <Trash2 className="h-4 w-4" />
          <span>Delete</span>
        </button>

        <div className="h-6 w-px bg-gray-300" />

        <button
          onClick={onClearSelection}
          className="text-sm text-gray-500 hover:text-gray-700"
        >
          Clear
        </button>
      </div>
    </div>
  );
};
