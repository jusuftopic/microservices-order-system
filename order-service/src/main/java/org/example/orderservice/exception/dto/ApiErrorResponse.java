package org.example.orderservice.exception.dto;

import java.time.LocalDateTime;
import java.util.List;


/**
 * Standard error response returned by the API when an exception occurs.
 *
 * <p>This class is used by {@code GlobalExceptionHandler} to provide
 * consistent error structures across all endpoints.</p>
 *
 * @param timestamp Timestamp when the error occurred.
 * @param error     Human-readable error message.
 * @param status    HTTP status code.
 * @param errors    Errors
 */
public record ApiErrorResponse(
        LocalDateTime timestamp,
        String error,
        int status,
        List<FieldError> errors
) {

    public static ApiErrorResponse of(String error, int status) {
        return new ApiErrorResponse(LocalDateTime.now(), error, status, null);
    }

    public static ApiErrorResponse of(String message, int status, List<FieldError> errors) {
        return new ApiErrorResponse(LocalDateTime.now(), message, status, errors);
    }

    public record FieldError(String field, String message) {}
}
