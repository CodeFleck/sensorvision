import React, { useState } from 'react';
import { Widget } from '../../types';
import { apiService } from '../../services/api';

interface CommandConfig {
  commandType?: string;
  command?: string;
  payload?: unknown;
  method?: string;
  url?: string;
  httpMethod?: string;
  headers?: Record<string, string>;
  topic?: string;
}

interface ControlButtonWidgetProps {
  widget: Widget;
  deviceId?: string;
}

export const ControlButtonWidget: React.FC<ControlButtonWidgetProps> = ({ widget, deviceId }) => {
  const [isExecuting, setIsExecuting] = useState(false);
  const [lastResult, setLastResult] = useState<{ success: boolean; message: string } | null>(null);

  const handleButtonClick = async () => {
    setIsExecuting(true);
    setLastResult(null);

    try {
      // Get command configuration
      const command = (widget.config.command as CommandConfig | undefined) || {};
      const { commandType, payload, method = 'mqtt' } = command;

      if (!deviceId) {
        setLastResult({
          success: false,
          message: 'No device ID configured for this widget',
        });
        return;
      }

      if (method === 'mqtt') {
        // Send command via REST API to backend, which publishes via MQTT
        const commandName = commandType || command.command || 'execute';

        try {
          const response = await apiService.post(
            `/devices/${deviceId}/commands`,
            {
              command: commandName,
              payload: payload || null,
            }
          );

          const result = response.data as { success?: boolean; message?: string };
          if (result.success) {
            setLastResult({
              success: true,
              message: result.message || `Command '${commandName}' sent successfully`,
            });
          } else {
            setLastResult({
              success: false,
              message: result.message || 'Command failed',
            });
          }
        } catch (apiError: unknown) {
          console.error('API error sending command:', apiError);
          const message = apiError instanceof Error && 'response' in apiError
            ? ((apiError as { response?: { data?: { message?: string } } }).response?.data?.message || 'Failed to send command')
            : 'Failed to send command';
          setLastResult({
            success: false,
            message,
          });
        }
      } else if (method === 'http') {
        // For HTTP commands, make an API call to external URL
        const { url, httpMethod = 'POST', headers = {} } = command;

        if (!url) {
          setLastResult({
            success: false,
            message: 'No URL configured for HTTP command',
          });
          return;
        }

        const response = await fetch(url, {
          method: httpMethod,
          headers: {
            'Content-Type': 'application/json',
            ...headers,
          },
          body: payload ? JSON.stringify(payload) : undefined,
        });

        if (response.ok) {
          setLastResult({
            success: true,
            message: 'Command executed successfully',
          });
        } else {
          setLastResult({
            success: false,
            message: `Error: ${response.statusText}`,
          });
        }
      } else {
        setLastResult({
          success: false,
          message: 'Unknown command method',
        });
      }
    } catch (error: unknown) {
      console.error('Error executing command:', error);
      setLastResult({
        success: false,
        message: error instanceof Error ? error.message : 'Command execution failed',
      });
    } finally {
      setIsExecuting(false);

      // Clear result message after 3 seconds
      setTimeout(() => setLastResult(null), 3000);
    }
  };

  const buttonStyle = (widget.config.style as string | undefined) || 'primary';
  const buttonSize = (widget.config.size as string | undefined) || 'medium';

  const getButtonClasses = () => {
    let baseClasses = 'font-semibold rounded-lg transition-all duration-200 shadow-lg ';

    // Size classes
    const sizeClasses = {
      small: 'px-4 py-2 text-sm',
      medium: 'px-6 py-3 text-base',
      large: 'px-8 py-4 text-lg',
    };
    baseClasses += sizeClasses[buttonSize as keyof typeof sizeClasses] + ' ';

    // Style classes
    const styleClasses = {
      primary: 'bg-[var(--accent-primary)] hover:bg-[var(--accent-primary-hover)] text-white',
      success: 'bg-[var(--accent-success)] hover:bg-[var(--accent-success-hover)] text-white',
      danger: 'bg-[var(--accent-danger)] hover:bg-[var(--accent-danger-hover)] text-white',
      warning: 'bg-[var(--accent-warning)] hover:bg-[var(--accent-warning-hover)] text-white',
      secondary: 'bg-[var(--bg-secondary)] hover:bg-[var(--bg-hover)] text-[var(--text-primary)] border border-[var(--border-default)]',
    };
    baseClasses += styleClasses[buttonStyle as keyof typeof styleClasses];

    if (isExecuting) {
      baseClasses += ' opacity-70 cursor-not-allowed';
    } else {
      baseClasses += ' hover:shadow-xl active:scale-95';
    }

    return baseClasses;
  };

  const requireConfirmation = (widget.config.requireConfirmation as boolean | undefined) ?? false;

  const handleClick = () => {
    if (requireConfirmation) {
      const confirmed = window.confirm(`Are you sure you want to execute "${widget.name}"?`);
      if (!confirmed) return;
    }
    handleButtonClick();
  };

  const buttonLabel = (widget.config.label as string | undefined) || widget.name || 'Execute';
  const showIcon = (widget.config.showIcon as boolean | undefined) ?? true;

  return (
    <div className="flex flex-col items-center justify-center w-full h-full p-4 space-y-4">
      {/* Button */}
      <button
        onClick={handleClick}
        disabled={isExecuting}
        className={getButtonClasses()}
      >
        <div className="flex items-center justify-center space-x-2">
          {showIcon && !isExecuting && (
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M13 10V3L4 14h7v7l9-11h-7z"
              />
            </svg>
          )}
          {isExecuting && (
            <svg className="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
              <circle
                className="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="4"
              />
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
              />
            </svg>
          )}
          <span>{isExecuting ? 'Executing...' : buttonLabel}</span>
        </div>
      </button>

      {/* Description */}
      {(widget.config.description as string | undefined) && (
        <div className="text-sm text-gray-400 text-center max-w-xs">
          {widget.config.description as string}
        </div>
      )}

      {/* Result Message */}
      {lastResult && (
        <div
          className={`text-sm font-medium px-4 py-2 rounded ${
            lastResult.success
              ? 'bg-green-500/20 text-green-400'
              : 'bg-red-500/20 text-red-400'
          }`}
        >
          {lastResult.message}
        </div>
      )}

      {/* Command Info */}
      {(widget.config.showCommandInfo as boolean | undefined) && (
        <div className="text-xs text-gray-500 space-y-1">
          <div>Method: {((widget.config.command as CommandConfig | undefined)?.method || 'mqtt')}</div>
          <div>Target: {((widget.config.command as CommandConfig | undefined)?.topic || (widget.config.command as CommandConfig | undefined)?.url || 'N/A')}</div>
        </div>
      )}
    </div>
  );
};
