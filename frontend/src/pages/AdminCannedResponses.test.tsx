import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AdminCannedResponses } from './AdminCannedResponses';
import { apiService } from '../services/api';

// Mock the API service
vi.mock('../services/api');

// Mock react-hot-toast
vi.mock('react-hot-toast', () => ({
  default: {
    error: vi.fn(),
    success: vi.fn(),
  },
}));

// Mock window.confirm
window.confirm = vi.fn();

describe('AdminCannedResponses', () => {
  const mockTemplates = [
    {
      id: 1,
      title: 'Welcome Message',
      body: 'Thank you for contacting support!',
      category: 'GENERAL',
      active: true,
      useCount: 5,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z',
    },
    {
      id: 2,
      title: 'Password Reset',
      body: 'To reset your password...',
      category: 'AUTHENTICATION',
      active: true,
      useCount: 10,
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-02T00:00:00Z',
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    (window.confirm as any).mockReturnValue(true);
    vi.mocked(apiService.getCannedResponses).mockResolvedValue(mockTemplates);
    vi.mocked(apiService.createCannedResponse).mockResolvedValue(mockTemplates[0]);
    vi.mocked(apiService.updateCannedResponse).mockResolvedValue(mockTemplates[0]);
    vi.mocked(apiService.deleteCannedResponse).mockResolvedValue(undefined);
  });

  it('should render page title', async () => {
    render(<AdminCannedResponses />);

    expect(screen.getByText('Canned Responses')).toBeInTheDocument();
  });

  it('should load and display templates', async () => {
    render(<AdminCannedResponses />);

    await waitFor(() => {
      expect(screen.getByText('Welcome Message')).toBeInTheDocument();
      expect(screen.getByText('Password Reset')).toBeInTheDocument();
    });
  });

  it('should filter by category', async () => {
    vi.mocked(apiService.getCannedResponses).mockResolvedValue([mockTemplates[1]]);

    render(<AdminCannedResponses />);

    const dropdown = screen.getByRole('combobox');
    fireEvent.change(dropdown, { target: { value: 'AUTHENTICATION' } });

    await waitFor(() => {
      expect(apiService.getCannedResponses).toHaveBeenCalledWith({
        category: 'AUTHENTICATION',
        includeInactive: true
      });
    });
  });

  it('should open create modal', async () => {
    render(<AdminCannedResponses />);

    const newButton = screen.getByText('New Template');
    fireEvent.click(newButton);

    await waitFor(() => {
      expect(screen.getByText('Create New Template')).toBeInTheDocument();
    });
  });

  it('should create template', async () => {
    render(<AdminCannedResponses />);

    const newButton = screen.getByText('New Template');
    fireEvent.click(newButton);

    await waitFor(() => {
      const titleInput = screen.getByPlaceholderText('e.g., Welcome Message');
      const bodyTextarea = screen.getByPlaceholderText('Type your template message here...');

      fireEvent.change(titleInput, { target: { value: 'New Template' } });
      fireEvent.change(bodyTextarea, { target: { value: 'New body' } });
    });

    const createButton = screen.getByText('Create Template');
    fireEvent.click(createButton);

    await waitFor(() => {
      expect(apiService.createCannedResponse).toHaveBeenCalled();
    });
  });

  it('should delete template when confirmed', async () => {
    render(<AdminCannedResponses />);

    await waitFor(() => {
      expect(screen.getByText('Welcome Message')).toBeInTheDocument();
    });

    const deleteButtons = screen.getAllByRole('button', { name: /delete template/i });
    fireEvent.click(deleteButtons[0]);

    await waitFor(() => {
      expect(apiService.deleteCannedResponse).toHaveBeenCalledWith(1);
    });
  });
});
