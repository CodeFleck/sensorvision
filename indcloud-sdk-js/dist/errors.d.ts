/**
 * Custom error classes for IndCloud SDK
 */
/**
 * Base error class for all IndCloud SDK errors
 */
export declare class IndCloudError extends Error {
    constructor(message: string);
}
/**
 * Authentication error - invalid API key
 */
export declare class AuthenticationError extends IndCloudError {
    constructor(message: string);
}
/**
 * Validation error - invalid data format
 */
export declare class ValidationError extends IndCloudError {
    constructor(message: string);
}
/**
 * Network error - connection/request failed
 */
export declare class NetworkError extends IndCloudError {
    constructor(message: string);
}
/**
 * Server error - 5xx response
 */
export declare class ServerError extends IndCloudError {
    constructor(message: string);
}
/**
 * Rate limit error - too many requests
 */
export declare class RateLimitError extends IndCloudError {
    constructor(message: string);
}
/**
 * WebSocket error - connection/subscription failed
 */
export declare class WebSocketError extends IndCloudError {
    constructor(message: string);
}
//# sourceMappingURL=errors.d.ts.map