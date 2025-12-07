import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EditWidgetModal } from './EditWidgetModal';
import { apiService } from '../../services/api';
import type { Widget, Device } from '../../types';

// Mock API service
vi.mock('../../services/api', () => ({
  apiService: {
    getDevices: vi.fn(),
    updateWidget: vi.fn(),
  },
}));

describe('EditWidgetModal', () => {
  const mockDevices: Device[] = [
    {
      id: '550e8400-e29b-41d4-a716-446655440001',
      externalId: 'device-001',
      name: 'Test Device 1',
      status: 'ONLINE' as const,
      location: 'Building A',
    },
    {
      id: '550e8400-e29b-41d4-a716-446655440002',
      externalId: 'device-002',
      name: 'Test Device 2',
      status: 'ONLINE' as const,
      location: 'Building B',
    },
  ];

  const mockWidget: Widget = {
    id: 1,
    dashboardId: 1,
    name: 'Test Widget',
    type: 'GAUGE',
    positionX: 0,
    positionY: 0,
    width: 4,
    height: 4,
    deviceId: 'device-001',
    variableName: 'kwConsumption',
    aggregation: 'LAST',
    timeRangeMinutes: 60,
    config: {},
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  };

  const defaultProps = {
    dashboardId: 1,
    widget: mockWidget,
    isOpen: true,
    onClose: vi.fn(),
    onSuccess: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    (apiService.getDevices as any).mockResolvedValue(mockDevices);
    (apiService.updateWidget as any).mockResolvedValue({});
  });

  it('should render modal when isOpen is true', async () => {
    render(<EditWidgetModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('Edit Widget')).toBeInTheDocument();
    });
  });

  it('should not render modal when isOpen is false', () => {
    render(<EditWidgetModal {...defaultProps} isOpen={false} />);

    expect(screen.queryByText('Edit Widget')).not.toBeInTheDocument();
  });

  it('should always display device selection dropdown (no conditional rendering)', async () => {
    render(<EditWidgetModal {...defaultProps} />);

    await waitFor(() => {
      // Check that "Device *" label exists
      expect(screen.getByText('Device *')).toBeInTheDocument();
      // Check that device dropdown with options exists
      expect(screen.getByText(/Test Device 1 \(device-001\)/i)).toBeInTheDocument();
    });
  });

  it('should NOT display "Use dashboard\'s selected device" toggle (regression test)', async () => {
    render(<EditWidgetModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.queryByText(/Use dashboard's selected device/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/This widget will display data from the device selected in the dashboard dropdown/i)).not.toBeInTheDocument();
    });
  });

  it('should load and display devices in dropdown', async () => {
    render(<EditWidgetModal {...defaultProps} />);

    await waitFor(() => {
      expect(apiService.getDevices).toHaveBeenCalled();
      expect(screen.getByText(/Test Device 1 \(device-001\)/i)).toBeInTheDocument();
      expect(screen.getByText(/Test Device 2 \(device-002\)/i)).toBeInTheDocument();
    });
  });

  it('should pre-populate form with widget data', async () => {
    render(<EditWidgetModal {...defaultProps} />);

    await waitFor(() => {
      const nameInput = screen.getByDisplayValue('Test Widget') as HTMLInputElement;
      expect(nameInput).toBeInTheDocument();
      expect(nameInput.value).toBe('Test Widget');

      const widthInputs = screen.getAllByDisplayValue('4');
      expect(widthInputs.length).toBeGreaterThan(0);
    });
  });

  it('should call updateWidget and callbacks on form submission', async () => {
    const user = userEvent.setup();
    render(<EditWidgetModal {...defaultProps} />);

    // Wait for form to be fully loaded
    await waitFor(() => {
      expect(screen.getByDisplayValue('Test Widget')).toBeInTheDocument();
    });

    // Change widget name
    const nameInput = screen.getByDisplayValue('Test Widget') as HTMLInputElement;
    await user.clear(nameInput);
    await user.type(nameInput, 'Updated Widget Name');

    // Submit form
    const submitButton = screen.getByText('Update Widget');
    await user.click(submitButton);

    await waitFor(() => {
      expect(apiService.updateWidget).toHaveBeenCalledWith(
        1, // dashboardId
        1, // widget.id
        expect.objectContaining({
          name: 'Updated Widget Name',
          deviceId: 'device-001',
        })
      );
      expect(defaultProps.onSuccess).toHaveBeenCalled();
      expect(defaultProps.onClose).toHaveBeenCalled();
    });
  });

  it('should close modal when cancel button is clicked', async () => {
    const user = userEvent.setup();
    render(<EditWidgetModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('Cancel')).toBeInTheDocument();
    });

    const cancelButton = screen.getByText('Cancel');
    await user.click(cancelButton);

    expect(defaultProps.onClose).toHaveBeenCalled();
  });

  it('should close modal when X button is clicked', async () => {
    const user = userEvent.setup();
    render(<EditWidgetModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('Edit Widget')).toBeInTheDocument();
    });

    // Find all buttons and click the first one (close button)
    const buttons = screen.getAllByRole('button');
    const closeButton = buttons[0]; // First button is the X close button
    await user.click(closeButton);

    expect(defaultProps.onClose).toHaveBeenCalled();
  });

  it('should handle API errors gracefully', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => { /* intentionally empty */ });
    const alertSpy = vi.spyOn(window, 'alert').mockImplementation(() => { /* intentionally empty */ });

    (apiService.updateWidget as any).mockRejectedValue(new Error('API Error'));

    const user = userEvent.setup();
    render(<EditWidgetModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByDisplayValue('Test Widget')).toBeInTheDocument();
    });

    const submitButton = screen.getByText('Update Widget');
    await user.click(submitButton);

    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalled();
      expect(alertSpy).toHaveBeenCalledWith('Failed to update widget');
    });

    consoleErrorSpy.mockRestore();
    alertSpy.mockRestore();
  });

  it('should display all widget types in dropdown', async () => {
    render(<EditWidgetModal {...defaultProps} />);

    await waitFor(() => {
      expect(screen.getByText('GAUGE')).toBeInTheDocument();
      expect(screen.getByText('METRIC CARD')).toBeInTheDocument();
      expect(screen.getByText('LINE CHART')).toBeInTheDocument();
      expect(screen.getByText('BAR CHART')).toBeInTheDocument();
      expect(screen.getByText('PIE CHART')).toBeInTheDocument();
      expect(screen.getByText('AREA CHART')).toBeInTheDocument();
      expect(screen.getByText('TABLE')).toBeInTheDocument();
    });
  });

  it('should display all variables in dropdown', async () => {
    render(<EditWidgetModal {...defaultProps} />);

    await waitFor(() => {
      // Check that "Variable *" label exists
      expect(screen.getByText('Variable *')).toBeInTheDocument();

      // Check for the selected variable
      expect(screen.getByDisplayValue('kwConsumption')).toBeInTheDocument();

      // All variables should be rendered as options in the select
      const selectElement = screen.getByDisplayValue('kwConsumption') as HTMLSelectElement;
      const optionValues = Array.from(selectElement.options).map(opt => opt.value);

      expect(optionValues).toContain('kwConsumption');
      expect(optionValues).toContain('voltage');
      expect(optionValues).toContain('current');
      expect(optionValues).toContain('powerFactor');
      expect(optionValues).toContain('frequency');
    });
  });
});
