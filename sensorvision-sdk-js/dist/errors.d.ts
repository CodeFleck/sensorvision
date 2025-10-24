/**
 * Custom error classes for SensorVision SDK
 */
/**
 * Base error class for all SensorVision SDK errors
 */
export declare class SensorVisionError extends Error {
    constructor(message: string);
}
/**
 * Authentication error - invalid API key
 */
export declare class AuthenticationError extends SensorVisionError {
    constructor(message: string);
}
/**
 * Validation error - invalid data format
 */
export declare class ValidationError extends SensorVisionError {
    constructor(message: string);
}
/**
 * Network error - connection/request failed
 */
export declare class NetworkError extends SensorVisionError {
    constructor(message: string);
}
/**
 * Server error - 5xx response
 */
export declare class ServerError extends SensorVisionError {
    constructor(message: string);
}
/**
 * Rate limit error - too many requests
 */
export declare class RateLimitError extends SensorVisionError {
    constructor(message: string);
}
/**
 * WebSocket error - connection/subscription failed
 */
export declare class WebSocketError extends SensorVisionError {
    constructor(message: string);
}
//# sourceMappingURL=errors.d.ts.map