import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { CannedResponsePicker } from './CannedResponsePicker';
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

describe('CannedResponsePicker', () => {
  const mockOnSelect = vi.fn();
  const mockTemplates = [
    {
      id: 1,
      title: 'Welcome Message',
      body: 'Thank you for contacting support!',
      category: 'GENERAL',
      useCount: 5,
    },
    {
      id: 2,
      title: 'Password Reset',
      body: 'To reset your password...',
      category: 'AUTHENTICATION',
      useCount: 10,
    },
    {
      id: 3,
      title: 'Bug Confirmed',
      body: 'We have confirmed this is a bug...',
      category: 'BUG',
      useCount: 3,
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(apiService.getCannedResponses).mockResolvedValue(mockTemplates);
    vi.mocked(apiService.markCannedResponseAsUsed).mockResolvedValue(undefined);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('should render the button', () => {
    render(<CannedResponsePicker onSelect={mockOnSelect} />);

    const button = screen.getByText('Use Template');
    expect(button).toBeInTheDocument();
  });

  it('should open dropdown when button is clicked', async () => {
    render(<CannedResponsePicker onSelect={mockOnSelect} />);

    const button = screen.getByText('Use Template');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Canned Responses')).toBeInTheDocument();
    });
  });

  it('should load templates when dropdown opens', async () => {
    render(<CannedResponsePicker onSelect={mockOnSelect} />);

    const button = screen.getByText('Use Template');
    fireEvent.click(button);

    await waitFor(() => {
      expect(apiService.getCannedResponses).toHaveBeenCalledWith({});
    });
  });

  it('should display all templates in the dropdown', async () => {
    render(<CannedResponsePicker onSelect={mockOnSelect} />);

    const button = screen.getByText('Use Template');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Welcome Message')).toBeInTheDocument();
      expect(screen.getByText('Password Reset')).toBeInTheDocument();
      expect(screen.getByText('Bug Confirmed')).toBeInTheDocument();
    });
  });

  it('should display template use counts', async () => {
    render(<CannedResponsePicker onSelect={mockOnSelect} />);

    const button = screen.getByText('Use Template');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('5')).toBeInTheDocument();
      expect(screen.getByText('10')).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument();
    });
  });

  it('should filter templates by category', async () => {
    vi.mocked(apiService.getCannedResponses).mockResolvedValue([mockTemplates[1]]);

    render(<CannedResponsePicker onSelect={mockOnSelect} />);

    const button = screen.getByText('Use Template');
    fireEvent.click(button);

    await waitFor(() => {
      const categorySelect = screen.getByRole('combobox');
      fireEvent.change(categorySelect, { target: { value: 'AUTHENTICATION' } });
    });

    await waitFor(() => {
      expect(apiService.getCannedResponses).toHaveBeenCalledWith({ category: 'AUTHENTICATION' });
    });
  });

  it('should call onSelect when template is clicked', async () => {
    render(<CannedResponsePicker onSelect={mockOnSelect} />);

    const button = screen.getByText('Use Template');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Welcome Message')).toBeInTheDocument();
    });

    const templateButton = screen.getByText('Welcome Message').closest('button');
    fireEvent.click(templateButton!);

    await waitFor(() => {
      expect(mockOnSelect).toHaveBeenCalledWith('Thank you for contacting support!');
    });
  });

  it('should track template usage when selected', async () => {
    render(<CannedResponsePicker onSelect={mockOnSelect} />);

    const button = screen.getByText('Use Template');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Password Reset')).toBeInTheDocument();
    });

    const templateButton = screen.getByText('Password Reset').closest('button');
    fireEvent.click(templateButton!);

    await waitFor(() => {
      expect(apiService.markCannedResponseAsUsed).toHaveBeenCalledWith(2);
    });
  });

  it('should close dropdown after template selection', async () => {
    render(<CannedResponsePicker onSelect={mockOnSelect} />);

    const button = screen.getByText('Use Template');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Welcome Message')).toBeInTheDocument();
    });

    const templateButton = screen.getByText('Welcome Message').closest('button');
    fireEvent.click(templateButton!);

    await waitFor(() => {
      expect(screen.queryByText('Canned Responses')).not.toBeInTheDocument();
    });
  });

  it('should show loading state while fetching templates', async () => {
    vi.mocked(apiService.getCannedResponses).mockImplementation(
      () => new Promise(resolve => setTimeout(() => resolve(mockTemplates), 100))
    );

    render(<CannedResponsePicker onSelect={mockOnSelect} />);

    const button = screen.getByText('Use Template');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Loading templates...')).toBeInTheDocument();
    });
  });

  it('should show empty state when no templates found', async () => {
    vi.mocked(apiService.getCannedResponses).mockResolvedValue([]);

    render(<CannedResponsePicker onSelect={mockOnSelect} />);

    const button = screen.getByText('Use Template');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('No templates found')).toBeInTheDocument();
    });
  });

  it('should close dropdown when backdrop is clicked', async () => {
    render(<CannedResponsePicker onSelect={mockOnSelect} />);

    const button = screen.getByText('Use Template');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Canned Responses')).toBeInTheDocument();
    });

    // Click backdrop
    const backdrop = document.querySelector('.fixed.inset-0.z-\\[100\\]');
    fireEvent.click(backdrop!);

    await waitFor(() => {
      expect(screen.queryByText('Canned Responses')).not.toBeInTheDocument();
    });
  });

  it('should close dropdown when X button is clicked', async () => {
    render(<CannedResponsePicker onSelect={mockOnSelect} />);

    const button = screen.getByText('Use Template');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Canned Responses')).toBeInTheDocument();
    });

    // Find close button (X icon button)
    const closeButtons = screen.getAllByRole('button');
    const closeButton = closeButtons.find(btn =>
      btn.querySelector('svg') && btn.className.includes('text-gray-400')
    );
    fireEvent.click(closeButton!);

    await waitFor(() => {
      expect(screen.queryByText('Canned Responses')).not.toBeInTheDocument();
    });
  });

  it('should apply custom button className', () => {
    const customClass = 'custom-button-class';
    render(<CannedResponsePicker onSelect={mockOnSelect} buttonClassName={customClass} />);

    const button = screen.getByText('Use Template').closest('button');
    expect(button).toHaveClass(customClass);
  });

  it('should handle API error gracefully', async () => {
    vi.mocked(apiService.getCannedResponses).mockRejectedValue(new Error('API Error'));

    render(<CannedResponsePicker onSelect={mockOnSelect} />);

    const button = screen.getByText('Use Template');
    fireEvent.click(button);

    // Component should still be functional despite the error
    await waitFor(() => {
      expect(apiService.getCannedResponses).toHaveBeenCalled();
    });
  });

  it('should not show error to user if usage tracking fails', async () => {
    vi.mocked(apiService.markCannedResponseAsUsed).mockRejectedValue(new Error('Tracking failed'));

    render(<CannedResponsePicker onSelect={mockOnSelect} />);

    const button = screen.getByText('Use Template');
    fireEvent.click(button);

    await waitFor(() => {
      expect(screen.getByText('Welcome Message')).toBeInTheDocument();
    });

    const templateButton = screen.getByText('Welcome Message').closest('button');
    fireEvent.click(templateButton!);

    // onSelect should still be called even if tracking fails
    await waitFor(() => {
      expect(mockOnSelect).toHaveBeenCalledWith('Thank you for contacting support!');
    });
  });
});
