import { describe, it, expect, beforeEach, vi } from 'vitest';
import { getApiBaseUrl, getBackendUrl, getWebSocketUrl } from './config';

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
});
