import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TrainingProgressModal } from './TrainingProgressModal';
import { trainingJobsApi, TrainingJob } from '../../services/mlService';

// Mock react-hot-toast
vi.mock('react-hot-toast', () => ({
  default: {
    success: vi.fn(),
    error: vi.fn(),
  },
  toast: vi.fn(),
}));

// Mock the mlService
vi.mock('../../services/mlService', () => ({
  trainingJobsApi: {
    get: vi.fn(),
    cancel: vi.fn(),
  },
  getTrainingJobStatusLabel: vi.fn((status: string) => {
    const labels: Record<string, string> = {
      PENDING: 'Pending',
      RUNNING: 'Running',
      COMPLETED: 'Completed',
      FAILED: 'Failed',
      CANCELLED: 'Cancelled',
    };
    return labels[status] || status;
  }),
  getTrainingJobStatusColor: vi.fn((status: string) => {
    const colors: Record<string, string> = {
      PENDING: 'yellow',
      RUNNING: 'blue',
      COMPLETED: 'green',
      FAILED: 'red',
      CANCELLED: 'gray',
    };
    return colors[status] || 'gray';
  }),
  getTrainingJobTypeLabel: vi.fn((type: string) => {
    const labels: Record<string, string> = {
      INITIAL_TRAINING: 'Initial Training',
      RETRAINING: 'Retraining',
      HYPERPARAMETER_TUNING: 'Hyperparameter Tuning',
    };
    return labels[type] || type;
  }),
  isTrainingJobTerminal: vi.fn((status: string) =>
    ['COMPLETED', 'FAILED', 'CANCELLED'].includes(status)
  ),
  isTrainingJobActive: vi.fn((status: string) =>
    ['PENDING', 'RUNNING'].includes(status)
  ),
}));

describe('TrainingProgressModal', () => {
  const mockRunningJob: TrainingJob = {
    id: 'job-123',
    modelId: 'model-456',
    organizationId: 1,
    jobType: 'INITIAL_TRAINING',
    status: 'RUNNING',
    trainingConfig: {},
    progressPercent: 50,
    currentStep: 'Training model',
    recordCount: 10000,
    deviceCount: 5,
    startedAt: '2024-01-01T10:00:00Z',
    createdAt: '2024-01-01T09:55:00Z',
  };

  const mockCompletedJob: TrainingJob = {
    ...mockRunningJob,
    status: 'COMPLETED',
    progressPercent: 100,
    currentStep: 'Complete',
    completedAt: '2024-01-01T11:00:00Z',
    durationSeconds: 3600,
    resultMetrics: {
      accuracy: 0.95,
      f1_score: 0.92,
    },
  };

  const mockFailedJob: TrainingJob = {
    ...mockRunningJob,
    status: 'FAILED',
    errorMessage: 'Out of memory error',
  };

  const defaultProps = {
    jobId: 'job-123',
    modelName: 'Test Anomaly Model',
    isOpen: true,
    onClose: vi.fn(),
    onComplete: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Rendering', () => {
    it('should render modal when isOpen is true', async () => {
      (trainingJobsApi.get as any).mockResolvedValue(mockRunningJob);
      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Training Progress')).toBeInTheDocument();
        expect(screen.getByText('Test Anomaly Model')).toBeInTheDocument();
      });
    });

    it('should not render modal when isOpen is false', () => {
      render(<TrainingProgressModal {...defaultProps} isOpen={false} />);

      expect(screen.queryByText('Training Progress')).not.toBeInTheDocument();
    });

    it('should display loading state initially', () => {
      (trainingJobsApi.get as any).mockResolvedValue(mockRunningJob);
      render(<TrainingProgressModal {...defaultProps} />);

      expect(screen.getByText('Loading job status...')).toBeInTheDocument();
    });

    it('should display job status after loading', async () => {
      (trainingJobsApi.get as any).mockResolvedValue(mockRunningJob);
      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Running')).toBeInTheDocument();
        expect(screen.getByText('50%')).toBeInTheDocument();
      });
    });

    it('should display current step when available', async () => {
      (trainingJobsApi.get as any).mockResolvedValue(mockRunningJob);
      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Training model')).toBeInTheDocument();
      });
    });

    it('should display job type', async () => {
      (trainingJobsApi.get as any).mockResolvedValue(mockRunningJob);
      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Initial Training')).toBeInTheDocument();
      });
    });

    it('should display training data stats when available', async () => {
      (trainingJobsApi.get as any).mockResolvedValue(mockRunningJob);
      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('10,000')).toBeInTheDocument(); // recordCount
        expect(screen.getByText('5')).toBeInTheDocument(); // deviceCount
      });
    });
  });

  describe('Progress Bar', () => {
    it('should display progress bar with correct width', async () => {
      (trainingJobsApi.get as any).mockResolvedValue(mockRunningJob);
      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        const progressBar = document.querySelector('[style*="width: 50%"]');
        expect(progressBar).toBeInTheDocument();
      });
    });

    it('should show 0% progress for pending jobs', async () => {
      (trainingJobsApi.get as any).mockResolvedValue({
        ...mockRunningJob,
        status: 'PENDING',
        progressPercent: 0,
      });

      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('0%')).toBeInTheDocument();
      });
    });

    it('should show 100% progress for completed jobs', async () => {
      (trainingJobsApi.get as any).mockResolvedValue(mockCompletedJob);

      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('100%')).toBeInTheDocument();
      });
    });
  });

  describe('Cancel Functionality', () => {
    it('should show cancel button for active jobs', async () => {
      (trainingJobsApi.get as any).mockResolvedValue(mockRunningJob);
      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Cancel Training')).toBeInTheDocument();
      });
    });

    it('should not show cancel button for completed jobs', async () => {
      (trainingJobsApi.get as any).mockResolvedValue(mockCompletedJob);

      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.queryByText('Cancel Training')).not.toBeInTheDocument();
      });
    });

    it('should call cancel API when cancel button is clicked and confirmed', async () => {
      const user = userEvent.setup();
      vi.spyOn(window, 'confirm').mockReturnValue(true);
      (trainingJobsApi.get as any).mockResolvedValue(mockRunningJob);
      (trainingJobsApi.cancel as any).mockResolvedValue({});

      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Cancel Training')).toBeInTheDocument();
      });

      await user.click(screen.getByText('Cancel Training'));

      await waitFor(() => {
        expect(trainingJobsApi.cancel).toHaveBeenCalledWith('job-123');
      });
    });

    it('should not call cancel API when cancel is not confirmed', async () => {
      const user = userEvent.setup();
      vi.spyOn(window, 'confirm').mockReturnValue(false);
      (trainingJobsApi.get as any).mockResolvedValue(mockRunningJob);

      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Cancel Training')).toBeInTheDocument();
      });

      await user.click(screen.getByText('Cancel Training'));

      expect(trainingJobsApi.cancel).not.toHaveBeenCalled();
    });
  });

  describe('Error Display', () => {
    it('should display error message for failed jobs', async () => {
      (trainingJobsApi.get as any).mockResolvedValue(mockFailedJob);

      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Training Failed')).toBeInTheDocument();
        expect(screen.getByText('Out of memory error')).toBeInTheDocument();
      });
    });

    it('should display error state when API call fails', async () => {
      (trainingJobsApi.get as any).mockRejectedValue(new Error('Network error'));

      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Error loading job status')).toBeInTheDocument();
        expect(screen.getByText('Network error')).toBeInTheDocument();
      });
    });
  });

  describe('Modal Close', () => {
    it('should call onClose when X button is clicked for completed job', async () => {
      const user = userEvent.setup();
      (trainingJobsApi.get as any).mockResolvedValue(mockCompletedJob);

      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Close')).toBeInTheDocument();
      });

      await user.click(screen.getByText('Close'));

      expect(defaultProps.onClose).toHaveBeenCalled();
    });

    it('should show "Close" button for completed jobs', async () => {
      (trainingJobsApi.get as any).mockResolvedValue(mockCompletedJob);

      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Close')).toBeInTheDocument();
        expect(screen.queryByText('Close (Continue in Background)')).not.toBeInTheDocument();
      });
    });

    it('should show "Close (Continue in Background)" for active jobs', async () => {
      (trainingJobsApi.get as any).mockResolvedValue(mockRunningJob);

      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Close (Continue in Background)')).toBeInTheDocument();
      });
    });
  });

  describe('Result Metrics', () => {
    it('should display training result metrics when available', async () => {
      (trainingJobsApi.get as any).mockResolvedValue(mockCompletedJob);

      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Training Results')).toBeInTheDocument();
        expect(screen.getByText('0.9500')).toBeInTheDocument(); // accuracy
        expect(screen.getByText('0.9200')).toBeInTheDocument(); // f1_score
      });
    });
  });

  describe('Duration Display', () => {
    it('should display duration when available', async () => {
      (trainingJobsApi.get as any).mockResolvedValue({
        ...mockCompletedJob,
        durationSeconds: 3665, // 1h 1m 5s
      });

      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('1h 1m')).toBeInTheDocument();
      });
    });

    it('should display start time when available', async () => {
      (trainingJobsApi.get as any).mockResolvedValue(mockRunningJob);
      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Started:')).toBeInTheDocument();
      });
    });
  });

  describe('Auto-updating indicator', () => {
    it('should display auto-updating message for active jobs', async () => {
      (trainingJobsApi.get as any).mockResolvedValue(mockRunningJob);
      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.getByText('Auto-updating every 2s')).toBeInTheDocument();
      });
    });

    it('should not display auto-updating message for completed jobs', async () => {
      (trainingJobsApi.get as any).mockResolvedValue(mockCompletedJob);
      render(<TrainingProgressModal {...defaultProps} />);

      await waitFor(() => {
        expect(screen.queryByText('Auto-updating every 2s')).not.toBeInTheDocument();
      });
    });
  });
});
