import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { FleetHealthGauge } from './FleetHealthGauge';
import { Device } from '../types';

// Helper to create mock devices
const createMockDevice = (overrides: Partial<Device> = {}): Device => ({
  id: 'device-uuid-001',
  externalId: 'device-001',
  name: 'Test Device',
  status: 'ONLINE',
  healthScore: 85,
  healthStatus: 'EXCELLENT',
  ...overrides,
});

describe('FleetHealthGauge', () => {
  // ========== Basic Rendering Tests ==========

  describe('Basic Rendering', () => {
    it('should render the component', () => {
      const devices = [createMockDevice()];
      const { container } = render(<FleetHealthGauge devices={devices} />);

      expect(container.firstChild).toBeInTheDocument();
    });

    it('should render the Fleet Health header', () => {
      const devices = [createMockDevice()];
      render(<FleetHealthGauge devices={devices} />);

      expect(screen.getByText('Fleet Health')).toBeInTheDocument();
    });

    it('should render an SVG gauge', () => {
      const devices = [createMockDevice()];
      render(<FleetHealthGauge devices={devices} />);

      const svg = screen.getByRole('img');
      expect(svg).toBeInTheDocument();
      expect(svg).toHaveAttribute('aria-label', expect.stringContaining('Fleet health:'));
    });

    it('should apply custom className', () => {
      const devices = [createMockDevice()];
      const { container } = render(
        <FleetHealthGauge devices={devices} className="custom-class" />
      );

      expect(container.firstChild).toHaveClass('custom-class');
    });
  });

  // ========== Health Score Calculation Tests ==========

  describe('Health Score Calculation', () => {
    it('should calculate average health score from multiple devices', () => {
      const devices = [
        createMockDevice({ healthScore: 80 }),
        createMockDevice({ healthScore: 90 }),
        createMockDevice({ healthScore: 70 }),
      ];
      render(<FleetHealthGauge devices={devices} />);

      // Average: (80 + 90 + 70) / 3 = 80
      expect(screen.getByText('80%')).toBeInTheDocument();
    });

    it('should display single device health score', () => {
      const devices = [createMockDevice({ healthScore: 75 })];
      render(<FleetHealthGauge devices={devices} />);

      expect(screen.getByText('75%')).toBeInTheDocument();
    });

    it('should round health score to nearest integer', () => {
      const devices = [
        createMockDevice({ healthScore: 75 }),
        createMockDevice({ healthScore: 76 }),
      ];
      render(<FleetHealthGauge devices={devices} />);

      // Average: 75.5, should round to 76
      expect(screen.getByText('76%')).toBeInTheDocument();
    });

    it('should ignore devices without health score in average calculation', () => {
      const devices = [
        createMockDevice({ healthScore: 80 }),
        createMockDevice({ healthScore: undefined }),
        createMockDevice({ healthScore: 60 }),
      ];
      render(<FleetHealthGauge devices={devices} />);

      // Average of devices with health: (80 + 60) / 2 = 70
      expect(screen.getByText('70%')).toBeInTheDocument();
    });
  });

  // ========== Health Status Labels Tests ==========

  describe('Health Status Labels', () => {
    it('should display "Excellent" for health >= 80', () => {
      const devices = [createMockDevice({ healthScore: 85 })];
      render(<FleetHealthGauge devices={devices} />);

      expect(screen.getByText('Excellent')).toBeInTheDocument();
    });

    it('should display "Good" for health 60-79', () => {
      const devices = [createMockDevice({ healthScore: 70 })];
      render(<FleetHealthGauge devices={devices} />);

      expect(screen.getByText('Good')).toBeInTheDocument();
    });

    it('should display "Fair" for health 40-59', () => {
      const devices = [createMockDevice({ healthScore: 50 })];
      render(<FleetHealthGauge devices={devices} />);

      expect(screen.getByText('Fair')).toBeInTheDocument();
    });

    it('should display "Poor" for health 20-39', () => {
      const devices = [createMockDevice({ healthScore: 30 })];
      render(<FleetHealthGauge devices={devices} />);

      expect(screen.getByText('Poor')).toBeInTheDocument();
    });

    it('should display "Critical" for health < 20', () => {
      const devices = [createMockDevice({ healthScore: 10 })];
      render(<FleetHealthGauge devices={devices} />);

      expect(screen.getByText('Critical')).toBeInTheDocument();
    });
  });

  // ========== Mini Stats Tests ==========

  describe('Mini Stats', () => {
    it('should display total device count', () => {
      const devices = [
        createMockDevice(),
        createMockDevice({ id: 'device-uuid-002', externalId: 'device-002' }),
        createMockDevice({ id: 'device-uuid-003', externalId: 'device-003' }),
      ];
      render(<FleetHealthGauge devices={devices} />);

      // Check the Devices section has the count
      expect(screen.getByText('Devices')).toBeInTheDocument();
      const devicesSection = screen.getByText('Devices').parentElement;
      expect(devicesSection?.textContent).toContain('3');
    });

    it('should display online device count', () => {
      const devices = [
        createMockDevice({ status: 'ONLINE' }),
        createMockDevice({ id: 'device-uuid-002', externalId: 'device-002', status: 'ONLINE' }),
        createMockDevice({ id: 'device-uuid-003', externalId: 'device-003', status: 'OFFLINE' }),
      ];
      render(<FleetHealthGauge devices={devices} />);

      // 2 online devices
      expect(screen.getByText('2')).toBeInTheDocument();
      expect(screen.getByText('Online')).toBeInTheDocument();
    });

    it('should display correct uptime percentage', () => {
      const devices = [
        createMockDevice({ status: 'ONLINE' }),
        createMockDevice({ id: 'device-uuid-002', externalId: 'device-002', status: 'ONLINE' }),
        createMockDevice({ id: 'device-uuid-003', externalId: 'device-003', status: 'OFFLINE' }),
        createMockDevice({ id: 'device-uuid-004', externalId: 'device-004', status: 'OFFLINE' }),
      ];
      render(<FleetHealthGauge devices={devices} />);

      // 2/4 = 50% uptime
      expect(screen.getByText('50%')).toBeInTheDocument();
      expect(screen.getByText('Uptime')).toBeInTheDocument();
    });

    it('should display 100% uptime when all devices online', () => {
      const devices = [
        createMockDevice({ status: 'ONLINE', healthScore: 50 }), // Use different health so we can identify uptime
        createMockDevice({ id: 'device-uuid-002', externalId: 'device-002', status: 'ONLINE', healthScore: 50 }),
      ];
      render(<FleetHealthGauge devices={devices} />);

      // Should have 100% uptime (both online)
      // Health is 50%, uptime is 100%
      expect(screen.getByText('Uptime')).toBeInTheDocument();
      // Check that we have the 100% for uptime near the Uptime label
      const uptimeSection = screen.getByText('Uptime').parentElement;
      expect(uptimeSection?.textContent).toContain('100%');
    });
  });

  // ========== Empty State Tests ==========

  describe('Empty State', () => {
    it('should display 0% health for empty device array', () => {
      render(<FleetHealthGauge devices={[]} />);

      // The health value appears in the center of the gauge
      // There will be multiple 0% (health and uptime), check at least one exists
      const zeroPercentElements = screen.getAllByText('0%');
      expect(zeroPercentElements.length).toBeGreaterThanOrEqual(1);
    });

    it('should display "Critical" status for empty device array', () => {
      render(<FleetHealthGauge devices={[]} />);

      expect(screen.getByText('Critical')).toBeInTheDocument();
    });

    it('should display 0 devices for empty array', () => {
      render(<FleetHealthGauge devices={[]} />);

      // Check the Devices section has 0
      expect(screen.getByText('Devices')).toBeInTheDocument();
      const devicesSection = screen.getByText('Devices').parentElement;
      expect(devicesSection?.textContent).toContain('0');
    });

    it('should display 0% uptime for empty array', () => {
      render(<FleetHealthGauge devices={[]} />);

      // Check the Uptime section
      expect(screen.getByText('Uptime')).toBeInTheDocument();
      const uptimeSection = screen.getByText('Uptime').parentElement;
      expect(uptimeSection?.textContent).toContain('0%');
    });
  });

  // ========== SVG Gauge Tests ==========

  describe('SVG Gauge', () => {
    it('should have accessible aria-label with health percentage', () => {
      const devices = [createMockDevice({ healthScore: 75 })];
      render(<FleetHealthGauge devices={devices} />);

      const svg = screen.getByRole('img');
      expect(svg).toHaveAttribute('aria-label', 'Fleet health: 75%');
    });

    it('should render gradient definition', () => {
      const devices = [createMockDevice()];
      const { container } = render(<FleetHealthGauge devices={devices} />);

      const gradient = container.querySelector('#fleetHealthGradient');
      expect(gradient).toBeInTheDocument();
    });

    it('should render background circle', () => {
      const devices = [createMockDevice()];
      const { container } = render(<FleetHealthGauge devices={devices} />);

      const circles = container.querySelectorAll('circle');
      expect(circles.length).toBe(2); // Background and value circles
    });
  });

  // ========== Edge Cases ==========

  describe('Edge Cases', () => {
    it('should handle devices with all health scores at 0', () => {
      const devices = [
        createMockDevice({ healthScore: 0 }),
        createMockDevice({ id: 'device-uuid-002', externalId: 'device-002', healthScore: 0 }),
      ];
      render(<FleetHealthGauge devices={devices} />);

      expect(screen.getAllByText('0%').length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('Critical')).toBeInTheDocument();
    });

    it('should handle devices with all health scores at 100', () => {
      const devices = [
        createMockDevice({ healthScore: 100, status: 'ONLINE' }),
        createMockDevice({ id: 'device-uuid-002', externalId: 'device-002', healthScore: 100, status: 'ONLINE' }),
      ];
      render(<FleetHealthGauge devices={devices} />);

      // There will be two "100%" (health and uptime), check both are present
      const allHundredPercent = screen.getAllByText('100%');
      expect(allHundredPercent.length).toBeGreaterThanOrEqual(1);
      expect(screen.getByText('Excellent')).toBeInTheDocument();
    });

    it('should handle mix of ONLINE and OFFLINE devices', () => {
      const devices = [
        createMockDevice({ status: 'ONLINE', healthScore: 90 }),
        createMockDevice({ id: 'device-uuid-002', externalId: 'device-002', status: 'OFFLINE', healthScore: 30 }),
      ];
      render(<FleetHealthGauge devices={devices} />);

      // Average health: 60
      expect(screen.getByText('60%')).toBeInTheDocument();
    });

    it('should handle UNKNOWN status devices', () => {
      const devices = [
        createMockDevice({ status: 'UNKNOWN', healthScore: 50 }),
      ];
      render(<FleetHealthGauge devices={devices} />);

      // UNKNOWN is not ONLINE, so online count should be 0
      expect(screen.getByText('0')).toBeInTheDocument();
    });
  });
});
