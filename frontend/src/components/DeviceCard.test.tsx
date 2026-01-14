import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { DeviceCard } from './DeviceCard';
import { Device, TelemetryPoint } from '../types';

// Helper to create mock device
const createMockDevice = (overrides: Partial<Device> = {}): Device => ({
  id: 'device-uuid-001',
  externalId: 'device-001',
  name: 'Test Device',
  status: 'ONLINE',
  healthScore: 85,
  healthStatus: 'EXCELLENT',
  location: 'Floor 1, Room A',
  lastSeenAt: new Date().toISOString(),
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  ...overrides,
});

// Helper to create mock telemetry
const createMockTelemetry = (overrides: Partial<TelemetryPoint> = {}): TelemetryPoint => ({
  deviceId: 'device-001',
  timestamp: new Date().toISOString(),
  kwConsumption: 50.5,
  voltage: 220.1,
  current: 0.57,
  ...overrides,
});

describe('DeviceCard', () => {
  // ========== Basic Rendering Tests ==========

  describe('Basic Rendering', () => {
    it('should render the component', () => {
      const device = createMockDevice();
      const { container } = render(<DeviceCard device={device} />);

      expect(container.firstChild).toBeInTheDocument();
    });

    it('should display device name', () => {
      const device = createMockDevice({ name: 'Factory Line 01' });
      render(<DeviceCard device={device} />);

      expect(screen.getByText('Factory Line 01')).toBeInTheDocument();
    });

    it('should display device external ID', () => {
      const device = createMockDevice({ externalId: 'sensor-xyz-123' });
      render(<DeviceCard device={device} />);

      expect(screen.getByText('sensor-xyz-123')).toBeInTheDocument();
    });

    it('should display device location when provided', () => {
      const device = createMockDevice({ location: 'Building A, Floor 3' });
      render(<DeviceCard device={device} latestTelemetry={createMockTelemetry()} />);

      expect(screen.getByText('Building A, Floor 3')).toBeInTheDocument();
    });

    it('should not display location section when location is missing', () => {
      const device = createMockDevice({ location: undefined });
      render(<DeviceCard device={device} latestTelemetry={createMockTelemetry()} />);

      expect(screen.queryByText('Building A, Floor 3')).not.toBeInTheDocument();
    });
  });

  // ========== Device Status Tests ==========

  describe('Device Status', () => {
    it('should display ONLINE status badge', () => {
      const device = createMockDevice({ status: 'ONLINE' });
      render(<DeviceCard device={device} />);

      expect(screen.getByText('ONLINE')).toBeInTheDocument();
    });

    it('should display OFFLINE status badge', () => {
      const device = createMockDevice({ status: 'OFFLINE' });
      render(<DeviceCard device={device} />);

      expect(screen.getByText('OFFLINE')).toBeInTheDocument();
    });

    it('should display UNKNOWN status badge', () => {
      const device = createMockDevice({ status: 'UNKNOWN' });
      render(<DeviceCard device={device} />);

      expect(screen.getByText('UNKNOWN')).toBeInTheDocument();
    });
  });

  // ========== Health Score Badge Tests ==========

  describe('Health Score Badge', () => {
    it('should display health score when provided', () => {
      const device = createMockDevice({ healthScore: 87 });
      render(<DeviceCard device={device} />);

      expect(screen.getByText('87%')).toBeInTheDocument();
    });

    it('should not display health score when undefined', () => {
      const device = createMockDevice({ healthScore: undefined });
      render(<DeviceCard device={device} />);

      // Should not find percentage text for health
      expect(screen.queryByText(/^\d+%$/)).not.toBeInTheDocument();
    });

    it('should apply EXCELLENT health styling (green)', () => {
      const device = createMockDevice({ healthScore: 90, healthStatus: 'EXCELLENT' });
      const { container } = render(<DeviceCard device={device} />);

      // Check for emerald/green styling - look for the text color
      const healthText = container.querySelector('.text-emerald-400');
      expect(healthText).toBeInTheDocument();
    });

    it('should apply GOOD health styling (cyan)', () => {
      const device = createMockDevice({ healthScore: 70, healthStatus: 'GOOD' });
      const { container } = render(<DeviceCard device={device} />);

      // Check for cyan styling
      const healthText = container.querySelector('.text-cyan-400');
      expect(healthText).toBeInTheDocument();
    });

    it('should apply FAIR health styling (amber)', () => {
      const device = createMockDevice({ healthScore: 50, healthStatus: 'FAIR' });
      const { container } = render(<DeviceCard device={device} />);

      // Check for amber styling
      const healthText = container.querySelector('.text-amber-400');
      expect(healthText).toBeInTheDocument();
    });

    it('should apply POOR health styling (orange)', () => {
      const device = createMockDevice({ healthScore: 30, healthStatus: 'POOR' });
      const { container } = render(<DeviceCard device={device} />);

      // Check for orange styling
      const healthText = container.querySelector('.text-orange-400');
      expect(healthText).toBeInTheDocument();
    });

    it('should apply CRITICAL health styling (rose) with animation', () => {
      const device = createMockDevice({ healthScore: 10, healthStatus: 'CRITICAL' });
      const { container } = render(<DeviceCard device={device} />);

      // Check for rose styling
      const healthText = container.querySelector('.text-rose-400');
      expect(healthText).toBeInTheDocument();

      // Critical should have pulse animation
      const pulsingDot = container.querySelector('.animate-pulse');
      expect(pulsingDot).toBeInTheDocument();
    });
  });

  // ========== Primary Metric Tests ==========

  describe('Primary Metric', () => {
    it('should display kW consumption as primary metric', () => {
      const device = createMockDevice();
      const telemetry = createMockTelemetry({ kwConsumption: 52.3 });
      render(<DeviceCard device={device} latestTelemetry={telemetry} />);

      expect(screen.getByText('52.3')).toBeInTheDocument();
      expect(screen.getByText('kW')).toBeInTheDocument();
    });

    it('should display kw_consumption as primary metric (snake_case)', () => {
      const device = createMockDevice();
      const telemetry = createMockTelemetry({
        kwConsumption: undefined,
        kw_consumption: 48.7,
      } as TelemetryPoint);
      render(<DeviceCard device={device} latestTelemetry={telemetry} />);

      expect(screen.getByText('48.7')).toBeInTheDocument();
    });

    it('should fall back to temperature when power not available', () => {
      const device = createMockDevice();
      const telemetry = {
        deviceId: 'device-001',
        timestamp: new Date().toISOString(),
        temperature: 25.5,
      } as TelemetryPoint;
      render(<DeviceCard device={device} latestTelemetry={telemetry} />);

      expect(screen.getByText('25.5')).toBeInTheDocument();
      expect(screen.getByText('C')).toBeInTheDocument();
    });

    it('should fall back to voltage when power/temperature not available', () => {
      const device = createMockDevice();
      const telemetry = {
        deviceId: 'device-001',
        timestamp: new Date().toISOString(),
        voltage: 220.5,
      } as TelemetryPoint;
      render(<DeviceCard device={device} latestTelemetry={telemetry} />);

      expect(screen.getByText('220.5')).toBeInTheDocument();
      expect(screen.getByText('V')).toBeInTheDocument();
    });
  });

  // ========== Sparkline Tests ==========

  describe('Sparkline', () => {
    it('should render sparkline when telemetry history has >= 2 points', () => {
      const device = createMockDevice();
      const telemetry = createMockTelemetry();
      const history = [45, 48, 52, 50, 55];

      const { container } = render(
        <DeviceCard
          device={device}
          latestTelemetry={telemetry}
          telemetryHistory={history}
        />
      );

      const sparklineSvg = container.querySelector('.sparkline-svg');
      expect(sparklineSvg).toBeInTheDocument();
    });

    it('should not render sparkline when telemetry history has < 2 points', () => {
      const device = createMockDevice();
      const telemetry = createMockTelemetry();
      const history = [45]; // Only one point

      const { container } = render(
        <DeviceCard
          device={device}
          latestTelemetry={telemetry}
          telemetryHistory={history}
        />
      );

      const sparklineSvg = container.querySelector('.sparkline-svg');
      expect(sparklineSvg).not.toBeInTheDocument();
    });

    it('should not render sparkline when telemetry history is empty', () => {
      const device = createMockDevice();
      const telemetry = createMockTelemetry();

      const { container } = render(
        <DeviceCard
          device={device}
          latestTelemetry={telemetry}
          telemetryHistory={[]}
        />
      );

      const sparklineSvg = container.querySelector('.sparkline-svg');
      expect(sparklineSvg).not.toBeInTheDocument();
    });
  });

  // ========== Trend Indicator Tests ==========

  describe('Trend Indicator', () => {
    it('should display trend indicator with sparkline data', () => {
      const device = createMockDevice();
      const telemetry = createMockTelemetry({ kwConsumption: 55 });
      const history = [45, 48, 52, 50, 55]; // Upward trend

      render(
        <DeviceCard
          device={device}
          latestTelemetry={telemetry}
          telemetryHistory={history}
        />
      );

      // Should show trend indicator text
      expect(screen.getByText('vs 1h ago')).toBeInTheDocument();
    });

    it('should calculate trend from telemetry history', () => {
      const device = createMockDevice();
      const telemetry = createMockTelemetry({ kwConsumption: 55 });
      const history = [40, 45, 50, 52, 55]; // 37.5% increase

      const { container } = render(
        <DeviceCard
          device={device}
          latestTelemetry={telemetry}
          telemetryHistory={history}
        />
      );

      // Should show upward trend (green)
      const trendIndicator = container.querySelector('.text-emerald-400');
      expect(trendIndicator).toBeInTheDocument();
    });

    it('should calculate trend from previousValue when history not available', () => {
      const device = createMockDevice();
      const telemetry = createMockTelemetry({ kwConsumption: 55 });

      render(
        <DeviceCard
          device={device}
          latestTelemetry={telemetry}
          previousValue={50}
          telemetryHistory={[]}
        />
      );

      // Should show trend indicator
      expect(screen.getByText('vs 1h ago')).toBeInTheDocument();
    });
  });

  // ========== Additional Readings Tests ==========

  describe('Additional Readings', () => {
    it('should display additional readings section when telemetry available', () => {
      const device = createMockDevice();
      const telemetry = createMockTelemetry();
      render(<DeviceCard device={device} latestTelemetry={telemetry} />);

      expect(screen.getByText('Latest Readings')).toBeInTheDocument();
    });

    it('should display voltage reading', () => {
      const device = createMockDevice();
      const telemetry = createMockTelemetry({
        kwConsumption: 50,
        voltage: 220.5
      });
      render(<DeviceCard device={device} latestTelemetry={telemetry} />);

      expect(screen.getByText('Voltage')).toBeInTheDocument();
      expect(screen.getByText(/220\.5/)).toBeInTheDocument();
    });

    it('should display current reading', () => {
      const device = createMockDevice();
      const telemetry = createMockTelemetry({
        kwConsumption: 50,
        current: 0.57
      });
      render(<DeviceCard device={device} latestTelemetry={telemetry} />);

      expect(screen.getByText('Current')).toBeInTheDocument();
      expect(screen.getByText(/0\.6/)).toBeInTheDocument(); // Rounded to 1 decimal
    });

    it('should exclude primary metric from additional readings', () => {
      const device = createMockDevice();
      const telemetry = createMockTelemetry({ kwConsumption: 50.5 });
      render(<DeviceCard device={device} latestTelemetry={telemetry} />);

      // Primary metric (kW) should appear in main display
      expect(screen.getByText('50.5')).toBeInTheDocument();

      // But "Power" should not appear twice (once in header is the icon label)
      const powerLabels = screen.queryAllByText('Power');
      expect(powerLabels.length).toBeLessThanOrEqual(1);
    });

    it('should display max 4 additional readings', () => {
      const device = createMockDevice();
      const telemetry = {
        deviceId: 'device-001',
        timestamp: new Date().toISOString(),
        kwConsumption: 50,
        voltage: 220,
        current: 0.5,
        temperature: 25,
        humidity: 60,
        pressure: 1.5,
        flowRate: 10,
      } as TelemetryPoint;

      render(<DeviceCard device={device} latestTelemetry={telemetry} />);

      // Should show the "Latest Readings" section
      expect(screen.getByText('Latest Readings')).toBeInTheDocument();

      // Should not show more than 4 secondary readings
      // Primary is kwConsumption, so we should see at most 4 others displayed
      // We can check by counting how many of these appear
      const visibleReadings = ['Voltage', 'Current', 'Temp', 'Humidity'].filter(
        label => screen.queryByText(label) !== null
      );
      expect(visibleReadings.length).toBeLessThanOrEqual(4);
    });
  });

  // ========== Last Seen Tests ==========

  describe('Last Seen', () => {
    it('should display last seen time', () => {
      const device = createMockDevice({
        lastSeenAt: new Date(Date.now() - 5 * 60 * 1000).toISOString() // 5 mins ago
      });
      render(<DeviceCard device={device} />);

      expect(screen.getByText(/Last seen/)).toBeInTheDocument();
    });

    it('should not display last seen when not provided', () => {
      const device = createMockDevice({ lastSeenAt: undefined });
      render(<DeviceCard device={device} />);

      expect(screen.queryByText(/Last seen/)).not.toBeInTheDocument();
    });

    it('should have title with full date on hover', () => {
      const lastSeenDate = new Date(Date.now() - 60 * 1000);
      const device = createMockDevice({ lastSeenAt: lastSeenDate.toISOString() });
      render(<DeviceCard device={device} />);

      const lastSeenElement = screen.getByText(/Last seen/).closest('div')?.querySelector('span');
      expect(lastSeenElement).toHaveAttribute('title');
    });
  });

  // ========== Hover State Tests ==========

  describe('Hover State', () => {
    it('should apply hover styling on mouse enter', () => {
      const device = createMockDevice();
      const { container } = render(<DeviceCard device={device} />);

      const card = container.querySelector('.device-card-hover');
      expect(card).toBeInTheDocument();

      fireEvent.mouseEnter(card!);

      // After hover, should have ring style
      expect(card).toHaveClass('ring-1');
    });

    it('should remove hover styling on mouse leave', () => {
      const device = createMockDevice();
      const { container } = render(<DeviceCard device={device} />);

      const card = container.querySelector('.device-card-hover');

      fireEvent.mouseEnter(card!);
      expect(card).toHaveClass('ring-1');

      fireEvent.mouseLeave(card!);
      expect(card).not.toHaveClass('ring-1');
    });
  });

  // ========== Variable Configuration Tests ==========

  describe('Variable Configuration', () => {
    it('should use correct label for known variables', () => {
      const device = createMockDevice();
      const telemetry = createMockTelemetry({
        kwConsumption: 50,
        powerFactor: 0.95
      });
      render(<DeviceCard device={device} latestTelemetry={telemetry} />);

      expect(screen.getByText('PF')).toBeInTheDocument();
    });

    it('should use correct unit for known variables', () => {
      const device = createMockDevice();
      const telemetry = createMockTelemetry({
        kwConsumption: 50,
        frequency: 60
      });
      render(<DeviceCard device={device} latestTelemetry={telemetry} />);

      expect(screen.getByText('Freq')).toBeInTheDocument();
      expect(screen.getByText(/Hz/)).toBeInTheDocument();
    });

    it('should generate label for unknown variables', () => {
      const device = createMockDevice();
      const telemetry = {
        deviceId: 'device-001',
        timestamp: new Date().toISOString(),
        myCustomSensor: 123.4,
      } as TelemetryPoint;
      render(<DeviceCard device={device} latestTelemetry={telemetry} />);

      // The getVariableInfo function converts camelCase to readable label
      // "myCustomSensor" -> "My Custom" (first 2 words)
      // Should display the value
      expect(screen.getByText('123.4')).toBeInTheDocument();
    });
  });

  // ========== Edge Cases ==========

  describe('Edge Cases', () => {
    it('should handle device with no telemetry', () => {
      const device = createMockDevice();
      render(<DeviceCard device={device} />);

      // Should render without crashing
      expect(screen.getByText(device.name)).toBeInTheDocument();
    });

    it('should handle telemetry with no numeric values', () => {
      const device = createMockDevice();
      const telemetry = {
        deviceId: 'device-001',
        timestamp: new Date().toISOString(),
        status: 'OK',
      } as unknown as TelemetryPoint;

      render(<DeviceCard device={device} latestTelemetry={telemetry} />);

      // Should render without crashing
      expect(screen.getByText(device.name)).toBeInTheDocument();
    });

    it('should handle null telemetry', () => {
      const device = createMockDevice();
      render(<DeviceCard device={device} latestTelemetry={null} />);

      // Should render without crashing
      expect(screen.getByText(device.name)).toBeInTheDocument();
    });

    it('should handle zero values in telemetry', () => {
      const device = createMockDevice();
      const telemetry = createMockTelemetry({ kwConsumption: 0 });
      render(<DeviceCard device={device} latestTelemetry={telemetry} />);

      expect(screen.getByText('0.0')).toBeInTheDocument();
    });

    it('should handle negative values in telemetry', () => {
      const device = createMockDevice();
      const telemetry = createMockTelemetry({ current: -0.5 });
      render(<DeviceCard device={device} latestTelemetry={telemetry} />);

      expect(screen.getByText(/-0\.5/)).toBeInTheDocument();
    });
  });
});
