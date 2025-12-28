package io.indcloud.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Generic API response wrapper
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    String error
) {
    /**
     * Create a success response with data and message
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    /**
     * Create a success response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    /**
     * Create an error response
     */
    public static <T> ApiResponse<T> error(String error) {
        return new ApiResponse<>(false, null, null, error);
    }

    /**
     * Create an error response with message
     */
    public static <T> ApiResponse<T> error(String error, String message) {
        return new ApiResponse<>(false, null, message, error);
    }
}
