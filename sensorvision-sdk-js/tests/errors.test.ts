/**
 * Unit tests for error classes
 */

import {
  SensorVisionError,
  AuthenticationError,
  ValidationError,
  NetworkError,
  ServerError,
  RateLimitError,
} from '../src/errors';

describe('Error Classes', () => {
  describe('SensorVisionError', () => {
    it('should create error with message', () => {
      const error = new SensorVisionError('Test error');
      expect(error.message).toBe('Test error');
      expect(error.name).toBe('SensorVisionError');
      expect(error instanceof Error).toBe(true);
    });
  });

  describe('AuthenticationError', () => {
    it('should create authentication error', () => {
      const error = new AuthenticationError('Invalid API key');
      expect(error.message).toBe('Invalid API key');
      expect(error.name).toBe('AuthenticationError');
      expect(error instanceof SensorVisionError).toBe(true);
    });
  });

  describe('ValidationError', () => {
    it('should create validation error', () => {
      const error = new ValidationError('Invalid data format');
      expect(error.message).toBe('Invalid data format');
      expect(error.name).toBe('ValidationError');
      expect(error instanceof SensorVisionError).toBe(true);
    });
  });

  describe('NetworkError', () => {
    it('should create network error', () => {
      const error = new NetworkError('Connection failed');
      expect(error.message).toBe('Connection failed');
      expect(error.name).toBe('NetworkError');
      expect(error instanceof SensorVisionError).toBe(true);
    });
  });

  describe('ServerError', () => {
    it('should create server error', () => {
      const error = new ServerError('Internal server error');
      expect(error.message).toBe('Internal server error');
      expect(error.name).toBe('ServerError');
      expect(error instanceof SensorVisionError).toBe(true);
    });
  });

  describe('RateLimitError', () => {
    it('should create rate limit error', () => {
      const error = new RateLimitError('Rate limit exceeded');
      expect(error.message).toBe('Rate limit exceeded');
      expect(error.name).toBe('RateLimitError');
      expect(error instanceof SensorVisionError).toBe(true);
    });
  });
});
