/**
 * Custom error classes for IndCloud SDK
 */

/**
 * Base error class for all IndCloud SDK errors
 */
export class IndCloudError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'IndCloudError';
    Object.setPrototypeOf(this, IndCloudError.prototype);
  }
}

/**
 * Authentication error - invalid API key
 */
export class AuthenticationError extends IndCloudError {
  constructor(message: string) {
    super(message);
    this.name = 'AuthenticationError';
    Object.setPrototypeOf(this, AuthenticationError.prototype);
  }
}

/**
 * Validation error - invalid data format
 */
export class ValidationError extends IndCloudError {
  constructor(message: string) {
    super(message);
    this.name = 'ValidationError';
    Object.setPrototypeOf(this, ValidationError.prototype);
  }
}

/**
 * Network error - connection/request failed
 */
export class NetworkError extends IndCloudError {
  constructor(message: string) {
    super(message);
    this.name = 'NetworkError';
    Object.setPrototypeOf(this, NetworkError.prototype);
  }
}

/**
 * Server error - 5xx response
 */
export class ServerError extends IndCloudError {
  constructor(message: string) {
    super(message);
    this.name = 'ServerError';
    Object.setPrototypeOf(this, ServerError.prototype);
  }
}

/**
 * Rate limit error - too many requests
 */
export class RateLimitError extends IndCloudError {
  constructor(message: string) {
    super(message);
    this.name = 'RateLimitError';
    Object.setPrototypeOf(this, RateLimitError.prototype);
  }
}

/**
 * WebSocket error - connection/subscription failed
 */
export class WebSocketError extends IndCloudError {
  constructor(message: string) {
    super(message);
    this.name = 'WebSocketError';
    Object.setPrototypeOf(this, WebSocketError.prototype);
  }
}
