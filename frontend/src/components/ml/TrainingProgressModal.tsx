import { useEffect, useState, useCallback, useRef } from 'react';
import {
  X,
  RefreshCw,
  CheckCircle,
  XCircle,
  AlertTriangle,
  Loader2,
  StopCircle,
  BarChart3,
  Clock,
  Database,
  Cpu,
} from 'lucide-react';
import { clsx } from 'clsx';
import {
  trainingJobsApi,
  TrainingJob,
  getTrainingJobStatusLabel,
  getTrainingJobStatusColor,
  getTrainingJobTypeLabel,
  isTrainingJobTerminal,
  isTrainingJobActive,
} from '../../services/mlService';
import toast from 'react-hot-toast';

/** Polling interval in milliseconds */
const POLLING_INTERVAL_MS = 2000;

interface TrainingProgressModalProps {
  /** The training job ID to monitor */
  jobId: string;
  /** The model name for display */
  modelName: string;
  /** Whether the modal is open */
  isOpen: boolean;
  /** Callback when the modal should close */
  onClose: () => void;
  /** Callback when training completes (success or failure) */
  onComplete?: (job: TrainingJob) => void;
}

/**
 * Modal component that displays real-time training progress.
 * Features:
 * - Live polling for job status (every 2 seconds)
 * - Visual progress bar with percentage
 * - Current phase/step display
 * - Training metrics when available
 * - Cancel functionality
 * - Error display on failure
 * - Auto-notification on completion
 */
export const TrainingProgressModal = ({
  jobId,
  modelName,
  isOpen,
  onClose,
  onComplete,
}: TrainingProgressModalProps) => {
  const [job, setJob] = useState<TrainingJob | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState(false);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);
  const hasNotifiedRef = useRef(false);
  // Use ref to track terminal status to avoid stale closure in interval
  const isTerminalRef = useRef(false);
  const modalRef = useRef<HTMLDivElement>(null);

  // Fetch job status - stable callback that doesn't depend on job state
  const fetchJob = useCallback(async () => {
    if (!jobId) return;

    try {
      const updatedJob = await trainingJobsApi.get(jobId);
      setJob(updatedJob);
      setError(null);

      // Update terminal ref for interval check
      const isTerminal = isTrainingJobTerminal(updatedJob.status);
      isTerminalRef.current = isTerminal;

      // Check if job completed and notify once
      if (isTerminal && !hasNotifiedRef.current) {
        hasNotifiedRef.current = true;

        // Stop polling immediately when terminal
        if (intervalRef.current) {
          clearInterval(intervalRef.current);
          intervalRef.current = null;
        }

        if (updatedJob.status === 'COMPLETED') {
          toast.success(`Training completed for "${modelName}"`);
        } else if (updatedJob.status === 'FAILED') {
          toast.error(`Training failed for "${modelName}"`);
        } else if (updatedJob.status === 'CANCELLED') {
          toast('Training cancelled', { icon: '🛑' });
        }

        onComplete?.(updatedJob);
      }
    } catch (err) {
      console.error('Failed to fetch job status:', err);
      setError(err instanceof Error ? err.message : 'Failed to fetch job status');
      // Stop polling on error to prevent hammering a failing endpoint
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    } finally {
      setLoading(false);
    }
  }, [jobId, modelName, onComplete]);

  // Set up polling - uses ref to check terminal status (no stale closure)
  useEffect(() => {
    if (!isOpen || !jobId) return;

    // Reset state for new job
    hasNotifiedRef.current = false;
    isTerminalRef.current = false;
    setLoading(true);
    setError(null);
    setJob(null);

    // Initial fetch
    fetchJob();

    // Set up polling interval
    // Uses ref to check terminal status - avoids stale closure bug
    intervalRef.current = setInterval(() => {
      // Check ref instead of state to avoid stale closure
      if (isTerminalRef.current) {
        if (intervalRef.current) {
          clearInterval(intervalRef.current);
          intervalRef.current = null;
        }
        return;
      }
      fetchJob();
    }, POLLING_INTERVAL_MS);

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [isOpen, jobId, fetchJob]);

  // Handle close - ask for confirmation if job is still running
  // Defined early so it can be used in the ESC key effect
  // Wrapped in useCallback to avoid stale closures in event handlers
  const handleClose = useCallback(() => {
    if (job && isTrainingJobActive(job.status)) {
      if (!window.confirm('Training is still in progress. The job will continue running in the background. Close anyway?')) {
        return;
      }
    }
    onClose();
  }, [job, onClose]);

  // Handle ESC key to close modal (accessibility)
  useEffect(() => {
    const handleKeyDown = (event: globalThis.KeyboardEvent) => {
      if (event.key === 'Escape' && isOpen) {
        handleClose();
      }
    };

    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
      // Focus the modal when it opens for accessibility
      modalRef.current?.focus();
    }

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, handleClose]);

  // Handle cancel
  const handleCancel = async () => {
    if (!job || cancelling) return;

    if (!window.confirm('Are you sure you want to cancel this training job?')) {
      return;
    }

    try {
      setCancelling(true);
      await trainingJobsApi.cancel(jobId);
      toast('Training job cancelled', { icon: '🛑' });
      fetchJob();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to cancel job';
      toast.error(message);
    } finally {
      setCancelling(false);
    }
  };

  if (!isOpen) return null;

  const progressPercent = job?.progressPercent ?? 0;
  const isActive = job && isTrainingJobActive(job.status);
  const isTerminal = job && isTrainingJobTerminal(job.status);

  // Status color mapping
  const statusColorMap: Record<string, string> = {
    yellow: 'bg-yellow-100 text-yellow-800',
    blue: 'bg-blue-100 text-blue-800',
    green: 'bg-green-100 text-green-800',
    red: 'bg-red-100 text-red-800',
    gray: 'bg-gray-100 text-gray-800',
  };

  // Progress bar color based on status
  const getProgressBarColor = () => {
    if (!job) return 'bg-blue-500';
    switch (job.status) {
      case 'COMPLETED': return 'bg-green-500';
      case 'FAILED': return 'bg-red-500';
      case 'CANCELLED': return 'bg-gray-500';
      default: return 'bg-blue-500';
    }
  };

  // Status icon
  const getStatusIcon = () => {
    if (!job) return <Loader2 className="h-6 w-6 animate-spin text-blue-500" />;
    switch (job.status) {
      case 'PENDING': return <Clock className="h-6 w-6 text-yellow-500" />;
      case 'RUNNING': return <Loader2 className="h-6 w-6 animate-spin text-blue-500" />;
      case 'COMPLETED': return <CheckCircle className="h-6 w-6 text-green-500" />;
      case 'FAILED': return <XCircle className="h-6 w-6 text-red-500" />;
      case 'CANCELLED': return <StopCircle className="h-6 w-6 text-gray-500" />;
      default: return <AlertTriangle className="h-6 w-6 text-yellow-500" />;
    }
  };

  return (
    <div
      className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      role="presentation"
      onClick={(e) => {
        // Close modal when clicking backdrop (outside modal content)
        if (e.target === e.currentTarget) {
          handleClose();
        }
      }}
    >
      <div
        ref={modalRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="training-modal-title"
        aria-describedby="training-modal-description"
        tabIndex={-1}
        className="bg-primary rounded-lg shadow-xl w-full max-w-lg mx-4 max-h-[90vh] overflow-hidden flex flex-col"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-default">
          <div className="flex items-center gap-3">
            {getStatusIcon()}
            <div>
              <h2 id="training-modal-title" className="text-lg font-semibold text-primary">
                Training Progress
              </h2>
              <p id="training-modal-description" className="text-sm text-secondary">
                {modelName}
              </p>
            </div>
          </div>
          <button
            onClick={handleClose}
            className="p-2 text-secondary hover:text-primary hover:bg-hover rounded-lg transition-colors"
            aria-label="Close training progress modal"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto px-6 py-4 space-y-6">
          {/* Loading State */}
          {loading && !job && (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
              <span className="ml-2 text-secondary">Loading job status...</span>
            </div>
          )}

          {/* Error State */}
          {error && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-4">
              <div className="flex items-center gap-2 text-red-800">
                <XCircle className="h-5 w-5" />
                <span className="font-medium">Error loading job status</span>
              </div>
              <p className="text-red-600 text-sm mt-1">{error}</p>
            </div>
          )}

          {/* Job Details */}
          {job && (
            <>
              {/* Status Badge */}
              <div className="flex items-center justify-between">
                <span className="text-sm text-secondary">Status</span>
                <span className={clsx(
                  'px-3 py-1 text-sm font-medium rounded-full',
                  statusColorMap[getTrainingJobStatusColor(job.status)] || 'bg-gray-100 text-gray-800'
                )}>
                  {getTrainingJobStatusLabel(job.status)}
                </span>
              </div>

              {/* Progress Bar */}
              <div className="space-y-2">
                <div className="flex items-center justify-between text-sm">
                  <span className="text-secondary">Progress</span>
                  <span className="font-medium text-primary">{progressPercent}%</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
                  <div
                    className={clsx('h-full transition-all duration-500 ease-out', getProgressBarColor())}
                    style={{ width: `${progressPercent}%` }}
                  />
                </div>
              </div>

              {/* Current Step */}
              {job.currentStep && (
                <div className="flex items-center gap-2 text-sm">
                  <Cpu className="h-4 w-4 text-secondary" />
                  <span className="text-secondary">Current Step:</span>
                  <span className="font-medium text-primary">{job.currentStep}</span>
                </div>
              )}

              {/* Job Type */}
              <div className="flex items-center gap-2 text-sm">
                <BarChart3 className="h-4 w-4 text-secondary" />
                <span className="text-secondary">Job Type:</span>
                <span className="font-medium text-primary">{getTrainingJobTypeLabel(job.jobType)}</span>
              </div>

              {/* Data Stats */}
              {(job.recordCount || job.deviceCount) && (
                <div className="bg-hover rounded-lg p-4">
                  <div className="flex items-center gap-2 mb-2">
                    <Database className="h-4 w-4 text-secondary" />
                    <span className="text-sm font-medium text-primary">Training Data</span>
                  </div>
                  <div className="grid grid-cols-2 gap-4 text-sm">
                    {job.recordCount && (
                      <div>
                        <span className="text-secondary">Records:</span>
                        <span className="ml-2 font-medium text-primary">{job.recordCount.toLocaleString()}</span>
                      </div>
                    )}
                    {job.deviceCount && (
                      <div>
                        <span className="text-secondary">Devices:</span>
                        <span className="ml-2 font-medium text-primary">{job.deviceCount}</span>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Timing Information */}
              {(job.startedAt || job.completedAt || job.durationSeconds) && (
                <div className="space-y-1 text-sm">
                  {job.startedAt && (
                    <div className="flex items-center gap-2">
                      <Clock className="h-4 w-4 text-secondary" />
                      <span className="text-secondary">Started:</span>
                      <span className="text-primary">{new Date(job.startedAt).toLocaleString()}</span>
                    </div>
                  )}
                  {job.completedAt && (
                    <div className="flex items-center gap-2">
                      <Clock className="h-4 w-4 text-secondary" />
                      <span className="text-secondary">Completed:</span>
                      <span className="text-primary">{new Date(job.completedAt).toLocaleString()}</span>
                    </div>
                  )}
                  {job.durationSeconds && (
                    <div className="flex items-center gap-2">
                      <Clock className="h-4 w-4 text-secondary" />
                      <span className="text-secondary">Duration:</span>
                      <span className="text-primary">{formatDuration(job.durationSeconds)}</span>
                    </div>
                  )}
                </div>
              )}

              {/* Result Metrics */}
              {job.resultMetrics && Object.keys(job.resultMetrics).length > 0 && (
                <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                  <h3 className="text-sm font-medium text-green-800 mb-2">Training Results</h3>
                  <div className="space-y-1 text-sm">
                    {Object.entries(job.resultMetrics).map(([key, value]) => (
                      <div key={key} className="flex justify-between">
                        <span className="text-green-700">{formatMetricKey(key)}:</span>
                        <span className="font-medium text-green-900">{formatMetricValue(value)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Error Message */}
              {job.errorMessage && (
                <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                  <div className="flex items-center gap-2 text-red-800 mb-2">
                    <XCircle className="h-5 w-5" />
                    <span className="font-medium">Training Failed</span>
                  </div>
                  <p className="text-red-600 text-sm">{job.errorMessage}</p>
                </div>
              )}
            </>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-default bg-hover">
          <div className="flex items-center gap-2">
            {isActive && (
              <span className="flex items-center gap-1 text-sm text-secondary">
                <RefreshCw className="h-3 w-3 animate-spin" />
                Auto-updating every 2s
              </span>
            )}
          </div>
          <div className="flex items-center gap-3">
            {/* Cancel button - only show for active jobs */}
            {isActive && (
              <button
                onClick={handleCancel}
                disabled={cancelling}
                className="flex items-center gap-2 px-4 py-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-50"
              >
                {cancelling ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <StopCircle className="h-4 w-4" />
                )}
                Cancel Training
              </button>
            )}

            {/* Close button */}
            <button
              onClick={handleClose}
              className="px-4 py-2 bg-link text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              {isTerminal ? 'Close' : 'Close (Continue in Background)'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// Helper functions

function formatDuration(seconds: number): string {
  if (seconds < 60) return `${seconds}s`;
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ${seconds % 60}s`;
  const hours = Math.floor(seconds / 3600);
  const mins = Math.floor((seconds % 3600) / 60);
  return `${hours}h ${mins}m`;
}

function formatMetricKey(key: string): string {
  return key
    .replace(/_/g, ' ')
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, str => str.toUpperCase())
    .trim();
}

function formatMetricValue(value: unknown): string {
  if (typeof value === 'number') {
    return value % 1 === 0 ? value.toString() : value.toFixed(4);
  }
  return String(value);
}

export default TrainingProgressModal;
