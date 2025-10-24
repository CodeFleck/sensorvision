"use strict";
/**
 * Custom error classes for SensorVision SDK
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.WebSocketError = exports.RateLimitError = exports.ServerError = exports.NetworkError = exports.ValidationError = exports.AuthenticationError = exports.SensorVisionError = void 0;
/**
 * Base error class for all SensorVision SDK errors
 */
class SensorVisionError extends Error {
    constructor(message) {
        super(message);
        this.name = 'SensorVisionError';
        Object.setPrototypeOf(this, SensorVisionError.prototype);
    }
}
exports.SensorVisionError = SensorVisionError;
/**
 * Authentication error - invalid API key
 */
class AuthenticationError extends SensorVisionError {
    constructor(message) {
        super(message);
        this.name = 'AuthenticationError';
        Object.setPrototypeOf(this, AuthenticationError.prototype);
    }
}
exports.AuthenticationError = AuthenticationError;
/**
 * Validation error - invalid data format
 */
class ValidationError extends SensorVisionError {
    constructor(message) {
        super(message);
        this.name = 'ValidationError';
        Object.setPrototypeOf(this, ValidationError.prototype);
    }
}
exports.ValidationError = ValidationError;
/**
 * Network error - connection/request failed
 */
class NetworkError extends SensorVisionError {
    constructor(message) {
        super(message);
        this.name = 'NetworkError';
        Object.setPrototypeOf(this, NetworkError.prototype);
    }
}
exports.NetworkError = NetworkError;
/**
 * Server error - 5xx response
 */
class ServerError extends SensorVisionError {
    constructor(message) {
        super(message);
        this.name = 'ServerError';
        Object.setPrototypeOf(this, ServerError.prototype);
    }
}
exports.ServerError = ServerError;
/**
 * Rate limit error - too many requests
 */
class RateLimitError extends SensorVisionError {
    constructor(message) {
        super(message);
        this.name = 'RateLimitError';
        Object.setPrototypeOf(this, RateLimitError.prototype);
    }
}
exports.RateLimitError = RateLimitError;
/**
 * WebSocket error - connection/subscription failed
 */
class WebSocketError extends SensorVisionError {
    constructor(message) {
        super(message);
        this.name = 'WebSocketError';
        Object.setPrototypeOf(this, WebSocketError.prototype);
    }
}
exports.WebSocketError = WebSocketError;
//# sourceMappingURL=errors.js.map