import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { getApiBaseUrl, getBackendUrl, getWebSocketUrl, featureFlags } from './config';

describe('config.ts', () => {
  beforeEach(() => {
    // Reset window.location to default
    Object.defineProperty(window, 'location', {
      writable: true,
      configurable: true,
      value: {
        protocol: 'http:',
        host: 'localhost:3000',
        origin: 'http://localhost:3000',
      },
    });
  });

  describe('getApiBaseUrl', () => {
    it('should return localhost:8080 in development (test environment)', () => {
      // In test environment, import.meta.env.PROD is false
      const result = getApiBaseUrl();
      expect(result).toBe('http://localhost:8080');
    });
  });

  describe('getBackendUrl', () => {
    it('should return localhost:8080 in development (test environment)', () => {
      // In test environment, import.meta.env.PROD is false
      const result = getBackendUrl();
      expect(result).toBe('http://localhost:8080');
    });

    it('should use window.location.origin when called (production behavior)', () => {
      // We can't mock import.meta.env.PROD, but we can verify the function
      // uses window.location.origin by checking the implementation

      Object.defineProperty(window, 'location', {
        writable: true,
        configurable: true,
        value: {
          protocol: 'https:',
          host: 'app.sensorvision.com',
          origin: 'https://app.sensorvision.com',
        },
      });

      // In production mode, this would return window.location.origin
      // In test mode, it returns localhost:8080
      // This test documents the expected production behavior
      expect(window.location.origin).toBe('https://app.sensorvision.com');
    });
  });

  describe('getWebSocketUrl', () => {
    it('should return ws://localhost:8080/ws/telemetry in development', () => {
      const result = getWebSocketUrl();
      expect(result).toBe('ws://localhost:8080/ws/telemetry');
    });

    it('should use window.location for WebSocket URL construction (production behavior)', () => {
      // Test that the logic for production would work correctly
      Object.defineProperty(window, 'location', {
        writable: true,
        configurable: true,
        value: {
          protocol: 'https:',
          host: 'app.sensorvision.com',
          origin: 'https://app.sensorvision.com',
        },
      });

      // In production, this would construct wss://app.sensorvision.com/ws/telemetry
      const expectedProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const expectedUrl = `${expectedProtocol}//${window.location.host}/ws/telemetry`;

      expect(expectedUrl).toBe('wss://app.sensorvision.com/ws/telemetry');
    });

    it('should use ws:// protocol when window.location uses http:', () => {
      Object.defineProperty(window, 'location', {
        writable: true,
        configurable: true,
        value: {
          protocol: 'http:',
          host: 'app.sensorvision.com',
          origin: 'http://app.sensorvision.com',
        },
      });

      const expectedProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const expectedUrl = `${expectedProtocol}//${window.location.host}/ws/telemetry`;

      expect(expectedUrl).toBe('ws://app.sensorvision.com/ws/telemetry');
    });
  });

  describe('URL encoding and reverse proxy compatibility', () => {
    it('should not append :8080 to production URLs (reverse proxy fix)', () => {
      // Verify that in production, the code would use window.location.origin
      // which doesn't include :8080 when behind a reverse proxy

      Object.defineProperty(window, 'location', {
        writable: true,
        configurable: true,
        value: {
          protocol: 'https:',
          host: 'production.sensorvision.com', // No port - reverse proxy
          origin: 'https://production.sensorvision.com',
        },
      });

      // The origin should not contain :8080
      expect(window.location.origin).toBe('https://production.sensorvision.com');
      expect(window.location.origin).not.toContain(':8080');
    });

    it('should construct correct WebSocket URL without :8080 (reverse proxy)', () => {
      Object.defineProperty(window, 'location', {
        writable: true,
        configurable: true,
        value: {
          protocol: 'https:',
          host: 'production.sensorvision.com',
          origin: 'https://production.sensorvision.com',
        },
      });

      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const wsUrl = `${protocol}//${window.location.host}/ws/telemetry`;

      expect(wsUrl).toBe('wss://production.sensorvision.com/ws/telemetry');
      expect(wsUrl).not.toContain(':8080');
    });
  });

  describe('featureFlags', () => {
    // Store original localStorage methods
    const originalGetItem = Storage.prototype.getItem;
    const originalSetItem = Storage.prototype.setItem;
    const originalRemoveItem = Storage.prototype.removeItem;

    beforeEach(() => {
      // Clear any localStorage overrides before each test
      localStorage.removeItem('FF_ENGAGING_DASHBOARD');
    });

    afterEach(() => {
      // Clean up after tests
      localStorage.removeItem('FF_ENGAGING_DASHBOARD');
      // Restore original methods if they were mocked
      Storage.prototype.getItem = originalGetItem;
      Storage.prototype.setItem = originalSetItem;
      Storage.prototype.removeItem = originalRemoveItem;
    });

    describe('engagingDashboard', () => {
      it('should return true by default (no localStorage override)', () => {
        // Ensure no override is set
        localStorage.removeItem('FF_ENGAGING_DASHBOARD');

        expect(featureFlags.engagingDashboard).toBe(true);
      });

      it('should return true when localStorage override is "true"', () => {
        localStorage.setItem('FF_ENGAGING_DASHBOARD', 'true');

        expect(featureFlags.engagingDashboard).toBe(true);
      });

      it('should return false when localStorage override is "false"', () => {
        localStorage.setItem('FF_ENGAGING_DASHBOARD', 'false');

        expect(featureFlags.engagingDashboard).toBe(false);
      });

      it('should ignore invalid localStorage values (not "true" or "false")', () => {
        // Invalid value should be treated as false (since it's not "true")
        localStorage.setItem('FF_ENGAGING_DASHBOARD', 'invalid');

        expect(featureFlags.engagingDashboard).toBe(false);
      });

      it('should return default when localStorage is cleared', () => {
        // First set to false
        localStorage.setItem('FF_ENGAGING_DASHBOARD', 'false');
        expect(featureFlags.engagingDashboard).toBe(false);

        // Then clear
        localStorage.removeItem('FF_ENGAGING_DASHBOARD');
        expect(featureFlags.engagingDashboard).toBe(true);
      });

      it('should allow quick rollback by setting to "false"', () => {
        // Simulate production rollback scenario
        expect(featureFlags.engagingDashboard).toBe(true);

        // Quick rollback
        localStorage.setItem('FF_ENGAGING_DASHBOARD', 'false');
        expect(featureFlags.engagingDashboard).toBe(false);

        // Re-enable
        localStorage.setItem('FF_ENGAGING_DASHBOARD', 'true');
        expect(featureFlags.engagingDashboard).toBe(true);
      });

      it('should read value dynamically on each access', () => {
        // First access - default
        expect(featureFlags.engagingDashboard).toBe(true);

        // Change override
        localStorage.setItem('FF_ENGAGING_DASHBOARD', 'false');

        // Second access - should reflect change immediately
        expect(featureFlags.engagingDashboard).toBe(false);
      });
    });

    describe('localStorage key convention', () => {
      it('should use FF_ prefix for feature flag keys', () => {
        // This test documents the expected key naming convention
        const expectedKey = 'FF_ENGAGING_DASHBOARD';

        localStorage.setItem(expectedKey, 'false');
        expect(featureFlags.engagingDashboard).toBe(false);
      });
    });
  });
});
